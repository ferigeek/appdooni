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
import com.github.ferigeek.appdooni.service.ImportAction;
import com.github.ferigeek.appdooni.service.OperatingSystemService;
import com.github.ferigeek.appdooni.service.TagFilterMode;
import com.github.ferigeek.appdooni.service.TagService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main window controller. Loads operating system tabs, the tag list, and the
 * application table, and keeps the table filtered by the selected operating
 * system tab, the selected tags, the tag match mode, and the search text.
 */
public final class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private TabPane osTabPane;
    @FXML private ListView<Tag> tagListView;
    @FXML private TextField tagSearchField;
    @FXML private ToggleButton tagFilterToggle;
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
        tagListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tagFilterToggle.setTooltip(new javafx.scene.control.Tooltip(
                "Match all selected tags (AND) or any (OR). Default OR."));
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
        tagListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshApplications());

        loadOperatingSystems();
        loadTags();
        refreshApplications();
    }

    private void loadOperatingSystems() {
        Tab allTab = new Tab("All");
        allTab.setUserData(null);
        osTabPane.getTabs().setAll(allTab);
        for (OperatingSystem os : operatingSystemService.listOperatingSystems()) {
            Tab tab = new Tab(os.getName());
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

    private void refreshApplications() {
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About AppDooni");
        alert.setHeaderText("AppDooni");
        alert.setContentText("AppDooni is a desktop application for cataloging applications across "
                + "operating systems.\n\nCreated by ferigeek.");
        alert.showAndWait();
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
}