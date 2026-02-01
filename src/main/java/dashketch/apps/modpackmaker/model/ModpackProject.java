package dashketch.apps.modpackmaker.model;

import java.io.File;

public class ModpackProject {
    private String name;
    private String mcVersion;
    private String loader;
    private File rootDir;

    // Constructor for NEW projects (Uses default path)
    public ModpackProject(String name, String mcVersion, String loader) {
        this.name = name;
        this.mcVersion = mcVersion;
        this.loader = loader;
        // Default location: UserHome/MyModpacks/ProjectName
        this.rootDir = new File(System.getProperty("user.home"), "MyModpacks/" + name);
    }

    // Constructor for OPENING projects (Uses provided path)
    public ModpackProject(String name, String mcVersion, String loader, File existingDir) {
        this.name = name;
        this.mcVersion = mcVersion;
        this.loader = loader;
        this.rootDir = existingDir;
    }

    public void initFileSystem() {
        if (!rootDir.exists()) rootDir.mkdirs();
        new File(rootDir, "mods").mkdirs();
        new File(rootDir, "config").mkdirs();
    }

    // Getters
    public String getName() { return name; }
    public String getMcVersion() { return mcVersion; }
    public String getLoader() { return loader; }
    public File getRootDir() { return rootDir; }
    public File getModsFolder() { return new File(rootDir, "mods"); }
}