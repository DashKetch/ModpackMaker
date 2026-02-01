package dashketch.apps.modpackmaker.controller;

import dashketch.apps.modpackmaker.model.ModpackProject;
import dashketch.apps.modpackmaker.service.ModrinthAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainController {
    @FXML private TextField projectNameField;
    @FXML private ComboBox<String> mcVersionBox;
    @FXML private ComboBox<String> loaderBox;
    @FXML private Button createBtn;

    @FXML private TextField searchField;
    @FXML private ListView<String> resultList;
    @FXML private TextArea logArea;

    private ModpackProject currentProject;
    private final ModrinthAPI api = new ModrinthAPI();

    // Tracks Modrinth IDs for the list view
    private final java.util.List<String> resultIds = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        mcVersionBox.getItems().addAll("1.20.1", "1.19.2", "1.18.2"); // Hardcoded for simplicity now
        loaderBox.getItems().addAll("fabric", "forge");
        mcVersionBox.getSelectionModel().selectFirst();
        loaderBox.getSelectionModel().selectFirst();
    }

    @FXML
    protected void onCreateProject() {
        String name = projectNameField.getText();
        if (name.isEmpty()) return;

        currentProject = new ModpackProject(name, mcVersionBox.getValue(), loaderBox.getValue());
        currentProject.initFileSystem();

        logArea.appendText("Created Project: " + name + "\n");
        logArea.appendText("Location: " + currentProject.getModsFolder().getParent() + "\n");
    }

    @FXML
    protected void onSearch() {
        if (currentProject == null) {
            logArea.appendText("Please create a project first!\n");
            return;
        }

        resultList.getItems().clear();
        resultIds.clear();

        new Thread(() -> {
            try {
                JsonArray hits = api.searchMods(searchField.getText(), currentProject.getMcVersion(), currentProject.getLoader());

                Platform.runLater(() -> hits.forEach(element -> {
                    JsonObject obj = element.getAsJsonObject();
                    String title = obj.get("title").getAsString();
                    String id = obj.get("project_id").getAsString();
                    resultList.getItems().add(title);
                    resultIds.add(id);
                }));
            } catch (Exception e) {
                Platform.runLater(() -> logArea.appendText("Search Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    @FXML
    protected void onInstall() {
        int index = resultList.getSelectionModel().getSelectedIndex();
        if (index == -1) return;

        String projectId = resultIds.get(index);
        logArea.appendText("Installing mod (and dependencies)... check console for details.\n");

        new Thread(() -> {
            try {
                api.installMod(projectId, currentProject);
                Platform.runLater(() -> logArea.appendText("Installation Complete!\n"));
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
                Platform.runLater(() -> logArea.appendText("Install Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }
}
