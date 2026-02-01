package dashketch.apps.modpackmaker.service;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class QuestUIHelper {

    // This creates a draggable "box" for a quest
    public static StackPane createQuestNode(String title, double x, double y) {
        StackPane node = new StackPane();

        Rectangle rect = new Rectangle(100, 50);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        rect.setFill(Color.web("#3c3f41")); // Minecraft Dark Gray
        rect.setStroke(Color.WHITE);

        Text text = new Text(title);
        text.setFill(Color.WHITE);

        node.getChildren().addAll(rect, text);

        // Positioning (FTB Quests uses 0.5d units, we multiply for pixels)
        node.setLayoutX(x * 50 + 500); // Offset to center on canvas
        node.setLayoutY(y * 50 + 500);

        // Basic Drag Logic
        node.setOnMouseDragged(event -> {
            node.setLayoutX(event.getSceneX() - 50);
            node.setLayoutY(event.getSceneY() - 25);
        });

        return node;
    }
}