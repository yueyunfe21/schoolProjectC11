package com.bot.dhxy.ui.mock;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class DhxyMonsterIconPreviewApp extends Application {

    @Override
    public void start(Stage stage) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(24));

        String[][] options = {
                {"fas-skull", "骷髅"},
                {"fas-skull-crossbones", "毒骷髅"},
                {"fas-ghost", "幽灵"},
                {"fas-dragon", "龙"},
                {"fas-spider", "蜘蛛"},
                {"fas-bug", "虫"},
                {"fas-user-ninja", "忍者"},
                {"fas-mask", "面具"}
        };

        for (int i = 0; i < options.length; i++) {
            grid.add(iconCard(options[i][0], options[i][1]), i % 4, i / 4);
        }

        VBox root = new VBox(18, title(), grid);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: white;");
        Scene scene = new Scene(root, 760, 360);
        stage.setTitle("DHXY 修罗怪物图标候选");
        stage.setScene(scene);
        stage.show();
    }

    private Label title() {
        Label label = new Label("修罗 / 怪物图标候选");
        label.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #111827;");
        return label;
    }

    private VBox iconCard(String iconLiteral, String name) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(46);
        icon.setIconColor(javafx.scene.paint.Color.web("#3b82f6"));

        Label literal = new Label(iconLiteral);
        literal.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
        Label label = new Label(name);
        label.setStyle("-fx-font-family: 'Microsoft YaHei UI'; -fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #111827;");

        VBox text = new VBox(2, label, literal);
        text.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(14, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(row);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setMinSize(160, 92);
        card.setPrefSize(160, 92);
        card.setMaxSize(160, 92);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dce2ea; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
