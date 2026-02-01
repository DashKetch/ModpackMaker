package dashketch.apps.modpackmaker.service;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import java.io.File;
import java.nio.file.Files;

public class QuestService {

    public void readChapter(File chapterFile) throws Exception {
        // Read the file as a string (Minecraft SNBT is just text)
        String content = Files.readString(chapterFile.toPath());

        // Parse it using the library
        CompoundBinaryTag tag = TagStringIO.get().asCompound(content);

        // Example: Get the chapter title
        String title = tag.getString("title");
        System.out.println("Loading Chapter: " + title);

        // Quests are usually in a list called 'quests'
        // We will map these to visual nodes in the next step
    }
}