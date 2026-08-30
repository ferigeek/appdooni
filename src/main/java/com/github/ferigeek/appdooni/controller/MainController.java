package com.github.ferigeek.appdooni.controller;

import com.github.ferigeek.appdooni.model.Application;
import com.github.ferigeek.appdooni.model.OperatingSystem;
import com.github.ferigeek.appdooni.model.Tag;
import com.github.ferigeek.appdooni.repository.ApplicationRepository;
import com.github.ferigeek.appdooni.repository.DatabaseManager;
import com.github.ferigeek.appdooni.repository.OperatingSystemRepository;
import com.github.ferigeek.appdooni.repository.TagRepository;
import com.github.ferigeek.appdooni.service.ApplicationService;
import com.github.ferigeek.appdooni.service.DatabaseService;
import com.github.ferigeek.appdooni.service.DuplicateNameException;
import com.github.ferigeek.appdooni.service.ImportAction;
import com.github.ferigeek.appdooni.service.OperatingSystemService;
import com.github.ferigeek.appdooni.service.TagFilterMode;
import com.github.ferigeek.appdooni.service.TagService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.CheckComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main window controller. Loads operating system tabs, the tag list, and the
 * application table, and keeps the table filtered by the selected operating
 * system tab, the selected tags, the tag match mode, and the search text.
 * The tag sidebar includes an AND/OR toggle and a Clear button that resets
 * the tag selection.
 */
