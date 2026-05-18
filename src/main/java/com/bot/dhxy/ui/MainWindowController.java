package com.bot.dhxy.ui;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.runner.TaskControlService;
import com.bot.dhxy.service.GameWindowService;
import com.bot.dhxy.ui.viewmodel.TaskDashboardView;
import com.bot.dhxy.ui.viewmodel.TaskLogView;
import com.bot.dhxy.ui.viewmodel.TaskOptionView;
import com.bot.dhxy.ui.viewmodel.TaskRecordView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX 主界面控制器。
 *
 * 当前先提供基础界面骨架：任务勾选、开始/停止按钮、任务记录表、日志列表。
 * 后面再逐步接入定时刷新和更完整的界面样式。
 */
@Component
@RequiredArgsConstructor
public class MainWindowController {

    private final TaskViewService taskViewService;
    private final TaskControlService taskControlService;
    private final TaskRunProperties taskRunProperties;
    private final GameWindowService gameWindowService;

    /**
     * 这些 JavaFX 控件不能在 Spring 创建 Bean 时初始化。
     *
     * 原因：Spring 容器启动时 JavaFX Toolkit 还没有初始化，
     * 如果在字段里直接 new VBox/TableView/ListView，会触发 Toolkit not initialized。
     * 所以这里只声明，真正创建放到 buildView() 里。
     */
    private VBox taskBox;
    private TableView<TaskRecordView> recordTable;
    private ListView<String> logList;
    private Button startButton;
    private Button stopButton;

    public Parent buildView() {
        initControls();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        root.setTop(buildTopBar());
        root.setLeft(buildTaskPanel());
        root.setCenter(buildRecordTable());
        root.setBottom(buildLogPanel());

        refreshDashboard();
        return root;
    }

    private void initControls() {
        taskBox = new VBox(8);
        recordTable = new TableView<>();
        logList = new ListView<>();
        startButton = new Button("开始");
        stopButton = new Button("停止");
    }

    private Parent buildTopBar() {
        Label title = new Label("DHXY Robot 控制台");
        Button refreshButton = new Button("刷新");

        refreshButton.setOnAction(event -> refreshDashboard());
        startButton.setOnAction(event -> startSelectedTasksInBackground());
        stopButton.setOnAction(event -> {
            taskControlService.stop();
            refreshDashboard();
        });

        HBox box = new HBox(10, title, refreshButton, startButton, stopButton);
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private Parent buildTaskPanel() {
        Label label = new Label("任务选择");
        VBox wrapper = new VBox(10, label, taskBox);
        wrapper.setPadding(new Insets(0, 12, 0, 0));
        wrapper.setPrefWidth(180);
        return wrapper;
    }

    private Parent buildRecordTable() {
        TableColumn<TaskRecordView, String> taskNameCol = new TableColumn<>("任务");
        taskNameCol.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        taskNameCol.setPrefWidth(120);

        TableColumn<TaskRecordView, String> resultCol = new TableColumn<>("结果");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setPrefWidth(90);

        TableColumn<TaskRecordView, Long> costCol = new TableColumn<>("耗时(ms)");
        costCol.setCellValueFactory(new PropertyValueFactory<>("costMillis"));
        costCol.setPrefWidth(90);

        TableColumn<TaskRecordView, String> messageCol = new TableColumn<>("备注");
        messageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        messageCol.setPrefWidth(260);

        recordTable.getColumns().setAll(List.of(taskNameCol, resultCol, costCol, messageCol));
        return recordTable;
    }

    private Parent buildLogPanel() {
        VBox wrapper = new VBox(6, new Label("任务日志"), logList);
        wrapper.setPadding(new Insets(12, 0, 0, 0));
        wrapper.setPrefHeight(220);
        return wrapper;
    }

    private void startSelectedTasksInBackground() {
        List<String> selectedTaskCodes = getSelectedTaskCodesFromUi();
        if (selectedTaskCodes.isEmpty()) {
            logList.getItems().add(0, "没有勾选任何任务，无法开始。");
            return;
        }

        startButton.setDisable(true);
        Thread worker = new Thread(() -> {
            try {
                if (taskRunProperties.isInitGameWindow()) {
                    boolean ready = gameWindowService.initGameWindow();
                    if (!ready) {
                        Platform.runLater(() -> {
                            logList.getItems().add(0, "游戏窗口初始化失败，任务未启动。");
                            startButton.setDisable(false);
                            refreshDashboard();
                        });
                        return;
                    }
                }

                taskControlService.startTasks(
                        selectedTaskCodes,
                        taskRunProperties.isLoop(),
                        taskRunProperties.isTestMode()
                );
            } finally {
                Platform.runLater(() -> {
                    startButton.setDisable(false);
                    refreshDashboard();
                });
            }
        }, "task-ui-start-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private List<String> getSelectedTaskCodesFromUi() {
        List<String> selectedTaskCodes = new ArrayList<>();
        for (javafx.scene.Node node : taskBox.getChildren()) {
            if (node instanceof CheckBox checkBox && checkBox.isSelected()) {
                Object userData = checkBox.getUserData();
                if (userData instanceof String taskCode && !taskCode.isBlank()) {
                    selectedTaskCodes.add(taskCode);
                }
            }
        }
        return selectedTaskCodes;
    }

    private void refreshDashboard() {
        TaskDashboardView dashboard = taskViewService.getDashboardView();
        refreshTaskOptions(dashboard.getTaskOptions());
        refreshRecordTable(dashboard.getRecentRecords());
        refreshLogList(dashboard.getRecentLogs());
    }

    private void refreshTaskOptions(List<TaskOptionView> options) {
        taskBox.getChildren().clear();
        for (TaskOptionView option : options) {
            CheckBox checkBox = new CheckBox(option.getTaskName() + " (" + option.getTaskCode() + ")");
            checkBox.setUserData(option.getTaskCode());
            checkBox.setSelected(option.isSelected());
            checkBox.setDisable(!option.isEnabled());
            taskBox.getChildren().add(checkBox);
        }
    }

    private void refreshRecordTable(List<TaskRecordView> records) {
        recordTable.getItems().setAll(records);
    }

    private void refreshLogList(List<TaskLogView> logs) {
        logList.getItems().clear();
        for (TaskLogView log : logs) {
            logList.getItems().add("[" + log.getTime() + "] " + log.getType() + " " + log.getMessage());
        }
    }
}
