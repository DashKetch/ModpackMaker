package dashketch.apps.modpackmaker.service;

import dashketch.apps.modpackmaker.model.ModpackProject;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import java.io.File;
import java.nio.file.Files;

public class QuestLoader {

    public static void loadQuestsToCanvas(ModpackProject project, javafx.scene.layout.Pane canvas) {
        File questFolder = new File(project.getRootDir(), "config/ftbquests/quests/chapters");
        if (!questFolder.exists()) return;

        canvas.getChildren().clear();

        File[] files = questFolder.listFiles((dir, name) -> name.endsWith(".snbt"));
        if (files == null) return;

        for (File f : files) {
            try {
                String content = Files.readString(f.toPath());
                CompoundBinaryTag root = TagStringIO.get().asCompound(content);
                ListBinaryTag quests = root.getList("quests");

                for (int i = 0; i < quests.size(); i++) {
                    CompoundBinaryTag q = quests.getCompound(i);
                    String title = q.getString("title");
                    if (title.isEmpty()) title = "Unnamed Quest";

                    double x = q.getDouble("x");
                    double y = q.getDouble("y");

                    // Add to UI
                    canvas.getChildren().add(QuestUIHelper.createQuestNode(title, x, y));
                }
            } catch (Exception e) {
                System.err.println("Failed to parse quest file: " + f.getName());
            }
        }
    }
}