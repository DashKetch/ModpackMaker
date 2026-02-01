package dashketch.apps.modpackmaker.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dashketch.apps.modpackmaker.model.ProjectManifest;
import java.io.*;

public class ProjectService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void saveManifest(File projectDir, ProjectManifest manifest) throws IOException {
        File manifestFile = new File(projectDir, "manifest.json");
        try (Writer writer = new FileWriter(manifestFile)) {
            gson.toJson(manifest, writer);
        }
    }

    public ProjectManifest loadManifest(File projectDir) throws IOException {
        File manifestFile = new File(projectDir, "manifest.json");
        if (!manifestFile.exists()) return null;
        try (Reader reader = new FileReader(manifestFile)) {
            return gson.fromJson(reader, ProjectManifest.class);
        }
    }
}