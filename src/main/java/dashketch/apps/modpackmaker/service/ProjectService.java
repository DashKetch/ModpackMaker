package dashketch.apps.modpackmaker.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dashketch.apps.modpackmaker.model.ModpackProject;
import dashketch.apps.modpackmaker.model.ProjectManifest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public void uninstallMod(ModpackProject project, String projectId) throws IOException {
        ProjectManifest manifest = loadManifest(project.getRootDir());
        if (manifest == null) return;

        ProjectManifest.InstalledMod modToRemove = manifest.mods.stream()
                .filter(m -> m.id.equals(projectId))
                .findFirst()
                .orElse(null);

        if (modToRemove != null) {
            // Use Path API for better error reporting and handling
            File modFile = new File(project.getModsFolder(), modToRemove.filename);
            Path path = modFile.toPath();

            try {
                // Force a garbage collection hint to release file handles if any remain
                System.gc();
                java.nio.file.Files.deleteIfExists(path);
                System.out.println("Deleted file: " + modToRemove.filename);
            } catch (IOException e) {
                System.err.println("Could not delete file (it may be in use): " + e.getMessage());
                // Throw the error so the UI can log it
                throw new IOException("File is locked by another process. Try restarting the app.");
            }

            manifest.mods.remove(modToRemove);
            saveManifest(project.getRootDir(), manifest);
        }
    }

    public void exportProject(ModpackProject project, File outputFile) throws IOException {
        Path sourceDirPath = project.getRootDir().toPath();
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(outputFile.toPath()))) {
            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        // Create a relative path for the file inside the zip
                        String zipPath = sourceDirPath.relativize(path).toString();
                        ZipEntry zipEntry = new ZipEntry(zipPath);
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println("Error zipping file: " + path);
                        }
                    });
        }
    }
}