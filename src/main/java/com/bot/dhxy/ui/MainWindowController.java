package com.bot.dhxy.ui;

import com.bot.dhxy.config.TaskRunProperties;
import com.bot.dhxy.runner.control.TaskControlService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.ui.viewmodel.TaskDashboardView;
import com.bot.dhxy.ui.viewmodel.TaskLogView;
import com.bot.dhxy.ui.viewmodel.TaskOptionView;
import com.bot.dhxy.ui.viewmodel.TaskPlanView;
import com.bot.dhxy.ui.viewmodel.TaskRecordView;
import com.bot.dhxy.ui.viewmodel.TaskRuntimeStateView;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.control.WindowRegistrationBatchBuilder;
import com.bot.dhxy.window.control.WindowSystemSnapshot;
import com.bot.dhxy.window.control.WindowTaskCommandDetail;
import com.bot.dhxy.window.control.WindowTaskCommandResult;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.bot.dhxy.window.control.WindowTaskStartRequest;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX 主界面控制器。
 *
 * 只负责界面构建、读取 UI 状态、刷新展示。
 * 单窗口任务动作交给 TaskUiActionService。
 * 多窗口任务动作交给 WindowTaskControlService。
 */
@Component
@RequiredArgsConstructor
public class MainWindowController {

    private static final DateTimeFormatter UI_LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_WINDOW_COMMAND_LOGS = 80;

    private final TaskViewService taskViewService;
    private final TaskUiActionService taskUiActionService;
    private final TaskControlService taskControlService;
    private final TaskRunProperties taskRunProperties;
    private final WindowTaskControlService windowTaskControlService;
    private final WindowRegistrationBatchBuilder windowRegistrationBatchBuilder;
    private final GameWindowRegistrationService gameWindowRegistrationService;

    private VBox taskBox;
    private TableView<TaskRecordView> recordTable;
    private ListView<String> logList;
    private Button refreshButton;
    private Button clearButton;
    private Button startButton;
    private Button stopButton;
    private CheckBox loopCheckBox;
    private CheckBox testModeCheckBox;
    private CheckBox initGameWindowCheckBox;
    private Label statusLabel;
    private Label requestLabel;
    private Label summaryLabel;
    private Label planSummaryLabel;
    private Label planExecutableLabel;
    private Label planIgnoredLabel;
    private Label planOptionsLabel;
    private Label planWarningLabel;

    private TableView<WindowTaskSnapshot> windowTable;
    private TextField windowIdField;
    private TextField windowRoleNameField;
    private TextField windowBatchCountField;
    private ComboBox<WindowRole> windowRoleComboBox;
    private ComboBox<TaskType> windowTaskTypeComboBox;
    private Button registerWindowButton;
    private Button registerTeamButton;
    private Button scanGameWindowsButton;
    private Button startIndependentWindowsButton;
    private Button selectAllWindowsButton;
    private Button clearWindowSelectionButton;
    private Button startByRoleButton;
    private Button startWindowSelectedTaskButton;
    private Button stopSelectedWindowsButton;
    private Button stopAllWindowsButton;
    private Button unregisterSelectedWindowsButton;
    private Button unregisterAllWindowsButton;
    private Button refreshWindowButton;
    private Label windowSystemLabel;

    private final List<String> windowCommandLogs = new ArrayList<>();
    private Timeline autoRefreshTimeline;

    public Parent buildView() {
        initControls();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        VBox centerArea = new VBox(8, buildWindowPanel(), buildRecordTable());
        VBox bottomArea = new VBox(8, buildLogPanel(), buildStatusBar());

        root.setTop(buildTopBar());
        root.setLeft(buildTaskPanel());
        root.setCenter(centerArea);
        root.setBottom(bottomArea);

        refreshDashboard();
        startAutoRefresh();
        return root;
    }