public final class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private TabPane osTabPane;
    @FXML private ListView<Tag> tagListView;
    @FXML private TextField tagSearchField;
    @FXML private ToggleButton tagFilterToggle;
    /** Clear button that removes all selected tags; disabled when no tags are selected. */
    @FXML private Button clearTagFilterButton;
    @FXML private TextField appSearchField;
    @FXML private TableView<Application> applicationTable;
    @FXML private TableColumn<Application, String> nameColumn;
    @FXML private TableColumn<Application, String> osColumn;
    @FXML private TableColumn<Application, String> tagColumn;
    @FXML private TableColumn<Application, String> descriptionColumn;
    @FXML private TableColumn<Application, String> sourceColumn;
    @FXML private TableColumn<Application, String> websiteColumn;
    @FXML private TextArea logArea;
    @FXML private MenuItem importMenuItem;
    @FXML private MenuItem exportMenuItem;

    private final ApplicationService applicationService;
    private final OperatingSystemService operatingSystemService;
    private final TagService tagService;
    private final DatabaseService databaseService;

    private final ObservableList<Tag> allTags = FXCollections.observableArrayList();
    private FilteredList<Tag> filteredTags;
    private final ObservableList<Application> applications = FXCollections.observableArrayList();

    public MainController() {
        DatabaseManager databaseManager = new DatabaseManager();
        this.operatingSystemService = new OperatingSystemService(
                new OperatingSystemRepository(databaseManager), new ApplicationRepository(databaseManager));
        this.tagService = new TagService(new TagRepository(databaseManager));
        this.applicationService = new ApplicationService(
                new ApplicationRepository(databaseManager),
                new OperatingSystemRepository(databaseManager),
                new TagRepository(databaseManager));
        this.databaseService = new DatabaseService(databaseManager);
    }

    @FXML
    public void initialize() {
        com.github.ferigeek.appdooni.logging.TextAreaAppender.setTextArea(logArea);
        loadLogFileIntoArea();
        tagListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tagFilterToggle.setTooltip(new javafx.scene.control.Tooltip(
                "Match all selected tags (AND) or any (OR). Default OR."));
        // Clear is disabled when no tags are selected; clearing triggers refresh via list-change listener.
        clearTagFilterButton.disableProperty().bind(
                javafx.beans.binding.Bindings.isEmpty(tagListView.getSelectionModel().getSelectedItems()));
        filteredTags = new FilteredList<>(allTags, tag -> true);
        tagListView.setItems(filteredTags);
        tagSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyTagFilter(newValue));

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("installationSource"));
        websiteColumn.setCellValueFactory(new PropertyValueFactory<>("websiteUrl"));
        osColumn.setCellValueFactory(cell -> joinOperatingSystems(cell.getValue()));
        tagColumn.setCellValueFactory(cell -> joinTags(cell.getValue()));
        applicationTable.setItems(applications);

        osTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> refreshApplications());
        tagFilterToggle.selectedProperty().addListener((observable, oldValue, newValue) -> refreshApplications());
        appSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshApplications());
        tagListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Tag>) change -> refreshApplications());

        setupTagContextMenu();

        loadOperatingSystems();
        loadTags();
        refreshApplications();
    }

    private void setupTagContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit", new org.kordamp.ikonli.javafx.FontIcon("fas-pen"));
        MenuItem deleteItem = new MenuItem("Remove", new org.kordamp.ikonli.javafx.FontIcon("fas-trash"));
        editItem.setOnAction(event -> {
            Tag selected = tagListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                editTag(selected);
            }
        });
        deleteItem.setOnAction(event -> {
            Tag selected = tagListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteSingleTag(selected);
            }
        });
        contextMenu.getItems().addAll(editItem, deleteItem);
        tagListView.setContextMenu(contextMenu);
    }

    private void loadOperatingSystems() {
        Tab allTab = new Tab("All", new org.kordamp.ikonli.javafx.FontIcon("fas-layer-group"));
        allTab.setUserData(null);
        osTabPane.getTabs().setAll(allTab);
        for (OperatingSystem os : operatingSystemService.listOperatingSystems()) {
            Tab tab = new Tab(os.getName(), new org.kordamp.ikonli.javafx.FontIcon("fas-desktop"));
            tab.setUserData(os.getId());
            osTabPane.getTabs().add(tab);
        }
    }

    private void loadTags() {
        allTags.setAll(tagService.listTags());
    }

    private void applyTagFilter(String text) {
        String term = text == null ? "" : text.trim().toLowerCase();
        filteredTags.setPredicate(tag -> term.isEmpty() || tag.getName().toLowerCase().contains(term));
    }

    /**
     * Reloads the application table using the current operating system tab,
     * selected tags with the chosen AND/OR mode, and the free-text search.
     */
    private void refreshApplications() {
        if (osTabPane.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        Integer osId = (Integer) osTabPane.getSelectionModel().getSelectedItem().getUserData();
        Set<Integer> tagIds = tagListView.getSelectionModel().getSelectedItems().stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());
        TagFilterMode mode = tagFilterToggle.isSelected() ? TagFilterMode.AND : TagFilterMode.OR;
        applications.setAll(applicationService.findFiltered(appSearchField.getText(), osId, tagIds, mode));
    }

    private javafx.beans.property.StringProperty joinOperatingSystems(Application application) {
        String value = application.getOperatingSystems().stream()
                .map(OperatingSystem::getName)
                .collect(Collectors.joining(", "));
        return new javafx.beans.property.SimpleStringProperty(value);
    }

    private javafx.beans.property.StringProperty joinTags(Application application) {
        String value = application.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.joining(", "));
        return new javafx.beans.property.SimpleStringProperty(value);
    }

    @FXML
    private void onTagFilterToggle(ActionEvent event) {
        tagFilterToggle.setText(tagFilterToggle.isSelected() ? "AND" : "OR");
        refreshApplications();
    }

    /**
     * Clears all selected tags in the tag list. The existing list-change listener
     * on the selected items triggers {@link #refreshApplications()} automatically.
     */
    @FXML
    private void onClearTagFilter(ActionEvent event) {
        tagListView.getSelectionModel().clearSelection();
    }

    /** Opens the details dialog when an application row is double-clicked. */
    @FXML
    private void onApplicationTableClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Application selected = applicationTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showApplicationDetails(selected);
            }
        }
    }

    @FXML
    private void onAddTag(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Tag");
        dialog.setHeaderText("Enter tag name");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                tagService.addTag(name);
                log.info("Added tag '{}'", name.trim());
                loadTags();
                refreshApplications();
            } catch (IllegalArgumentException | DuplicateNameException e) {
                log.warn("Failed to add tag '{}': {}", name, e.getMessage());
                showError("Cannot add tag", e.getMessage());
            }
        });
    }

    @FXML
    private void onModifyTag(ActionEvent event) {
        if (allTags.isEmpty()) {
            showInfo("No tags", "There are no tags to modify.");
            return;
        }
        ChoiceDialog<Tag> choice = new ChoiceDialog<>(allTags.get(0), allTags);
        choice.setTitle("Modify Tag");
        choice.setHeaderText("Select a tag to modify");
        choice.setContentText("Tag:");
        Optional<Tag> selected = choice.showAndWait();
        selected.ifPresent(this::editTag);
    }

    private void editTag(Tag tag) {
        TextInputDialog dialog = new TextInputDialog(tag.getName());
        dialog.setTitle("Modify Tag");
        dialog.setHeaderText("Enter new name for '" + tag.getName() + "'");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            String oldName = tag.getName();
            tag.setName(newName);
            try {
                tagService.updateTag(tag);
                log.info("Renamed tag '{}' to '{}'", oldName, newName.trim());
                loadTags();
                refreshApplications();
            } catch (IllegalArgumentException | DuplicateNameException e) {
                tag.setName(oldName);
                log.warn("Failed to rename tag '{}': {}", oldName, e.getMessage());
                showError("Cannot modify tag", e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteTag(ActionEvent event) {
        showDeleteTagDialog();
    }

    private void deleteSingleTag(Tag tag) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Tag");
        confirm.setHeaderText("Delete tag '" + tag.getName() + "'?");
        confirm.setContentText("Applications that had this tag will remain.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            tagService.deleteTag(tag.getId());
            log.info("Deleted tag '{}'", tag.getName());
            loadTags();
            refreshApplications();
        }
    }

    private void showDeleteTagDialog() {
        if (allTags.isEmpty()) {
            showInfo("No tags", "There are no tags to delete.");
            return;
        }
        Dialog<Tag> dialog = new Dialog<>();
        dialog.setTitle("Delete Tag");
        dialog.setHeaderText("Select a tag to delete");
        ListView<Tag> listView = new ListView<>(FXCollections.observableArrayList(allTags));
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        dialog.getDialogPane().setContent(listView);
        ButtonType deleteType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteType, ButtonType.CANCEL);
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteType);
        deleteButton.setDisable(true);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> deleteButton.setDisable(newV == null));
        dialog.setResultConverter(button -> button == deleteType ? listView.getSelectionModel().getSelectedItem() : null);
        Optional<Tag> result = dialog.showAndWait();
        result.ifPresent(this::deleteSingleTag);
    }

    @FXML
    private void onAddOperatingSystem(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Operating System");
        dialog.setHeaderText("Enter operating system name");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                operatingSystemService.addOperatingSystem(name);
                log.info("Added operating system '{}'", name.trim());
                loadOperatingSystems();
                refreshApplications();
            } catch (IllegalArgumentException | DuplicateNameException e) {
                log.warn("Failed to add operating system '{}': {}", name, e.getMessage());
                showError("Cannot add operating system", e.getMessage());
            }
        });
    }

    @FXML
    private void onModifyOperatingSystem(ActionEvent event) {
        var systems = operatingSystemService.listOperatingSystems();
        if (systems.isEmpty()) {
            showInfo("No operating systems", "There are no operating systems to modify.");
            return;
        }
        ChoiceDialog<OperatingSystem> choice = new ChoiceDialog<>(systems.get(0), systems);
        choice.setTitle("Modify Operating System");
        choice.setHeaderText("Select an operating system to modify");
        choice.setContentText("Operating system:");
        Optional<OperatingSystem> selected = choice.showAndWait();
        selected.ifPresent(this::editOperatingSystem);
    }

    private void editOperatingSystem(OperatingSystem os) {
        TextInputDialog dialog = new TextInputDialog(os.getName());
        dialog.setTitle("Modify Operating System");
        dialog.setHeaderText("Enter new name for '" + os.getName() + "'");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            String oldName = os.getName();
            os.setName(newName);
            try {
                operatingSystemService.updateOperatingSystem(os);
                log.info("Renamed operating system '{}' to '{}'", oldName, newName.trim());
                loadOperatingSystems();
                refreshApplications();
            } catch (IllegalArgumentException | DuplicateNameException e) {
                os.setName(oldName);
                log.warn("Failed to rename operating system '{}': {}", oldName, e.getMessage());
                showError("Cannot modify operating system", e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteOperatingSystem(ActionEvent event) {
        showDeleteOperatingSystemDialog();
    }

    private void showDeleteOperatingSystemDialog() {
        var systems = operatingSystemService.listOperatingSystems();
        if (systems.isEmpty()) {
            showInfo("No operating systems", "There are no operating systems to delete.");
            return;
        }
        Dialog<OperatingSystem> dialog = new Dialog<>();
        dialog.setTitle("Delete Operating System");
        dialog.setHeaderText("Select an operating system to delete");
        ListView<OperatingSystem> listView = new ListView<>(FXCollections.observableArrayList(systems));
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        dialog.getDialogPane().setContent(listView);
        ButtonType deleteType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteType, ButtonType.CANCEL);
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteType);
        deleteButton.setDisable(true);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> deleteButton.setDisable(newV == null));
        dialog.setResultConverter(button -> button == deleteType ? listView.getSelectionModel().getSelectedItem() : null);
        Optional<OperatingSystem> result = dialog.showAndWait();
        result.ifPresent(this::deleteSingleOperatingSystem);
    }

    private void deleteSingleOperatingSystem(OperatingSystem os) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Operating System");
        confirm.setHeaderText("Delete operating system '" + os.getName() + "'?");
        confirm.setContentText("Applications available only for this operating system will be deleted. "
                + "Others will keep their remaining operating systems.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            operatingSystemService.deleteOperatingSystem(os.getId());
            log.info("Deleted operating system '{}'", os.getName());
            loadOperatingSystems();
            refreshApplications();
        }
    }

    @FXML
    private void onAddApplication(ActionEvent event) {
        Dialog<Application> dialog = createApplicationDialog(null);
        Optional<Application> result = dialog.showAndWait();
        result.ifPresent(app -> {
            log.info("Added application '{}'", app.getName());
            refreshApplications();
        });
    }

    private void showApplicationDetails(Application application) {
        // Reload to get fresh associations
        Application fresh = applicationService.findApplication(application.getId()).orElse(application);
        Dialog<Application> dialog = createApplicationDetailsDialog(fresh);
        Optional<Application> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            log.info("Updated application '{}'", updated.getName());
            refreshApplications();
        });
    }

    private Dialog<Application> createApplicationDialog(Application existing) {
        boolean isEdit = existing != null;
        Dialog<Application> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Application" : "Add Application");
        dialog.setHeaderText(isEdit ? "Edit application" : "Enter application details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        TextField sourceField = new TextField();
        TextField websiteField = new TextField();

        var allOs = operatingSystemService.listOperatingSystems();
        CheckComboBox<OperatingSystem> osCombo = new CheckComboBox<>(FXCollections.observableArrayList(allOs));
        var allTagsList = tagService.listTags();
        CheckComboBox<Tag> tagCombo = new CheckComboBox<>(FXCollections.observableArrayList(allTagsList));

        if (isEdit) {
            nameField.setText(existing.getName());
            descriptionArea.setText(existing.getDescription());
            sourceField.setText(existing.getInstallationSource());
            websiteField.setText(existing.getWebsiteUrl());
            existing.getOperatingSystems().forEach(os -> osCombo.getCheckModel().check(os));
            existing.getTags().forEach(tag -> tagCombo.getCheckModel().check(tag));
        }

        grid.add(new Label("Name*:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Installation source:"), 0, 2);
        grid.add(sourceField, 1, 2);
        grid.add(new Label("Website:"), 0, 3);
        grid.add(websiteField, 1, 3);
        grid.add(new Label("Operating systems*:"), 0, 4);
        grid.add(osCombo, 1, 4);
        grid.add(new Label("Tags:"), 0, 5);
        grid.add(tagCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType(isEdit ? "Save" : "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                showError("Validation", "Application name must not be blank.");
                return null;
            }
            Set<Integer> osIds = osCombo.getCheckModel().getCheckedItems().stream()
                    .map(OperatingSystem::getId).collect(Collectors.toSet());
            if (osIds.isEmpty()) {
                showError("Validation", "Every application must have at least one operating system.");
                return null;
            }
            Set<Integer> tagIds = tagCombo.getCheckModel().getCheckedItems().stream()
                    .map(Tag::getId).collect(Collectors.toSet());
            try {
                if (isEdit) {
                    existing.setName(name);
                    existing.setDescription(emptyToNull(descriptionArea.getText()));
                    existing.setInstallationSource(emptyToNull(sourceField.getText()));
                    existing.setWebsiteUrl(emptyToNull(websiteField.getText()));
                    applicationService.updateApplication(existing, osIds, tagIds);
                    return existing;
                } else {
                    Application app = new Application();
                    app.setName(name);
                    app.setDescription(emptyToNull(descriptionArea.getText()));
                    app.setInstallationSource(emptyToNull(sourceField.getText()));
                    app.setWebsiteUrl(emptyToNull(websiteField.getText()));
                    return applicationService.addApplication(app, osIds, tagIds);
                }
            } catch (IllegalArgumentException | DuplicateNameException e) {
                showError(isEdit ? "Cannot update application" : "Cannot add application", e.getMessage());
                return null;
            }
        });

        return dialog;
    }

    private Dialog<Application> createApplicationDetailsDialog(Application application) {
        Dialog<Application> dialog = new Dialog<>();
        dialog.setTitle("Application Details");
        dialog.setHeaderText(application.getName());

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        ToggleButton editToggle = new ToggleButton("Edit", new org.kordamp.ikonli.javafx.FontIcon("fas-pen"));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(application.getName());
        TextArea descriptionArea = new TextArea(application.getDescription());
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        TextField sourceField = new TextField(application.getInstallationSource());
        TextField websiteField = new TextField(application.getWebsiteUrl());

        var allOs = operatingSystemService.listOperatingSystems();
        CheckComboBox<OperatingSystem> osCombo = new CheckComboBox<>(FXCollections.observableArrayList(allOs));
        application.getOperatingSystems().forEach(os -> osCombo.getCheckModel().check(os));
        var allTagsList = tagService.listTags();
        CheckComboBox<Tag> tagCombo = new CheckComboBox<>(FXCollections.observableArrayList(allTagsList));
        application.getTags().forEach(tag -> tagCombo.getCheckModel().check(tag));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Installation source:"), 0, 2);
        grid.add(sourceField, 1, 2);
        grid.add(new Label("Website:"), 0, 3);
        grid.add(websiteField, 1, 3);
        grid.add(new Label("Operating systems:"), 0, 4);
        grid.add(osCombo, 1, 4);
        grid.add(new Label("Tags:"), 0, 5);
        grid.add(tagCombo, 1, 5);

        // Disabled by default, enabled when edit toggle is on
        nameField.disableProperty().bind(editToggle.selectedProperty().not());
        descriptionArea.disableProperty().bind(editToggle.selectedProperty().not());
        sourceField.disableProperty().bind(editToggle.selectedProperty().not());
        websiteField.disableProperty().bind(editToggle.selectedProperty().not());
        osCombo.disableProperty().bind(editToggle.selectedProperty().not());
        tagCombo.disableProperty().bind(editToggle.selectedProperty().not());

        root.getChildren().addAll(editToggle, grid);
        dialog.getDialogPane().setContent(root);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(editToggle.selectedProperty().not());

        dialog.setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                showError("Validation", "Application name must not be blank.");
                return null;
            }
            Set<Integer> osIds = osCombo.getCheckModel().getCheckedItems().stream()
                    .map(OperatingSystem::getId).collect(Collectors.toSet());
            if (osIds.isEmpty()) {
                showError("Validation", "Every application must have at least one operating system.");
                return null;
            }
            Set<Integer> tagIds = tagCombo.getCheckModel().getCheckedItems().stream()
                    .map(Tag::getId).collect(Collectors.toSet());
            try {
                application.setName(name);
                application.setDescription(emptyToNull(descriptionArea.getText()));
                application.setInstallationSource(emptyToNull(sourceField.getText()));
                application.setWebsiteUrl(emptyToNull(websiteField.getText()));
                applicationService.updateApplication(application, osIds, tagIds);
                return application;
            } catch (IllegalArgumentException | DuplicateNameException e) {
                showError("Cannot update application", e.getMessage());
                return null;
            }
        });

        return dialog;
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @FXML
    private void onImportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import database");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = fileChooser.showOpenDialog(stageOrDefault(event));
        if (file == null) {
            return;
        }
        try {
            databaseService.importDatabase(file.toPath(), this::resolveImportConflict);
            log.info("Imported database from {}", file);
            loadOperatingSystems();
            loadTags();
            refreshApplications();
        } catch (IOException e) {
            log.error("Import failed", e);
            showError("Import failed", e.getMessage());
        }
    }

    @FXML
    private void onExportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export database");
        fileChooser.setInitialFileName("appdooni-" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".db");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = fileChooser.showSaveDialog(stageOrDefault(event));
        if (file == null) {
            return;
        }
        try {
            Path exported = databaseService.export(file.toPath().getParent());
            log.info("Exported database to {}", exported);
        } catch (IOException e) {
            log.error("Export failed", e);
            showError("Export failed", e.getMessage());
        }
    }

    @FXML
    private void onExit(ActionEvent event) {
        javafx.application.Platform.exit();
    }

    @FXML
    private void onAbout(ActionEvent event) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("About AppDooni");
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        Label title = new Label("AppDooni", new org.kordamp.ikonli.javafx.FontIcon("fas-info-circle"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label desc = new Label("AppDooni is a desktop application for cataloging applications across operating systems.");
        desc.setWrapText(true);
        Label author = new Label("Created by ferigeek.", new org.kordamp.ikonli.javafx.FontIcon("fas-user"));
        Hyperlink link = new Hyperlink("https://github.com/ferigeek/appdooni", new org.kordamp.ikonli.javafx.FontIcon("fas-external-link-alt"));
        link.setOnAction(e -> {
            if (hostServices != null) {
                hostServices.showDocument("https://github.com/ferigeek/appdooni");
            }
        });
        box.getChildren().addAll(title, desc, author, link);
        stage.setScene(new Scene(box, 420, 180));
        stage.showAndWait();
    }

    @FXML
    private void onSourceCode(ActionEvent event) {
        if (hostServices != null) {
            hostServices.showDocument("https://github.com/ferigeek/appdooni");
        } else {
            log.warn("Host services unavailable; cannot open source code page");
        }
    }

    private ImportAction resolveImportConflict(String type, String name) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Import conflict");
        alert.setHeaderText(type + " '" + name + "' already exists");
        alert.setContentText("Choose how to resolve this conflict.");
        ButtonType skip = new ButtonType("Skip");
        ButtonType overwrite = new ButtonType("Overwrite");
        ButtonType keepBoth = new ButtonType("Keep both");
        alert.getButtonTypes().setAll(skip, overwrite, keepBoth);
        return switch (alert.showAndWait().orElse(skip).getText()) {
            case "Skip" -> ImportAction.SKIP;
            case "Overwrite" -> ImportAction.OVERWRITE;
            default -> ImportAction.KEEP_BOTH;
        };
    }

    private javafx.application.HostServices hostServices;

    public void setHostServices(javafx.application.HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void loadLogFileIntoArea() {
        try {
            java.nio.file.Path logPath = com.github.ferigeek.appdooni.util.AppDirectories.getLogPath();
            if (java.nio.file.Files.isRegularFile(logPath)) {
                String content = java.nio.file.Files.readString(logPath);
                logArea.setText(content);
                logArea.positionCaret(content.length());
            }
        } catch (IOException e) {
            log.warn("Could not load log file", e);
        }
    }

    private Stage stageOrDefault(ActionEvent event) {
        if (event != null && event.getSource() instanceof javafx.scene.Node node) {
            return (Stage) node.getScene().getWindow();
        }
        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}