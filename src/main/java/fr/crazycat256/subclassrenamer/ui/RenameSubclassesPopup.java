package fr.crazycat256.subclassrenamer.ui;

import fr.crazycat256.subclassrenamer.Processor;
import fr.crazycat256.subclassrenamer.SubclassRenamer;
import jakarta.enterprise.inject.Instance;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.workspace.model.Workspace;

import static atlantafx.base.theme.Styles.*;
import static fr.crazycat256.subclassrenamer.SubclassRenamer.INDEX_PLACEHOLDER;
import static fr.crazycat256.subclassrenamer.SubclassRenamer.NAME_PLACEHOLDER;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;


/**
 * Popup for handling renaming of subclasses.
 *
 * @author crazycat256
 */
public class RenameSubclassesPopup extends RecafStage {

    private static final Logger logger = Logging.get(RenameSubclassesPopup.class);
    private final SubclassRenamer plugin;
    private final Instance<MappingApplier> applierProvider;
    private final InheritanceGraph inheritanceGraph;
    private final Workspace workspace;
    private final JvmClassInfo info;
    private final Label output = new Label();
    private final TextField patternInput = new TextField();
    private final TextField regexInput = new TextField();
    private final CheckBox recursiveBox = new CheckBox();

    /**
     *
     * @param plugin
     * 		Plugin with config values.
     * @param applierProvider
     * 		Provider for mapping applier.
     * @param workspace
     * 		Workspace to pull classes from.
     * @param info
     *      Class to rename.
     */
    public RenameSubclassesPopup(SubclassRenamer plugin, Instance<MappingApplier> applierProvider, InheritanceGraph inheritanceGraph, Workspace workspace, JvmClassInfo info) {
        this.plugin = plugin;
        this.applierProvider = applierProvider;
        this.inheritanceGraph = inheritanceGraph;
        this.workspace = workspace;
        this.info = info;

        titleProperty().set("Rename Subclasses");

        Label title = new Label("Class: " + info.getName());
        title.paddingProperty().set(new Insets(0, 0, 5, 0));

        String packageName = info.getName().contains("/") ? info.getName().substring(0, info.getName().lastIndexOf("/")) : "";
        String simpleClassName = info.getName().contains("/") ? info.getName().substring(info.getName().lastIndexOf("/") + 1) : info.getName();
        patternInput.setPromptText("Pattern");
        patternInput.setText((packageName.isEmpty() ? simpleClassName : packageName + "/" + simpleClassName) + "_" + INDEX_PLACEHOLDER);
        patternInput.setOnKeyPressed(this::onKeyPressed);

        Label infoLabel = new Label(String.format("Use " + NAME_PLACEHOLDER + " for the original name and " + INDEX_PLACEHOLDER + " for an auto-incremented index"));
        HBox infoContainer = new HBox(infoLabel);
        infoContainer.setPadding(new Insets(0, 0, 7, 0));

        recursiveBox.setText("Recursively");
        recursiveBox.setTooltip(new Tooltip("Rename subclasses of subclasses, and so on"));
        recursiveBox.setSelected(true);
        HBox recursiveBoxContainer = new HBox(recursiveBox);
        recursiveBoxContainer.setPadding(new Insets(0, 0, 7, 0));

        HBox.setHgrow(regexInput, Priority.ALWAYS);
        Label regexLabel = new Label("Regex Filter: ");
        regexLabel.setTooltip(new Tooltip("Only rename classes that match the regex, leave empty to rename all subclasses"));
        regexInput.setText("");
        regexInput.setPromptText("^com/example/package.*$");
        regexInput.setOnKeyPressed(this::onKeyPressed);
        HBox regexContainer = new HBox(regexLabel, regexInput);
        regexContainer.setPadding(new Insets(0, 0, 7, 0));
        regexContainer.setAlignment(Pos.CENTER_LEFT);

        Button accept = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), this::accept);
        Button cancel = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
        accept.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, SUCCESS);
        cancel.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, DANGER);

        HBox buttons = new HBox(accept, cancel);
        buttons.setSpacing(10);
        buttons.setPadding(new Insets(10, 0, 10, 0));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        output.setTextFill(new Color(229f/255, 62f/255, 52f/255, 1f));

        StackPane outputAndButtons = new StackPane();
        outputAndButtons.getChildren().addAll(output, buttons);

        VBox layout = new VBox(title, patternInput, infoContainer, recursiveBoxContainer, regexContainer, outputAndButtons);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(10));
        setScene(new RecafScene(layout, 600, 213));

    }

    /**
     * Handle key press events.
     */
    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            accept();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            hide();
        }
    }

    /**
     * Accept the input and begin renaming.
     */
    private void accept() {
        logger.info("Renaming subclasses of {} with pattern '{}' if matching regex '{}'", info.getName(), patternInput.getText(), regexInput.getText());
        String pattern = patternInput.getText();
        if (pattern.isEmpty()) {
            output.setText("Pattern cannot be empty.");
            return;
        }
        if (!pattern.contains(NAME_PLACEHOLDER) && !pattern.contains(INDEX_PLACEHOLDER)) {
            output.setText("Pattern must contain either " + NAME_PLACEHOLDER + " or " + INDEX_PLACEHOLDER);
            return;
        }

        Processor processor = new Processor(plugin, applierProvider, inheritanceGraph, workspace, info, pattern, regexInput.getText(), recursiveBox.isSelected());

        processor.analyze(workspace.getPrimaryResource().getJvmClassBundle().entrySet());
        processor.apply();
        hide();
    }
}