    private void initControls() {
        taskBox = new VBox(8);
        recordTable = new TableView<>();
        logList = new ListView<>();
        refreshButton = new Button("刷新");
        clearButton = new Button("清空日志");
        startButton = new Button("开始");
        stopButton = new Button("停止");
        loopCheckBox = new CheckBox("循环执行");
        testModeCheckBox = new CheckBox("测试模式");
        initGameWindowCheckBox = new CheckBox("初始化游戏窗口");
        statusLabel = new Label("状态：初始化中");
        requestLabel = new Label("请求：-");
        summaryLabel = new Label("结果：-");
        planSummaryLabel = new Label("计划：-");
        planExecutableLabel = new Label("执行：-");
        planIgnoredLabel = new Label("忽略：-");
        planOptionsLabel = new Label("选项：-");
        planWarningLabel = new Label("警告：-");

        windowTable = new TableView<>();
        windowTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        windowIdField = new TextField("window-1");
        windowIdField.setPrefWidth(110);
        windowRoleNameField = new TextField("角色A");
        windowRoleNameField.setPrefWidth(90);
        windowBatchCountField = new TextField("5");
        windowBatchCountField.setPrefWidth(48);
        windowRoleComboBox = new ComboBox<>();
        windowRoleComboBox.getItems().setAll(WindowRole.values());
        windowRoleComboBox.setValue(WindowRole.UNKNOWN);
        windowTaskTypeComboBox = new ComboBox<>();
        windowTaskTypeComboBox.getItems().setAll(TaskType.values());
        windowTaskTypeComboBox.setValue(TaskType.WUHuan);
        configureWindowComboBoxText();

        registerWindowButton = new Button("注册/刷新窗口");
        registerTeamButton = new Button("测试注册窗口");
        scanGameWindowsButton = new Button("扫描游戏窗口");
        startIndependentWindowsButton = new Button("一键启动独立窗口");
        selectAllWindowsButton = new Button("全选窗口");
        clearWindowSelectionButton = new Button("取消选择");
        startByRoleButton = new Button("测试按身份启动");
        startWindowSelectedTaskButton = new Button("启动已选任务");
        stopSelectedWindowsButton = new Button("停止选中窗口");
        stopAllWindowsButton = new Button("停止全部窗口");
        unregisterSelectedWindowsButton = new Button("移除选中窗口");
        unregisterAllWindowsButton = new Button("移除全部窗口");
        refreshWindowButton = new Button("刷新窗口表");
        windowSystemLabel = new Label("窗口：-");

        loopCheckBox.setSelected(taskRunProperties.isLoop());
        testModeCheckBox.setSelected(taskRunProperties.isTestMode());
        initGameWindowCheckBox.setSelected(taskRunProperties.isInitGameWindow());
        loopCheckBox.setOnAction(event -> refreshDashboard());
        testModeCheckBox.setOnAction(event -> refreshDashboard());
        initGameWindowCheckBox.setOnAction(event -> refreshDashboard());
        applyRuntimeControls(null);
    }

    private void configureWindowComboBoxText() {
        windowRoleComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WindowRole role) {
                return role == null ? "-" : role.getDisplayName();
            }

