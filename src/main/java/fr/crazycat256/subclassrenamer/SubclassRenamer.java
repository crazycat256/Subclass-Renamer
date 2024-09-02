package fr.crazycat256.subclassrenamer;

import fr.crazycat256.subclassrenamer.ui.RenameSubclassesPopup;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.cell.context.*;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.util.Menus;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.cell.context.ContextMenuProviderService;
import software.coley.recaf.services.cell.context.ContextSource;

/**
 * Subclass renamer plugin.
 * This plugin automatically renames each subclass of a class.
 *
 * @author crazycat256
 */
@Dependent
@PluginInformation(
    id = "subclass-renamer",
    name = "Subclass Renamer",
    version = "2.2",
    author = "crazycat256",
    description = "Automatically renames each subclass of a class."
)
public class SubclassRenamer implements Plugin {
    private static final Logger logger = Logging.get(SubclassRenamer.class);
    public static final String NAME_PLACEHOLDER = "{name}";
    public static final String INDEX_PLACEHOLDER = "{index}";
    public final int renameTimeout = 30; // TODO: Add config for this
    private final ContextMenuProviderService menuProviderService;
    private final Instance<MappingApplier> applierProvider;
    private final ClassContextMenuAdapter adapter = new SubclassRenamerClassContextMenuAdapter();

    @Inject
    public SubclassRenamer(ContextMenuProviderService menuProviderService, Instance<MappingApplier> applierProvider) {
        this.menuProviderService = menuProviderService;
        this.applierProvider = applierProvider;
    }

    @Override
    public void onEnable() {
        logger.info("Enabled SubclassRenamer plugin.");
        menuProviderService.addClassContextMenuAdapter(adapter);
    }

    @Override
    public void onDisable() {
        logger.info("Disabled SubclassRenamer plugin.");
        menuProviderService.removeClassContextMenuAdapter(adapter);
    }

    /**
     * Show the rename subclasses popup.
     * @param workspace
     * 		Workspace to pull classes from.
     * @param info
     * 		Class to rename.
     */
    private void displayPopup(Workspace workspace, JvmClassInfo info) {
        new RenameSubclassesPopup(this, applierProvider, workspace, info).show();
    }


    /**
     * Adapter for adding the "Rename subclasses" action to the class context menu.
     * This can't be an anonymous class because it needs to be public.
     */
    public class SubclassRenamerClassContextMenuAdapter implements ClassContextMenuAdapter {
        @Override
        public void adaptJvmClassMenu(@Nonnull ContextMenu menu, @Nonnull ContextSource source, @Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info) {
            menu.getItems().add(Menus.actionLiteral("Rename subclasses", CarbonIcons.PARENT_CHILD, () -> {
                displayPopup(workspace, info);
            }));
        }
    }
}
