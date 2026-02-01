package dashketch.apps.modpackmaker.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectManifest {
    public String name;
    public String mcVersion;
    public String loader;
    public List<InstalledMod> mods = new ArrayList<>();

    public static class InstalledMod {
        public String id;
        public String title;
        public String filename;

        public InstalledMod(String id, String title, String filename) {
            this.id = id;
            this.title = title;
            this.filename = filename;
        }
    }
}