            @Override
            public WindowRole fromString(String string) {
                return WindowRole.UNKNOWN;
            }
        });
        windowTaskTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(TaskType taskType) {
                return taskType == null ? "-" : taskType.getDisplayName();
            }

            @Override
            public TaskType fromString(String string) {
                return TaskType.UNKNOWN;
            }
        });
    }

    private Parent buildTopBar() {
        Label title = new Label("DHXY Robot 控制台");
        Label emergencyStopLabel = new Label("紧急停止：Ctrl+Shift+F12");

        refreshButton.setOnAction(event -> refreshDashboard());
        clearButton.setOnAction(event -> {
            taskUiActionService.clearFromUi();
            clearWindowLogs();
            refreshDashboard();
        });
        startButton.setOnAction(event -> startSelectedTasksInBackground());
        stopButton.setOnAction(event -> {
            taskUiActionService.stopFromUi();
            refreshDashboard();
        });

        HBox box = new HBox(10, title, refreshButton, clearButton, startButton, stopButton,
                loopCheckBox, testModeCheckBox, initGameWindowCheckBox, emergencyStopLabel);
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private Parent buildTaskPanel() {
        Label label = new Label("任务选择");
        VBox wrapper = new VBox(10, label, taskBox, buildPlanPanel());
        wrapper.setPadding(new Insets(0, 12, 0, 0));
        wrapper.setPrefWidth(260);
        return wrapper;
    }

    private Parent buildPlanPanel() {
        VBox box = new VBox(4,
                new Label("执行计划预览"),
                planSummaryLabel,
                planExecutableLabel,
                planIgnoredLabel,
                planOptionsLabel,
                planWarningLabel);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    private Parent buildWindowPanel() {
        buildWindowTableColumns();

        registerWindowButton.setOnAction(event -> registerOrRefreshWindowFromUi());
        registerTeamButton.setOnAction(event -> registerTeamFromUi());
        scanGameWindowsButton.setOnAction(event -> runWindowCommandInBackground(() ->
                gameWindowRegistrationService.registerDetectedGameWindows(windowTaskTypeComboBox.getValue())));
        startIndependentWindowsButton.setOnAction(event -> runWindowCommandInBackground(() ->
                gameWindowRegistrationService.scanRegisterAndStartIndependentWindows(windowTaskTypeComboBox.getValue())));
        selectAllWindowsButton.setOnAction(event -> selectAllWindows());
        clearWindowSelectionButton.setOnAction(event -> clearWindowSelection());
        startByRoleButton.setOnAction(event -> runWindowCommandInBackground(() ->
                windowTaskControlService.start(WindowTaskStartRequest.detectedRole(getSelectedWindowIds(), windowTaskTypeComboBox.getValue()))));
        startWindowSelectedTaskButton.setOnAction(event -> runWindowCommandInBackground(() ->
                windowTaskControlService.start(WindowTaskStartRequest.selectedTask(getSelectedWindowIds()))));
        stopSelectedWindowsButton.setOnAction(event -> runWindowCommandInBackground(() ->
                windowTaskControlService.stopWindows(getSelectedWindowIds())));
        stopAllWindowsButton.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::stopAll));
        unregisterSelectedWindowsButton.setOnAction(event -> runWindowCommandInBackground(() ->
                windowTaskControlService.unregisterWindows(getSelectedWindowIds())));
        unregisterAllWindowsButton.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::unregisterAll));
        refreshWindowButton.setOnAction(event -> refreshWindowPanel());

        HBox formRow = new HBox(8,
                new Label("窗口ID/前缀"), windowIdField,
                new Label("角色名"), windowRoleNameField,
                new Label("显示身份"), windowRoleComboBox,
                new Label("任务"), windowTaskTypeComboBox,
                registerWindowButton);

        HBox batchRow = new HBox(8,
                new Label("测试窗口数"), windowBatchCountField,
                registerTeamButton,
                scanGameWindowsButton,
                startIndependentWindowsButton,
                new Label("正式模式：每个窗口独立执行当前任务，不判断队长/队员"));

        HBox selectionRow = new HBox(8,
                refreshWindowButton,
                selectAllWindowsButton,
                clearWindowSelectionButton,
                unregisterSelectedWindowsButton,
                unregisterAllWindowsButton);

        HBox actionRow = new HBox(8,
                startWindowSelectedTaskButton,
                startByRoleButton,
                stopSelectedWindowsButton,
                stopAllWindowsButton);

        windowTable.setPrefHeight(145);
        windowTable.setMinHeight(120);
        windowTable.setMaxHeight(160);
        VBox wrapper = new VBox(6,
                new Label("多窗口控制"),
                windowSystemLabel,
                formRow,
                batchRow,
                selectionRow,
                actionRow,
                windowTable);
        wrapper.setPadding(new Insets(0, 0, 8, 0));
        return wrapper;
    }

    private void buildWindowTableColumns() {
        TableColumn<WindowTaskSnapshot, String> windowIdCol = new TableColumn<>("窗口ID");
        windowIdCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToDash(cell.getValue().getWindowId())));
        windowIdCol.setPrefWidth(95);

        TableColumn<WindowTaskSnapshot, String> geometryCol = new TableColumn<>("坐标/大小");
        geometryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToDash(cell.getValue().getGeometryText())));
        geometryCol.setPrefWidth(130);

        TableColumn<WindowTaskSnapshot, String> roleNameCol = new TableColumn<>("角色名");
        roleNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToDash(cell.getValue().getRoleName())));
        roleNameCol.setPrefWidth(85);

        TableColumn<WindowTaskSnapshot, String> roleCol = new TableColumn<>("显示身份");
        roleCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRoleDisplayName()));
        roleCol.setPrefWidth(80);

        TableColumn<WindowTaskSnapshot, String> selectedTaskCol = new TableColumn<>("已选任务");
        selectedTaskCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getSelectedTaskDisplayName()));
        selectedTaskCol.setPrefWidth(90);

        TableColumn<WindowTaskSnapshot, String> runningTaskCol = new TableColumn<>("运行任务");
        runningTaskCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRunningTaskDisplayName()));
        runningTaskCol.setPrefWidth(90);

        TableColumn<WindowTaskSnapshot, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatusDisplayName()));
        statusCol.setPrefWidth(85);

        TableColumn<WindowTaskSnapshot, String> runningCol = new TableColumn<>("运行中");
        runningCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().isRunning() ? "是" : "否"));
        runningCol.setPrefWidth(65);

        TableColumn<WindowTaskSnapshot, String> messageCol = new TableColumn<>("备注");
        messageCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToDash(cell.getValue().getLastMessage())));
        messageCol.setPrefWidth(260);

        windowTable.getColumns().setAll(List.of(windowIdCol, geometryCol, roleNameCol, roleCol, selectedTaskCol,
                runningTaskCol, statusCol, runningCol, messageCol));
    }

    private Parent buildRecordTable() {
        TableColumn<TaskRecordView, String> taskNameCol = new TableColumn<>("任务");
        taskNameCol.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        taskNameCol.setPrefWidth(100);

        TableColumn<TaskRecordView, String> resultCol = new TableColumn<>("结果");
        resultCol.setCellValueFactory(new PropertyValueFactory<>("result"));
        resultCol.setPrefWidth(80);

        TableColumn<TaskRecordView, String> startCol = new TableColumn<>("开始时间");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startCol.setPrefWidth(150);

        TableColumn<TaskRecordView, String> endCol = new TableColumn<>("结束时间");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        endCol.setPrefWidth(150);

        TableColumn<TaskRecordView, String> costCol = new TableColumn<>("耗时");
        costCol.setCellValueFactory(new PropertyValueFactory<>("costText"));
        costCol.setPrefWidth(80);

        TableColumn<TaskRecordView, String> messageCol = new TableColumn<>("备注");
        messageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        messageCol.setPrefWidth(260);

        recordTable.setPrefHeight(130);
        recordTable.setMinHeight(90);
        recordTable.getColumns().setAll(List.of(taskNameCol, resultCol, startCol, endCol, costCol, messageCol));
        return recordTable;
    }

    private Parent buildLogPanel() {
        VBox wrapper = new VBox(6, new Label("任务日志"), logList);
        wrapper.setPadding(new Insets(12, 0, 0, 0));
        wrapper.setPrefHeight(220);
        return wrapper;
    }

    private Parent buildStatusBar() {
        VBox box = new VBox(4, statusLabel, requestLabel, summaryLabel);
        box.setPadding(new Insets(4, 0, 0, 0));
        return box;
    }

    private void startSelectedTasksInBackground() {
        if (taskControlService.isRunning()) {
            addWindowLog("当前已有单窗口任务正在运行，请勿重复开始。");
            refreshDashboard();
            return;
        }

        List<String> selectedTaskCodes = getSelectedTaskCodesFromUi();
        if (selectedTaskCodes.isEmpty()) {
            addWindowLog("没有勾选任何任务，无法开始。");
            return;
        }

        lockRunOptions(true);
        startButton.setDisable(true);
        stopButton.setDisable(false);
        clearButton.setDisable(true);

        Thread worker = new Thread(() -> {
            try {
                taskUiActionService.startFromUi(
                        selectedTaskCodes,
                        loopCheckBox.isSelected(),
                        testModeCheckBox.isSelected(),
                        initGameWindowCheckBox.isSelected()
                );
            } finally {
                javafx.application.Platform.runLater(this::refreshDashboard);
            }
        }, "task-ui-start-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshDashboard()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    public void shutdownUi() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        taskUiActionService.stopFromUi();
        windowTaskControlService.stopAll();
    }

    private void applyRuntimeControls(TaskRuntimeStateView runtimeState) {
        boolean running = runtimeState != null && runtimeState.isRunning();
        boolean stopping = runtimeState != null && runtimeState.isStopping();
        boolean busy = running || stopping;

        if (startButton != null) {
            startButton.setText("开始");
            startButton.setDisable(busy);
        }
        if (stopButton != null) {
            stopButton.setText(stopping ? "停止中..." : "停止");
            stopButton.setDisable(!running || stopping);
        }
        if (clearButton != null) {
            clearButton.setDisable(busy);
        }
        if (refreshButton != null) {
            refreshButton.setDisable(false);
        }
        lockRunOptions(busy);
    }

    private void lockRunOptions(boolean locked) {
        if (loopCheckBox != null) {
            loopCheckBox.setDisable(locked);
        }
        if (testModeCheckBox != null) {
            testModeCheckBox.setDisable(locked);
        }
        if (initGameWindowCheckBox != null) {
            initGameWindowCheckBox.setDisable(locked);
        }
        if (taskBox != null) {
            for (javafx.scene.Node node : taskBox.getChildren()) {
                if (node instanceof CheckBox checkBox) {
                    checkBox.setDisable(locked);
                }
            }
        }
    }

    private void updateStatusFromUiSelection() {
        if (statusLabel == null) {
            return;
        }
        String status = taskControlService.isRunning() ? "运行中" : "空闲";
        int selectedCount = getSelectedTaskCodesFromUi().size();
        statusLabel.setText("状态：" + status + " | 已选择任务：" + selectedCount
                + " | 循环=" + loopCheckBox.isSelected()
                + " | 测试模式=" + testModeCheckBox.isSelected()
                + " | 初始化窗口=" + initGameWindowCheckBox.isSelected());
    }

    private void updateRuntimeState(TaskRuntimeStateView runtimeState) {
        if (runtimeState == null) {
            updateStatusFromUiSelection();
            return;
        }
        statusLabel.setText("状态：" + nullToDash(runtimeState.getStatusText())
                + " | status=" + nullToDash(runtimeState.getStatus())
                + " | 耗时=" + nullToDash(runtimeState.getElapsedText())
                + " | started=" + nullToDash(runtimeState.getStartedAt())
                + " | finished=" + nullToDash(runtimeState.getFinishedAt()));
        requestLabel.setText("请求：" + nullToDash(runtimeState.getRequestText()));
        summaryLabel.setText("结果：" + nullToDash(runtimeState.getSummaryText()));
    }

    private void updatePlanPreview(TaskPlanView planView) {
        if (planView == null) {
            planSummaryLabel.setText("计划：-");
            planExecutableLabel.setText("执行：-");
            planIgnoredLabel.setText("忽略：-");
            planOptionsLabel.setText("选项：-");
            planWarningLabel.setText("警告：-");
            return;
        }
        planSummaryLabel.setText("计划：" + nullToDash(planView.getSummaryText()));
        planExecutableLabel.setText("执行(" + planView.getExecutableCount() + ")：" + nullToDash(planView.getExecutableTasksText()));
        planIgnoredLabel.setText("忽略(" + planView.getIgnoredCount() + ")：" + nullToDash(planView.getIgnoredTasksText()));
        planOptionsLabel.setText("选项：" + nullToDash(planView.getOptionsText()));
        planWarningLabel.setText("警告：" + nullToDash(planView.getWarningText()));
    }

    private String nullToDash(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private List<String> getSelectedTaskCodesFromUi() {
        List<String> selectedTaskCodes = new ArrayList<>();
        if (taskBox == null) {
            return selectedTaskCodes;
        }
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

    private Map<String, Boolean> getCurrentTaskSelectionMap() {
        Map<String, Boolean> selectionMap = new HashMap<>();
        if (taskBox == null) {
            return selectionMap;
        }
        for (javafx.scene.Node node : taskBox.getChildren()) {
            if (node instanceof CheckBox checkBox) {
                Object userData = checkBox.getUserData();
                if (userData instanceof String taskCode && !taskCode.isBlank()) {
                    selectionMap.put(taskCode, checkBox.isSelected());
                }
            }
        }
        return selectionMap;
    }

    private List<String> getSelectedWindowIds() {
        if (windowTable == null) {
            return List.of();
        }
        return windowTable.getSelectionModel().getSelectedItems().stream()
                .map(WindowTaskSnapshot::getWindowId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    private void registerOrRefreshWindowFromUi() {
        WindowRegistrationRequest request = WindowRegistrationRequest.of(
                windowIdField.getText(),
                windowRoleComboBox.getValue(),
                windowRoleNameField.getText(),
                windowTaskTypeComboBox.getValue()
        );
        handleWindowCommandResult(windowTaskControlService.registerWindows(List.of(request)));
    }

    private void registerTeamFromUi() {
        int count = windowRegistrationBatchBuilder.parseCount(windowBatchCountField.getText());
        List<WindowRegistrationRequest> requests = windowRegistrationBatchBuilder.buildIndependentWindows(
                windowIdField.getText(),
                windowRoleNameField.getText(),
                count,
                windowTaskTypeComboBox.getValue()
        );
        handleWindowCommandResult(windowTaskControlService.registerWindows(requests));
    }

    private void selectAllWindows() {
        if (windowTable != null) {
            windowTable.getSelectionModel().selectAll();
        }
    }

    private void clearWindowSelection() {
        if (windowTable != null) {
            windowTable.getSelectionModel().clearSelection();
        }
    }

    private void runWindowCommandInBackground(WindowCommand command) {
        setWindowButtonsDisabled(true);
        Thread worker = new Thread(() -> {
            WindowTaskCommandResult result;
            try {
                result = command.execute();
            } catch (Exception e) {
                result = WindowTaskCommandResult.empty("窗口命令异常：" + e.getMessage(), windowTaskControlService.getSnapshots());
            }
            WindowTaskCommandResult finalResult = result;
            javafx.application.Platform.runLater(() -> {
                handleWindowCommandResult(finalResult);
                setWindowButtonsDisabled(false);
            });
        }, "window-task-ui-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void setWindowButtonsDisabled(boolean disabled) {
        if (registerWindowButton != null) {
            registerWindowButton.setDisable(disabled);
        }
        if (registerTeamButton != null) {
            registerTeamButton.setDisable(disabled);
        }
        if (scanGameWindowsButton != null) {
            scanGameWindowsButton.setDisable(disabled);
        }
        if (startIndependentWindowsButton != null) {
            startIndependentWindowsButton.setDisable(disabled);
        }
        if (selectAllWindowsButton != null) {
            selectAllWindowsButton.setDisable(disabled);
        }
        if (clearWindowSelectionButton != null) {
            clearWindowSelectionButton.setDisable(disabled);
        }
        if (startByRoleButton != null) {
            startByRoleButton.setDisable(disabled);
        }
        if (startWindowSelectedTaskButton != null) {
            startWindowSelectedTaskButton.setDisable(disabled);
        }
        if (stopSelectedWindowsButton != null) {
            stopSelectedWindowsButton.setDisable(disabled);
        }
        if (stopAllWindowsButton != null) {
            stopAllWindowsButton.setDisable(disabled);
        }
        if (unregisterSelectedWindowsButton != null) {
            unregisterSelectedWindowsButton.setDisable(disabled);
        }
        if (unregisterAllWindowsButton != null) {
            unregisterAllWindowsButton.setDisable(disabled);
        }
        if (refreshWindowButton != null) {
            refreshWindowButton.setDisable(disabled);
        }
    }

    private void handleWindowCommandResult(WindowTaskCommandResult result) {
        if (result == null) {
            refreshWindowPanel();
            return;
        }
        addWindowLog(result.getMessage());
        if (result.hasAssignments()) {
            for (var assignment : result.getAssignments()) {
                addWindowLog("测试身份分配：" + nullToDash(assignment.getWindowId())
                        + " | " + assignment.getRoleDisplayName()
                        + " -> " + assignment.getTaskDisplayName()
                        + " | " + assignment.getReason());
            }
        }
        if (result.hasDetails()) {
            for (WindowTaskCommandDetail detail : result.getDetails()) {
                addWindowLog((detail.isSuccess() ? "成功：" : "失败：")
                        + nullToDash(detail.getWindowId()) + " | " + nullToDash(detail.getMessage()));
            }
        }
        refreshWindowPanel();
        refreshDashboard();
    }

    private void addWindowLog(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String line = "[" + LocalTime.now().format(UI_LOG_TIME_FORMATTER) + "] [多窗口] " + message;
        windowCommandLogs.add(0, line);
        while (windowCommandLogs.size() > MAX_WINDOW_COMMAND_LOGS) {
            windowCommandLogs.remove(windowCommandLogs.size() - 1);
        }
        if (logList != null) {
            renderLogList(List.of());
        }
    }

    private void clearWindowLogs() {
        windowCommandLogs.clear();
    }

    private void refreshDashboard() {
        List<String> selectedTaskCodes = getSelectedTaskCodesFromUi();
        Map<String, Boolean> currentSelection = getCurrentTaskSelectionMap();
        TaskDashboardView dashboard = taskViewService.getDashboardView(
                selectedTaskCodes,
                loopCheckBox.isSelected(),
                testModeCheckBox.isSelected(),
                initGameWindowCheckBox.isSelected()
        );
        TaskRuntimeStateView runtimeState = dashboard.getRuntimeState();
        refreshTaskOptions(dashboard.getTaskOptions(), currentSelection, runtimeState);
        refreshRecordTable(dashboard.getRecentRecords());
        refreshLogList(dashboard.getRecentLogs());
        updateRuntimeState(runtimeState);
        updatePlanPreview(dashboard.getPlanView());
        applyRuntimeControls(runtimeState);
        refreshWindowPanel();
    }

    private void refreshWindowPanel() {
        if (windowTable == null || windowSystemLabel == null) {
            return;
        }
        WindowSystemSnapshot snapshot = windowTaskControlService.getSystemSnapshot();
        List<String> selectedWindowIds = getSelectedWindowIds();
        windowTable.getItems().setAll(snapshot.getWindows());
        for (WindowTaskSnapshot window : windowTable.getItems()) {
            if (selectedWindowIds.contains(window.getWindowId())) {
                windowTable.getSelectionModel().select(window);
            }
        }
        windowSystemLabel.setText("窗口：已注册 " + snapshot.getRegisteredWindowCount()
                + " / " + snapshot.getMaxWindowCount()
                + " | 运行中 " + snapshot.getRunningWindowCount()
                + " | 空闲 " + snapshot.getIdleWindowCount()
                + " | 剩余 " + snapshot.getRemainingWindowCapacity()
                + " | 已满=" + snapshot.isCapacityFull());
    }

    private void refreshTaskOptions(List<TaskOptionView> options,
                                    Map<String, Boolean> currentSelection,
                                    TaskRuntimeStateView runtimeState) {
        boolean busy = runtimeState != null && runtimeState.isBusy();
        taskBox.getChildren().clear();
        for (TaskOptionView option : options) {
            CheckBox checkBox = new CheckBox(option.getTaskName() + " (" + option.getTaskCode() + ")");
            checkBox.setUserData(option.getTaskCode());
            checkBox.setSelected(currentSelection.getOrDefault(option.getTaskCode(), option.isSelected()));
            checkBox.setDisable(busy || !option.isEnabled());
            checkBox.setOnAction(event -> refreshDashboard());
            taskBox.getChildren().add(checkBox);
        }
    }

    private void refreshRecordTable(List<TaskRecordView> records) {
        recordTable.getItems().setAll(records);
    }

    private void refreshLogList(List<TaskLogView> logs) {
        renderLogList(logs);
    }

    private void renderLogList(List<TaskLogView> logs) {
        logList.getItems().clear();
        logList.getItems().addAll(windowCommandLogs);
        for (TaskLogView log : logs) {
            String taskText = log.getTaskCode() == null || log.getTaskCode().isBlank()
                    ? ""
                    : " [" + log.getTaskCode() + "/" + nullToDash(log.getTaskName()) + "]";
            logList.getItems().add("[" + log.getTime() + "] " + log.getType() + taskText + " " + log.getMessage());
        }
    }

    @FunctionalInterface
    private interface WindowCommand {
        WindowTaskCommandResult execute();
    }
}
