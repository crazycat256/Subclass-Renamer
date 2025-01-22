package fr.crazycat256.subclassrenamer;

import fr.crazycat256.subclassrenamer.ui.RenameSubclassesPopup;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.cell.context.*;
import software.coley.recaf.services.mapping.MappingApplierService;
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
@PluginInformation(id = "##ID##", version = "##VERSION##", name = "##NAME##", description = "##DESC##")
public class SubclassRenamer implements Plugin {
    private static final Logger logger = Logging.get(SubclassRenamer.class);
    public static final String NAME_PLACEHOLDER = "{name}";
    public static final String INDEX_PLACEHOLDER = "{index}";
    public static final int RENAME_TIMEOUT = 30; // TODO: Add config for this
    private final ContextMenuProviderService menuProviderService;
    private final ClassContextMenuAdapter adapter;

    @Inject
    public SubclassRenamer(ContextMenuProviderService menuProviderService, MappingApplierService applierService) {
        this.menuProviderService = menuProviderService;
        this.adapter = new SubclassRenamerClassContextMenuAdapter(applierService);
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
     * Adapter for adding the "Rename subclasses" action to the class context menu.
     * This can't be an anonymous class because it needs to be public.
     * This also needs to be static because otherwise a wierd error occurs.
     */
    public static class SubclassRenamerClassContextMenuAdapter implements ClassContextMenuAdapter {

        private final MappingApplierService applierService;

        public SubclassRenamerClassContextMenuAdapter(@Nonnull MappingApplierService applierService) {
            this.applierService = applierService;
        }

        @Override
        public void adaptJvmClassMenu(@Nonnull ContextMenu menu, @Nonnull ContextSource source, @Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info) {
            menu.getItems().add(Menus.actionLiteral("Rename subclasses", CarbonIcons.PARENT_CHILD, () -> {
                new RenameSubclassesPopup(applierService, workspace, info).show();
            }));
        }
    }
}
