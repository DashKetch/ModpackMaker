package dashketch.apps.modpackmaker.controller;

import dashketch.apps.modpackmaker.model.ModpackProject;
import dashketch.apps.modpackmaker.model.ProjectManifest;
import dashketch.apps.modpackmaker.service.ModrinthAPI;
import dashketch.apps.modpackmaker.service.ProjectService;
import dashketch.apps.modpackmaker.service.VersionService;
import dashketch.apps.modpackmaker.service.QuestUIHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // --- FXML UI Components ---
    @FXML private TextField projectNameField;
    @FXML private ComboBox<String> mcVersionBox;
    @FXML private ComboBox<String> loaderBox;
    @FXML private TextField searchField;
    @FXML private ListView<String> resultList;
    @FXML private TextArea logArea;
    @FXML private Pane questCanvas;

    // --- Logic & Services ---
    private ModpackProject currentProject;
    private final ModrinthAPI api = new ModrinthAPI();
    private final VersionService versionService = new VersionService();
    private final ProjectService projectService = new ProjectService();

    // Track IDs of search results so we know what to install
    private final List<String> resultIds = new ArrayList<>();

    /**
     * Called automatically by JavaFX when the FXML is loaded.
     */
    @FXML
    public void initialize() {
        // 1. Setup Loader Options
        loaderBox.getItems().addAll("fabric", "forge", "neoforge");
        loaderBox.getSelectionModel().selectFirst();

        // 2. Fetch Minecraft Versions from Mojang API
        new Thread(() -> {
            try {
                List<String> versions = versionService.getMinecraftVersions();
                Platform.runLater(() -> {
                    mcVersionBox.getItems().addAll(versions);
                    if (!versions.isEmpty()) mcVersionBox.getSelectionModel().selectFirst();
                });
            } catch (IOException e) {
                Platform.runLater(() -> logArea.appendText("Error: Failed to fetch MC versions.\n"));
            }
        }).start();

        //3. Scrollable canvas
        if (questCanvas != null) {
            questCanvas.setOnScroll(event -> {
                double zoomFactor = 1.05;
                double deltaY = event.getDeltaY();

                if (deltaY < 0) {
                    zoomFactor = 2.0 - zoomFactor;
                }

                questCanvas.setScaleX(questCanvas.getScaleX() * zoomFactor);
                questCanvas.setScaleY(questCanvas.getScaleY() * zoomFactor);
                event.consume();
            });
        }
    }

    /**
     * Creates a new modpack folder structure.
     */
    @FXML
    protected void onCreateProject() {
        String name = projectNameField.getText();
        if (name.isEmpty()) {
            logArea.appendText("Error: Project name cannot be empty.\n");
            return;
        }

        currentProject = new ModpackProject(name, mcVersionBox.getValue(), loaderBox.getValue());
        currentProject.initFileSystem();

        logArea.appendText("Project Created: " + name + "\n");
    }

    /**
     * Opens an existing project and loads its manifest.
     */
    @FXML
    protected void onOpenProject() {
        if (logArea == null) return; // Safety check for FXML injection

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Existing Modpack");

        Window window = (logArea.getScene() != null) ? logArea.getScene().getWindow() : null;
        File selectedDirectory = directoryChooser.showDialog(window);

        if (selectedDirectory != null) {
            try {
                ProjectManifest manifest = projectService.loadManifest(selectedDirectory);

                if (manifest == null) {
                    logArea.appendText("Error: No manifest.json found in " + selectedDirectory.getName() + "\n");
                    return;
                }

                currentProject = new ModpackProject(manifest.name, manifest.mcVersion, manifest.loader, selectedDirectory);

                // Update UI to match loaded project
                projectNameField.setText(currentProject.getName());
                mcVersionBox.setValue(currentProject.getMcVersion());
                loaderBox.setValue(currentProject.getLoader());

                logArea.appendText("Successfully opened: " + currentProject.getName() + "\n");
            } catch (Exception e) {
                logArea.appendText("Failed to open project: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Searches Modrinth for MODS only.
     */
    @FXML
    protected void onSearch() {
        if (currentProject == null) {
            logArea.appendText("Error: Create or Open a project first.\n");
            return;
        }

        resultList.getItems().clear();
        resultIds.clear();
        String query = searchField.getText();

        new Thread(() -> {
            try {
                JsonArray hits = api.searchMods(query, currentProject.getMcVersion(), currentProject.getLoader());
                Platform.runLater(() -> {
                    hits.forEach(element -> {
                        JsonObject obj = element.getAsJsonObject();
                        resultList.getItems().add(obj.get("title").getAsString());
                        resultIds.add(obj.get("project_id").getAsString());
                    });
                });
            } catch (IOException e) {
                Platform.runLater(() -> logArea.appendText("Search Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    /**
     * Installs the selected mod and its required dependencies.
     */
    @FXML
    protected void onInstall() {
        int index = resultList.getSelectionModel().getSelectedIndex();
        if (index == -1 || currentProject == null) return;

        String projectId = resultIds.get(index);
        logArea.appendText("Starting installation of " + resultList.getSelectionModel().getSelectedItem() + "...\n");

        new Thread(() -> {
            try {
                api.installMod(projectId, currentProject);
                Platform.runLater(() -> logArea.appendText("Installation finished!\n"));
            } catch (Exception e) {
                Platform.runLater(() -> logArea.appendText("Install Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    /**
     * Removes the mod file and updates manifest.json
     */
    @FXML
    protected void onRemoveMod() {
        int index = resultList.getSelectionModel().getSelectedIndex();
        if (index == -1 || currentProject == null) return;

        String projectId = resultIds.get(index);

        new Thread(() -> {
            try {
                projectService.uninstallMod(currentProject, projectId);
                Platform.runLater(() -> {
                    logArea.appendText("Mod removed successfully.\n");
                    onSearch(); // Refresh list
                });
            } catch (IOException e) {
                Platform.runLater(() -> logArea.appendText("Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    /**
     * Exports the entire project folder to a .zip file.
     */
    @FXML
    protected void onExport() {
        if (currentProject == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Modpack");
        fileChooser.setInitialFileName(currentProject.getName() + ".zip");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip Files", "*.zip"));

        File file = fileChooser.showSaveDialog(logArea.getScene().getWindow());

        if (file != null) {
            new Thread(() -> {
                try {
                    projectService.exportProject(currentProject, file);
                    Platform.runLater(() -> logArea.appendText("Exported to: " + file.getAbsolutePath() + "\n"));
                } catch (IOException e) {
                    Platform.runLater(() -> logArea.appendText("Export failed: " + e.getMessage() + "\n"));
                }
            }).start();
        }
    }

    /**
     * Placeholder to test the Quest Canvas rendering.
     */
    @FXML
    protected void testQuestRender() {
        if (questCanvas == null) return;
        questCanvas.getChildren().clear();

        // Visualizing dummy nodes to test zoom/pan/drag
        questCanvas.getChildren().add(QuestUIHelper.createQuestNode("Welcome", 0, 0));
        questCanvas.getChildren().add(QuestUIHelper.createQuestNode("Getting Started", 2, 1));
        logArea.appendText("Rendered test quest nodes.\n");
    }

    /**
     * view installed mods
     */

    @FXML
    protected void onViewInstalled() {
        if (currentProject == null) {
            logArea.appendText("Open a project first to see installed mods.\n");
            return;
        }

        resultList.getItems().clear();
        resultIds.clear();

        try {
            ProjectManifest manifest = projectService.loadManifest(currentProject.getRootDir());
            if (manifest == null || manifest.mods.isEmpty()) {
                logArea.appendText("No mods installed yet.\n");
                return;
            }

            for (ProjectManifest.InstalledMod mod : manifest.mods) {
                resultList.getItems().add("[Installed] " + mod.title);
                resultIds.add(mod.id);
            }
            logArea.appendText("Showing " + manifest.mods.size() + " installed mods.\n");
        } catch (IOException e) {
            logArea.appendText("Error loading manifest: " + e.getMessage() + "\n");
        }
    }
}