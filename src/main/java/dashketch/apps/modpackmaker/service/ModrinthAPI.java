package dashketch.apps.modpackmaker.service;

import dashketch.apps.modpackmaker.model.ModpackProject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dashketch.apps.modpackmaker.model.ProjectManifest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ModrinthAPI {
    private static final String BASE_URL = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "Nate Askew/ModpackMaker/1.0.0 (nateisthegreatgreat@gmail.com)";
    private final OkHttpClient client = new OkHttpClient();

    // 1. Search for Mods
    public JsonArray searchMods(String query, String mcVersion, String loader) throws IOException {
        String facets = String.format(
                "[[\"versions:%s\"],[\"categories:%s\"],[\"project_type:mod\"]]",
                mcVersion,
                loader.toLowerCase()
        );

        String encodedFacets = URLEncoder.encode(facets, StandardCharsets.UTF_8);
        String url = BASE_URL + "/search?query=" + query + "&facets=" + encodedFacets + "&limit=500";

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Search failed: " + response.code());
            assert response.body() != null;
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return json.getAsJsonArray("hits");
        }
    }

    // 2. Install Mod + Dependencies
    public void installMod(String projectId, ModpackProject project) throws IOException {
        System.out.println("Installing project: " + projectId);

        JsonObject versionObj = getVersionForProject(projectId, project.getMcVersion(), project.getLoader());
        if (versionObj == null) {
            System.out.println("No compatible version found for " + projectId);
            return;
        }

        downloadFile(versionObj, project);

        JsonArray dependencies = versionObj.getAsJsonArray("dependencies");
        if (dependencies != null) {
            for (JsonElement dep : dependencies) {
                JsonObject depObj = dep.getAsJsonObject();
                if (depObj.has("dependency_type") &&
                        depObj.get("dependency_type").getAsString().equals("required")) {

                    String depProjectId = depObj.get("project_id").getAsString();
                    installMod(depProjectId, project);
                }
            }
        }
    }

    private JsonObject getVersionForProject(String projectId, String mcVer, String loader) throws IOException {
        String url = BASE_URL + "/project/" + projectId + "/version?loaders=[\"" + loader + "\"]&game_versions=[\"" + mcVer + "\"]";
        Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            JsonArray versions = JsonParser.parseString(response.body().string()).getAsJsonArray();
            if (versions.isEmpty()) return null;
            return versions.get(0).getAsJsonObject();
        }
    }

    private void downloadFile(JsonObject versionObj, ModpackProject project) throws IOException {
        JsonArray files = versionObj.getAsJsonArray("files");
        JsonObject primaryFile = files.get(0).getAsJsonObject();

        String downloadUrl = primaryFile.get("url").getAsString();
        String filename = primaryFile.get("filename").getAsString();

        // Use "name" if "title" isn't available in the version object
        String title = versionObj.has("name") ? versionObj.get("name").getAsString() : filename;
        String projectId = versionObj.get("project_id").getAsString();

        Request request = new Request.Builder().url(downloadUrl).header("User-Agent", USER_AGENT).build();

        try (Response response = client.newCall(request).execute()) {
            File outputFile = new File(project.getModsFolder(), filename);
            if (!outputFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    assert response.body() != null;
                    fos.write(response.body().bytes());
                }
                System.out.println("Downloaded: " + filename);
            }

            // Update the Manifest
            ProjectService projectService = new ProjectService();
            ProjectManifest manifest = projectService.loadManifest(project.getRootDir());

            if (manifest == null) {
                manifest = new ProjectManifest();
                manifest.name = project.getName();
                manifest.mcVersion = project.getMcVersion();
                manifest.loader = project.getLoader();
            }

            // Add the mod if not present
            boolean exists = manifest.mods.stream().anyMatch(m -> m.id.equals(projectId));
            if (!exists) {
                manifest.mods.add(new ProjectManifest.InstalledMod(projectId, title, filename));
                projectService.saveManifest(project.getRootDir(), manifest);
            }
        }
    }
}