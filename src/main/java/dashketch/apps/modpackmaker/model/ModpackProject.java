package dashketch.apps.modpackmaker.model;

import java.io.File;

public class ModpackProject {
    private String name;
    private String mcVersion;
    private String loader; // "fabric" or "forge"
    private File rootDir;
    public File getRootDir() {
        return rootDir;
    }

    public ModpackProject(String name, String mcVersion, String loader) {
        this.name = name;
        this.mcVersion = mcVersion;
        this.loader = loader;
        // Creates a folder in your user home directory for testing
        this.rootDir = new File(System.getProperty("user.home"), "MyModpacks/" + name);
    }

    public void initFileSystem() {
        if (!rootDir.exists()) rootDir.mkdirs();
        new File(rootDir, "mods").mkdirs();
        new File(rootDir, "config").mkdirs();
        System.out.println("Project created at: " + rootDir.getAbsolutePath());
    }

    public File getModsFolder() {
        return new File(rootDir, "mods");
    }

    public String getMcVersion() { return mcVersion; }
    public String getLoader() { return loader; }
    public String getName() { return name; }
}