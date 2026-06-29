package com.bot.dhxy.ui;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for small JavaFX wiring rules that are hard to exercise headlessly.
 */
public class MainWindowControllerSourceGuardTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String source = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/ui/MainWindowController.java"), StandardCharsets.UTF_8);
        String css = Files.readString(root.resolve(
                "src/main/resources/styles/dhxy-fluent.css"), StandardCharsets.UTF_8);

        require(!source.contains("本地OCR测名字"),
                "main window must not expose the local OCR debug-name button");
        require(!source.contains("PlayerNameOcrDebugService"),
                "main window controller should not keep the removed OCR debug dependency");
        require(source.contains("button.setTooltip(new Tooltip(tooltipText));"),
                "row action buttons must show hover tooltips in the 操作 column");
        require(source.contains("windowSelectionMemory.replaceVisibleSelection"),
                "window search/filter refresh must update only visible selection state");
        require(source.contains("restoreWindowTableSelectionFromMemory"),
                "table refresh must restore visible row selection from persistent selected ids");
        require(!source.contains("new Button(\"详情\")"),
                "操作 column should not render the old 详情 button");
        require(!source.contains("row-detail-button"),
                "操作 column should not keep the removed detail button style");
        require(!source.contains("重试该窗口任务"),
                "problem windows should use the normal start action instead of a separate retry action");
        require(!source.contains("停止该窗口并刷新状态"),
                "row stop action should stay a plain stop action");
        require(!source.contains("rowActionButton(\"↻\""),
                "操作 column should not render the retry icon");
        require(source.contains("actionsCol.setPrefWidth(58);"),
                "操作 column should be compact for only start/pause and stop buttons");
        require(source.contains("rowActionButton(\"Ⅱ\", \"暂停该窗口任务\", \"row-pause-button\""),
                "running row action should render the green pause button style");
        require(source.contains("cell.getValue().getRunningTaskProgressText()"),
                "进度 column must show in-task run progress, not queue progress");
        require(source.contains("+ \" · 进度 \" + snapshot.getRunningTaskProgressText()"),
                "selected window detail must show in-task run progress, not queue progress");
        require(source.contains("addStyleClass(stopAllWindowsButton, \"bulk-danger-button\");"),
                "top stop-all button should use the solid red bulk danger style");
        require(source.contains("addStyleClass(startCurrentTaskButton, \"toolbar-action-button\");"),
                "top main start button should share normal toolbar button dimensions");
        require(source.contains("addStyleClass(selectAllWindowsButton, \"toolbar-action-button\");"),
                "top select-all button should share normal toolbar button dimensions");
        require(source.contains("addStyleClass(clearWindowSelectionButton, \"toolbar-action-button\");"),
                "top clear-selection button should share normal toolbar button dimensions");
        require(!topWindowToolbarBlock(source).contains("pauseSelectedWindowsButton"),
                "top window toolbar should not keep a separate pause selected button");
        require(!topWindowToolbarBlock(source).contains("stopSelectedWindowsButton"),
                "top window toolbar should not keep a separate stop selected button");
        require(source.contains("handleMainStartPauseButton();"),
                "main start button should route between start and pause behavior");
        require(source.contains("updateMainStartButtonStyle(mainStartShouldPause);"),
                "main start button should switch visual style when it becomes pause");
        require(source.contains("shouldPauseFromMainStartButton(selected)"),
                "main start button should switch to pause only when selected windows are all running");
        require(!cssBlock(css, ".window-table {").contains("-fx-border-color"),
                "window table should not draw a second outer border inside the work panel");
        require(!cssBlock(css, ".window-table .table-row-cell:selected {").contains("#93c5fd"),
                "selected rows should not draw an extra blue divider over the table border");
        require(!selectionListenerBlock(source).contains("rememberVisibleWindowTableSelection"),
                "table selection listener must not overwrite checkbox-backed selection memory");
    }

    private static String cssBlock(String css, String selector) {
        int start = css.indexOf(selector);
        require(start >= 0, "css selector must exist: " + selector);
        int end = css.indexOf("}", start);
        require(end > start, "css selector block must be readable: " + selector);
        return css.substring(start, end);
    }

    private static String topWindowToolbarBlock(String source) {
        String marker = "HBox rightTools = new HBox";
        int start = source.indexOf(marker);
        require(start >= 0, "top window toolbar block must exist");
        int end = source.indexOf("rightTools.setAlignment", start);
        require(end > start, "top window toolbar block must be readable");
        return source.substring(start, end);
    }

    private static String selectionListenerBlock(String source) {
        String marker = "getSelectedItems().addListener";
        int start = source.indexOf(marker);
        require(start >= 0, "main window table selection listener must exist");
        int end = source.indexOf("configureVisualStyles();", start);
        require(end > start, "main window table selection listener block must be readable");
        return source.substring(start, end);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
