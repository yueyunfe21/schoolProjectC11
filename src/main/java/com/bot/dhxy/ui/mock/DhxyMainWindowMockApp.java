package com.bot.dhxy.ui.mock;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Standalone visual mock for the redesigned main control page.
 *
 * <p>This class is intentionally disconnected from Spring and task services. It exists only so the
 * redesigned JavaFX surface can be reviewed locally before migrating layout/style back into the real
 * {@code MainWindowController}.
 */
public class DhxyMainWindowMockApp extends Application {

    private static final String[] NAV_ITEMS = {"主控", "设置", "验证", "调试", "日志", "说明"};
    private static final String[] NAV_ICONS = {
            "fas-home", "fas-cog", "fas-shield-alt", "fas-bug", "fas-clipboard-list", "fas-info-circle"
    };

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(buildView(), 1488, 1058);
        scene.getStylesheets().add(getClass().getResource("/styles/dhxy-main-window-mock.css").toExternalForm());
        stage.setTitle("DHXY Robot 控制台 - UI Mock");
        stage.setScene(scene);
        stage.show();
    }

    private Parent buildView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("mock-root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildMainContent());
        return root;
    }

    private Parent buildTopBar() {
        Label title = new Label("DHXY Robot 控制台");
        title.getStyleClass().add("app-title");

        Label shortcut = new Label("暂停 Ctrl+Shift+F11 / ");
        shortcut.getStyleClass().add("shortcut-text");
        Label emergency = new Label("紧急停止 Ctrl+Shift+F12");
        emergency.getStyleClass().add("emergency-text");
        HBox shortcutBox = new HBox(shortcut, emergency);
        shortcutBox.setAlignment(Pos.CENTER_LEFT);

        Label darkText = new Label("深色模式");
        darkText.getStyleClass().add("dark-mode-text");
        FontIcon moonIcon = icon("fas-moon", 17, "topbar-icon");
        StackPane switchKnob = new StackPane();
        switchKnob.getStyleClass().add("switch-knob");
        StackPane switchTrack = new StackPane(switchKnob);
        switchTrack.getStyleClass().add("switch-track");
        HBox darkMode = new HBox(8, moonIcon, darkText, switchTrack);
        darkMode.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(38, title, shortcutBox, spacer, darkMode);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        return topBar;
    }

    private Parent buildSidebar() {
        VBox nav = new VBox(14);
        nav.getStyleClass().add("sidebar-nav");
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            nav.getChildren().add(navButton(NAV_ITEMS[i], NAV_ICONS[i], i == 0));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label version = new Label("版本 v1.0.0");
        version.getStyleClass().add("version-text");
        Button update = new Button("更新");
        update.getStyleClass().add("update-button");
        VBox footer = new VBox(13, version, update);
        footer.getStyleClass().add("sidebar-footer");

        VBox sidebar = new VBox(nav, spacer, footer);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    private Parent navButton(String text, String iconLiteral, boolean active) {
        FontIcon icon = icon(iconLiteral, 24, "nav-icon");
        Label label = new Label(text);
        label.getStyleClass().add("nav-label");
        HBox content = new HBox(22, icon, label);
        content.setAlignment(Pos.CENTER_LEFT);
        Button button = new Button();
        button.setGraphic(content);
        button.getStyleClass().add("nav-button");
        if (active) {
            button.getStyleClass().add("nav-button-active");
        }
        return button;
    }

    private Parent buildMainContent() {
        VBox content = new VBox(16, buildMetricCards(), buildWindowPanel(), buildTaskPanel());
        content.getStyleClass().add("main-content");
        return content;
    }

    private Parent buildMetricCards() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("metric-grid");
        grid.add(metricCard("fas-th-large", "窗口", "2", "/ 5", "blue"), 0, 0);
        grid.add(metricCard("fas-play", "运行中", "1", "", "green"), 1, 0);
        grid.add(metricCard("fas-exclamation-triangle", "异常", "0", "", "red"), 2, 0);
        return grid;
    }

    private Parent metricCard(String iconLiteral, String title, String value, String suffix, String tone) {
        StackPane iconBubble = new StackPane(icon(iconLiteral, 24, "metric-icon"));
        iconBubble.getStyleClass().addAll("metric-bubble", "metric-bubble-" + tone);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("metric-value", "metric-value-" + tone);
        Label suffixLabel = new Label(suffix);
        suffixLabel.getStyleClass().add("metric-suffix");
        HBox valueRow = new HBox(5, valueLabel, suffixLabel);
        valueRow.setAlignment(Pos.BASELINE_LEFT);
        VBox text = new VBox(4, titleLabel, valueRow);
        HBox card = new HBox(18, iconBubble, text);
        card.getStyleClass().add("metric-card");
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    private Parent buildWindowPanel() {
        Label title = new Label("窗口与任务");
        title.getStyleClass().add("section-title");

        HBox toolbar = new HBox(14,
                toolbarButton("fas-sync-alt", "刷新窗口表", "secondary-toolbar-button"),
                selectButton("全部"),
                searchField(),
                spacer(),
                toolbarButton(null, "停止全部", "danger-toolbar-button"),
                toolbarButton(null, "取消选择", "secondary-toolbar-button"),
                toolbarButton(null, "全选", "secondary-toolbar-button"),
                toolbarButton("fas-play", "启动", "primary-toolbar-button"));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(toolbar.getChildren().get(3), Priority.ALWAYS);

        GridPane table = new GridPane();
        table.getStyleClass().add("window-table");
        String[] headers = {"", "角色名", "Base", "服务器", "ID", "状态", "运行任务", "进度", "操作"};
        for (int i = 0; i < headers.length; i++) {
            table.add(tableCell(headers[i], true, "table-header"), i, 0);
        }
        addWindowRow(table, 1, "邢邵ヌ忍者", "江山如画", "江山如画", "67555", "空闲", "未知任务", "-", false);
        addWindowRow(table, 2, "逍遥小队长", "江山如画", "江山如画", "67556", "运行中", "五环", "1/2", true);
        for (int row = 3; row <= 6; row++) {
            addEmptyWindowRow(table, row);
        }

        VBox panel = new VBox(14, title, toolbar, table);
        panel.getStyleClass().add("window-panel");
        return panel;
    }

    private void addWindowRow(
            GridPane table,
            int row,
            String roleName,
            String base,
            String server,
            String id,
            String status,
            String runningTask,
            String progress,
            boolean running
    ) {
        table.add(checkboxCell(), 0, row);
        table.add(tableCell(roleName, false, "table-cell"), 1, row);
        table.add(tableCell(base, false, "table-cell"), 2, row);
        table.add(tableCell(server, false, "table-cell"), 3, row);
        table.add(tableCell(id, false, "table-cell"), 4, row);
        table.add(statusBadge(status, running), 5, row);
        table.add(tableCell(runningTask, false, "table-cell"), 6, row);
        table.add(tableCell(progress, false, "table-cell"), 7, row);
        table.add(rowActions(running), 8, row);
    }

    private void addEmptyWindowRow(GridPane table, int row) {
        table.add(checkboxCell(), 0, row);
        for (int column = 1; column <= 7; column++) {
            table.add(tableCell("", false, "table-cell"), column, row);
        }
        table.add(emptyActionsCell(), 8, row);
    }

    private Parent checkboxCell() {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("window-check-box");
        StackPane cell = new StackPane(checkBox);
        cell.getStyleClass().add("checkbox-cell");
        return cell;
    }

    private Parent tableCell(String text, boolean header, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(header ? "table-header-label" : "table-label");
        StackPane cell = new StackPane(label);
        cell.getStyleClass().add(styleClass);
        return cell;
    }

    private Parent statusBadge(String text, boolean running) {
        Label label = new Label(text);
        label.getStyleClass().add(running ? "status-running" : "status-idle");
        StackPane cell = new StackPane(label);
        cell.getStyleClass().add("table-cell");
        return cell;
    }

    private Parent rowActions(boolean running) {
        Button runButton = iconTextButton(running ? "fas-pause" : "fas-play", running ? "暂停" : "启动",
                running ? "row-pause-button" : "row-start-button");
        Button stopButton = iconTextButton("fas-stop", "停止", "row-stop-button");
        HBox actions = new HBox(20, runButton, stopButton);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("actions-cell");
        return actions;
    }

    private Parent emptyActionsCell() {
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("actions-cell");
        return actions;
    }

    private Parent buildTaskPanel() {
        Label title = new Label("选择任务");
        title.getStyleClass().add("section-title");

        GridPane taskGrid = new GridPane();
        taskGrid.getStyleClass().add("task-grid");
        taskGrid.add(taskCard("far-circle", "五环", "主线任务", "2轮", true), 0, 0);
        taskGrid.add(taskCard("fas-sync-alt", "五环V2", "主线任务", "2轮", false), 1, 0);
        taskGrid.add(taskCard("text:5x", "五倍", "日常任务", "100次", false), 2, 0);
        taskGrid.add(taskCard("fas-skull", "修罗", "挑战任务", "60分", false), 3, 0);
        taskGrid.add(taskCard("fas-gamepad", "自动战斗", "辅助任务", "手动", false), 0, 1);
        taskGrid.add(taskCard("fas-crosshairs", "坐标调试", "调试工具", "2点", false), 1, 1);
        taskGrid.add(taskCard("fas-map-marker-alt", "地图校准", "调试工具", "5点", false), 2, 1);
        taskGrid.add(taskCard("fas-tachometer-alt", "导航压力测试", "测试工具", "手动", false), 3, 1);

        VBox panel = new VBox(12, title, taskGrid);
        panel.getStyleClass().add("task-panel");
        return panel;
    }

    private Parent taskCard(String iconLiteral, String title, String subtitle, String chipText, boolean selected) {
        Node icon = taskIcon(iconLiteral);
        Label name = new Label(title);
        name.getStyleClass().add("task-name");
        name.setWrapText(true);
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("task-subtitle");
        sub.setWrapText(true);
        Label chip = new Label(chipText);
        chip.getStyleClass().add("task-chip");
        VBox text = new VBox(4, name, sub);
        HBox content = new HBox(14, icon, text, chip);
        content.setAlignment(Pos.CENTER_LEFT);
        StackPane card = new StackPane(content);
        card.getStyleClass().add("task-card");
        if (selected) {
            card.getStyleClass().add("task-card-selected");
            FontIcon check = icon("fas-check", 16, "task-check-icon");
            StackPane checkCorner = new StackPane(check);
            checkCorner.getStyleClass().add("task-check-corner");
            card.getChildren().add(checkCorner);
            StackPane.setAlignment(checkCorner, Pos.TOP_RIGHT);
        }
        return card;
    }

    private Node taskIcon(String iconLiteral) {
        if (iconLiteral.startsWith("text:")) {
            Label textIcon = new Label(iconLiteral.substring("text:".length()));
            textIcon.getStyleClass().add("task-text-icon");
            return textIcon;
        }
        return icon(iconLiteral, 32, "task-icon");
    }

    private Parent searchField() {
        FontIcon search = icon("fas-search", 17, "search-icon");
        TextField field = new TextField();
        field.setPromptText("搜索角色 / ID / 服务器");
        field.getStyleClass().add("search-field");
        HBox box = new HBox(10, search, field);
        box.getStyleClass().add("search-box");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Button selectButton(String text) {
        Button button = new Button();
        button.getStyleClass().add("secondary-toolbar-button");
        button.setGraphic(new HBox(36, new Label(text), icon("fas-chevron-down", 13, "select-chevron")));
        return button;
    }

    private Button toolbarButton(String iconLiteral, String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        if (iconLiteral != null) {
            button.setGraphic(icon(iconLiteral, 17, "button-icon"));
        }
        return button;
    }

    private Button iconTextButton(String iconLiteral, String text, String styleClass) {
        Button button = new Button(text);
        button.setGraphic(icon(iconLiteral, 16, "button-icon"));
        button.getStyleClass().add(styleClass);
        return button;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private FontIcon icon(String iconLiteral, int size, String styleClass) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(size);
        icon.getStyleClass().add(styleClass);
        return icon;
    }
}
