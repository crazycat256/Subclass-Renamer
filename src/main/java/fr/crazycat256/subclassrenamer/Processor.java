package fr.crazycat256.subclassrenamer;

import jakarta.enterprise.inject.Instance;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.workspace.model.Workspace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Main handler for creating new names and applying them.
 * Copied from Recaf's <a href="https://github.com/Recaf-Plugins/Auto-Renamer/blob/master/src/main/java/me/coley/recaf/plugin/rename/Processor.java">AutoRename Plugin</a>
 *
 * @author Matt Coley
 */
public class Processor {
    private static final Logger logger = Logging.get(Processor.class);
    private final Map<String, String> mappings = new ConcurrentHashMap<>();
    private final SubclassRenamer plugin;
    private final Instance<MappingApplier> applierProvider;
    private final Workspace workspace;
    private final ClassInfo info;
    private final Pattern pattern;
    private final boolean recursive;
    private final NameGenerator generator;

    /**
     * @param plugin
     * 		Plugin with config values.
     * @param applierProvider
     *      Provider for mapping applier.
     * @param workspace
     *      Workspace to pull classes from.
     * @param info
     *      Class to rename.
     * @param pattern
     *      Pattern to use for naming.
     * @param recursive
     *      Whether to rename subclasses of subclasses.
     */
    public Processor(SubclassRenamer plugin, Instance<MappingApplier> applierProvider, Workspace workspace, ClassInfo info, String pattern, String regex, boolean recursive) {
        this.plugin = plugin;
        this.applierProvider = applierProvider;
        this.workspace = workspace;
        this.info = info;
        this.pattern = regex.isEmpty() ? null : Pattern.compile(regex);
        this.recursive = recursive;

        generator = new NameGenerator(workspace, pattern);
    }

    /**
     * Analyze the workspace to create mappings for renaming.
     *
     * @param infos
     * 		Set of class info.
     */
    public void analyze(Set<Map.Entry<String, JvmClassInfo>> infos) {
        // Reset mappings
        mappings.clear();

        // Phase 1: Create mappings for class names
        //  - following phases can use these names to enrich their naming logic
        pooled("Analyze: Class names", service -> {
            for (Map.Entry<String, JvmClassInfo> entry : infos) {
                service.submit(() -> analyzeClass(entry.getValue()));
            }
        });
    }

    /**
     * Generate mapping for class.
     *
     * @param classInfo
     * 		Class to rename.
     */
    private void analyzeClass(ClassInfo classInfo) {
        try {
            // Skip special cases: 'module-classInfo'/'package-classInfo'
            if (classInfo.getName().matches("(?:[\\w\\/]+\\/)?(?:module|package)-info")) {
                return;
            }
            // Skip if the class is the same as the target class
            if (classInfo.getName().equals(info.getName())) {
                return;
            }
            // Class name
            String oldClassName = classInfo.getName();

            // Skip if the class is not a subclass of the target class
            if (!isSubclassOf(classInfo, info)) {
                return;
            }

            // Skip if the class does not match the regex
            if (pattern != null && !pattern.matcher(oldClassName).matches()) {
                return;
            }

            // Create new name
            String newClassName = generator.createClassName(classInfo);

            // Add mapping
            if (newClassName != null) {
                mappings.put(oldClassName, newClassName);
            }
        } catch (Throwable t) {
            logger.error("Error occurred in Processor#analyzeClass", t);
        }
    }

    /**
     * Applies the mappings created from {@link #analyze(Set) the analysis phase}
     * to the primary resource of the workspace
     */
    public void apply() {
        SortedMap<String, String> sortedMappings = new TreeMap<>(mappings);

        // Create mappings
        IntermediateMappings mappings = new IntermediateMappings();
        for (Map.Entry<String, String> entry : sortedMappings.entrySet()) {
            mappings.addClass(entry.getKey(), entry.getValue());
        }

        // Apply mappings
        MappingApplier applier = applierProvider.get();
        MappingResults results = applier.applyToPrimaryResource(mappings);
        results.apply();


        logger.info("Done auto-mapping! Applied {} mappings", sortedMappings.size());
    }

    /**
     * Run a task that utilizes {@link ExecutorService} for parallel execution.
     * Pooled
     *
     * @param phaseName
     * 		Task name.
     * @param task
     * 		Task to run.
     */
    private void pooled(String phaseName, Consumer<ExecutorService> task) {
        try {
            long start = System.currentTimeMillis();
            logger.info("SubclassRenamer Processing: Task '{}' starting", phaseName);
            ExecutorService service;
            if (generator.allowMultiThread()) {
                service = Executors.newFixedThreadPool(getThreadCount());
            } else {
                service = Executors.newSingleThreadExecutor();
            }
            task.accept(service);
            service.shutdown();
            service.awaitTermination(plugin.renameTimeout, TimeUnit.SECONDS);
            logger.info("SubclassRenamer Processing: Task '{}' completed in {}ms", phaseName, (System.currentTimeMillis() - start));
        } catch (Throwable t) {
            logger.info("Failed processor phase '{}', reason: {}", phaseName, t.getMessage(), t);
        }
    }

    /**
     * Get the number of threads to use for processing.
     * @return Number of threads to use.
     */
    private static int getThreadCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Check if the class is a subclass of the parent.
     * @param info
     * 		Class to check.
     * @param parent
     * 		Parent class.
     * @return {@code true} if the class inherits from the parent.
     */
    private boolean isSubclassOf(ClassInfo info, ClassInfo parent) {
        if (info.getSuperName() != null && info.getSuperName().equals(parent.getName())) {
            return true;
        }

        // Check if the class implements the parent
        if (info.getInterfaces().contains(parent.getName())) {
            return true;
        }

        if (recursive) {

            // Visit super class
            if (info.getSuperName() != null && !info.getSuperName().equals("java/lang/Object")) {
                ClassPathNode superClass = workspace.findClass(info.getSuperName());
                if (superClass != null) {
                    ClassInfo superClassInfo = superClass.getValue();
                    if (isSubclassOf(superClassInfo, parent)) {
                        return true;
                    }
                }
            }

            // Visit interfaces
            for (String iface : info.getInterfaces()) {
                ClassPathNode interfaceClass = workspace.findClass(iface);
                if (interfaceClass == null) continue;
                ClassInfo interfaceClassInfo = interfaceClass.getValue();
                if (isSubclassOf(interfaceClassInfo, parent)) {
                    return true;
                }
            }
        }
        return false;
    }
}
