package com.bot.dhxy.ui;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.auth.LicenseAuthResult;
import com.bot.dhxy.auth.LicenseAuthService;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.vision.MapSurveyService;
import com.bot.dhxy.vision.PlayerNameOcrDebugService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.control.WindowRegistrationBatchBuilder;
import com.bot.dhxy.window.control.WindowSystemSnapshot;
import com.bot.dhxy.window.control.WindowTaskCommandDetail;
import com.bot.dhxy.window.control.WindowTaskCommandResult;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.bot.dhxy.window.control.WindowTaskStartRequest;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.diagnostics.WindowCaptureExperimentService;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.diagnostics.WindowMessageInputExperimentService;
import com.bot.dhxy.window.execution.WindowTaskQueue;
import com.bot.dhxy.window.execution.WindowTaskSnapshot;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.model.WindowRuntimeStatus;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class MainWindowController {

    private static final DateTimeFormatter UI_LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter UI_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private static final Pattern WINDOW_IDENTITY_PATTERN = Pattern.compile("-\\s+(.+?)\\s+-\\s+(.+?)\\s*[\\(（]ID[:：]\\s*(\\d+)[\\)）]");
    private static final int MAX_WINDOW_COMMAND_LOGS = 120;

    private final WindowTaskControlService windowTaskControlService;
    private final WindowRegistrationBatchBuilder windowRegistrationBatchBuilder;
    private final GameWindowRegistrationService gameWindowRegistrationService;
    private final WindowCaptureExperimentService windowCaptureExperimentService;
    private final WindowInteractionMetricsService windowInteractionMetricsService;
    private final WindowMessageInputExperimentService windowMessageInputExperimentService;
    private final LicenseAuthService licenseAuthService;
    private final BotProperties botProperties;
    private final MapSurveyService mapSurveyService;
    private final PlayerNameOcrDebugService playerNameOcrDebugService;
    private final CoordinateHelper coordinateHelper;

    private TableView<WindowTaskSnapshot> windowTable;
    private TextField windowIdField;
    private TextField windowRoleNameField;
    private TextField windowBatchCountField;
    private ComboBox<TaskType> windowTaskTypeComboBox;
    private ComboBox<TaskType> queueTaskTypeComboBox;
    private ComboBox<WindowTableFilter> windowFilterComboBox;
    private TextField windowSearchField;
    private Button registerWindowButton;
    private Button registerTeamButton;
    private Button scanGameWindowsButton;
    private Button startIndependentWindowsButton;
    private Button applySelectedTaskButton;
    private Button addCurrentTaskToQueueButton;
    private Button addQueueTaskButton;
    private Button removeQueueTaskButton;
    private Button moveQueueTaskUpButton;
    private Button moveQueueTaskDownButton;
    private Button clearQueueButton;
    private Button startQueueButton;
    private Button presetCurrentTaskQueueButton;
    private Button presetFiveRingQueueButton;
    private Button presetAutoBattleQueueButton;
    private Button presetFiveRingAutoBattleQueueButton;
    private Button presetDebugCoordinateQueueButton;
    private Button setDebugCoordinateTaskButton;
    private Button windowCaptureExperimentButton;
    private Button backgroundAltQExperimentButton;
    private Button backgroundAlt1ExperimentButton;
    private Button backgroundCenterClickExperimentButton;
    private Button backgroundCenterRightClickExperimentButton;
    private Button backgroundChildRightClickExperimentButton;
    private Button interactionMetricsDashboardButton;
    private Button playerNameOcrDebugButton;
    private Button saveMapLabelSampleButton;
    private Button testMapLabelSampleButton;
    private Button recordCameraLeftButton;
    private Button recordCameraRightButton;
    private Button recordCameraTopButton;
    private Button recordCameraBottomButton;
    private Button recordCameraCenterButton;
    private Button testProjectedPlayerPointButton;
    private Button recordPlayerPointCorrectionButton;
    private Button testCorrectedPlayerPointButton;
    private Button undoPlayerPointCorrectionButton;
    private Button selectAllWindowsButton;
    private Button selectRunningWindowsButton;
    private Button selectIdleWindowsButton;
    private Button selectProblemWindowsButton;
    private Button selectBoundWindowsButton;
    private Button selectUnboundWindowsButton;
    private Button clearWindowSelectionButton;
    private Button startCurrentTaskButton;
    private Button startWindowSelectedTaskButton;
    private Button startTeamRoleDebugButton;
    private Button pauseSelectedWindowsButton;
    private Button resumeSelectedWindowsButton;
    private Button pauseAllWindowsButton;
    private Button resumeAllWindowsButton;
    private Button stopSelectedWindowsButton;
    private Button stopAllWindowsButton;
    private Button unregisterSelectedWindowsButton;
    private Button unregisterAllWindowsButton;
    private Button refreshWindowButton;
    private MenuButton windowSelectionMenuButton;
    private MenuButton windowManageMenuButton;
    private MenuButton runControlMenuButton;
    private MenuItem selectAllWindowsMenuItem;
    private MenuItem selectRunningWindowsMenuItem;
    private MenuItem selectIdleWindowsMenuItem;
    private MenuItem selectProblemWindowsMenuItem;
    private MenuItem selectBoundWindowsMenuItem;
    private MenuItem selectUnboundWindowsMenuItem;
    private MenuItem clearWindowSelectionMenuItem;
    private MenuItem unregisterSelectedWindowsMenuItem;
    private MenuItem unregisterAllWindowsMenuItem;
    private MenuItem pauseSelectedWindowsMenuItem;
    private MenuItem resumeSelectedWindowsMenuItem;
    private MenuItem pauseAllWindowsMenuItem;
    private MenuItem resumeAllWindowsMenuItem;
    private MenuItem stopSelectedWindowsMenuItem;
    private MenuItem stopAllWindowsMenuItem;
    private CheckBox darkModeCheckBox;
    private CheckBox playerHpSupplyCheckBox;
    private CheckBox playerMpSupplyCheckBox;
    private CheckBox petHpSupplyCheckBox;
    private CheckBox petMpSupplyCheckBox;
    private ComboBox<Integer> playerHpThresholdComboBox;
    private ComboBox<Integer> playerMpThresholdComboBox;
    private ComboBox<Integer> petHpThresholdComboBox;
    private ComboBox<Integer> petMpThresholdComboBox;
    private Button applySupplyConfigButton;
    private TextField xiuluoRunCountField;
    private ComboBox<Integer> wuhuanRunCountComboBox;
    private TextField fivefoldRunCountField;
    private TextField tiantingRunCountField;
    private TextField zhuaguiRunCountField;
    private CheckBox summonSkillCleanEnabledCheckBox;
    private CheckBox taskStartupPreparationEnabledCheckBox;
    private ComboBox<Integer> summonSkillIntervalMinutesComboBox;
    private Button applyGameConfigButton;
    private Button clearButton;
    private Label windowSystemLabel;
    private Label windowActionHintLabel;
    private Label windowMetricLabel;
    private Label runningMetricLabel;
    private Label problemMetricLabel;
    private Label queueSummaryLabel;
    private Label taskSelectionSummaryLabel;
    private Label selectedWindowCountLabel;
    private HBox taskCountEditorBar;
    private Label taskCountEditorTitleLabel;
    private TextField taskCountEditorField;
    private Label taskCountEditorUnitLabel;
    private TextField mapCalibratorMapNameField;
    private Label mapCalibratorHintLabel;
    private BorderPane rootPane;
    private Parent selectedWindowDetailPanel;
    private VBox selectedWindowDetailBox;
    private ListView<String> queueTaskList;
    private ListView<String> logList;
    private Timeline autoRefreshTimeline;

    private final List<String> windowCommandLogs = new ArrayList<>();
    private final List<TaskType> pendingTaskQueue = new ArrayList<>();
    private final Map<TaskType, String> taskCountSummaries = createDefaultTaskCountSummaries();
    private final List<Button> taskTileButtons = new ArrayList<>();
    private List<String> pendingAutoSelectedWindowIds = List.of();
    private TaskType activeTaskCountType;
    private Timeline taskCountHoldTimeline;
    private boolean taskCountHoldRepeated;
    private boolean windowCommandRunning;
    private boolean selectedWindowDetailExpanded;

    private Map<TaskType, String> createDefaultTaskCountSummaries() {
        Map<TaskType, String> summaries = new EnumMap<>(TaskType.class);
        summaries.put(TaskType.WUHuan, "1轮");
        summaries.put(TaskType.XIULUO, "1次");
        summaries.put(TaskType.AUTO_BATTLE, "60分");
        summaries.put(TaskType.DEBUG_COORDINATE, "手动");
        summaries.put(TaskType.DEBUG_MAP_CALIBRATOR, "2点");
        summaries.put(TaskType.DEBUG_TEAM_ROLE, "1次");
        summaries.put(TaskType.DEBUG_XIULUO_MOCK_OBJECTIVE, "瑶池");
        return summaries;
    }

    public Parent buildView() {
        initControls();

        rootPane = new BorderPane();
        rootPane.getStyleClass().add("app-root");
        rootPane.setTop(buildTopBar());
        rootPane.setCenter(buildMainShell());

        refreshDashboard();
        startAutoRefresh();
        return rootPane;
    }

    private void initControls() {
        clearButton = new Button("清空日志");
        logList = new ListView<>();
        windowTable = new TableView<>();
        windowTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        windowIdField = new TextField("window-1");
        windowIdField.setPrefWidth(110);
        windowRoleNameField = new TextField("角色A");
        windowRoleNameField.setPrefWidth(90);
        windowBatchCountField = new TextField("5");
        windowBatchCountField.setPrefWidth(48);

        windowTaskTypeComboBox = new ComboBox<>();
        windowTaskTypeComboBox.getItems().setAll(TaskType.values());
        windowTaskTypeComboBox.setValue(TaskType.WUHuan);

        queueTaskTypeComboBox = new ComboBox<>();
        queueTaskTypeComboBox.getItems().setAll(selectableTaskTypes());
        queueTaskTypeComboBox.setValue(TaskType.WUHuan);

        windowFilterComboBox = new ComboBox<>();
        windowFilterComboBox.getItems().setAll(WindowTableFilter.values());
        windowFilterComboBox.setValue(WindowTableFilter.ALL);
        windowFilterComboBox.setPrefWidth(106);
        windowSearchField = new TextField();
        windowSearchField.setPromptText("搜索角色 / ID / 服务器");
        windowSearchField.setPrefWidth(220);
        configureWindowComboBoxText();

        registerWindowButton = new Button("注册/刷新窗口");
        registerTeamButton = new Button("测试注册窗口");
        scanGameWindowsButton = new Button("扫描游戏窗口");
        startIndependentWindowsButton = new Button("一键启动独立窗口");
        applySelectedTaskButton = new Button("设置选中任务");
        addCurrentTaskToQueueButton = new Button("当前任务入队");
        addQueueTaskButton = new Button("加入队列");
        removeQueueTaskButton = new Button("移除队列项");
        moveQueueTaskUpButton = new Button("上移");
        moveQueueTaskDownButton = new Button("下移");
        clearQueueButton = new Button("清空队列");
        startQueueButton = new Button("启动队列");
        presetCurrentTaskQueueButton = new Button("预设:当前任务");
        presetFiveRingQueueButton = new Button("预设:五环");
        presetAutoBattleQueueButton = new Button("预设:自动战斗");
        presetFiveRingAutoBattleQueueButton = new Button("预设:五环+自动战斗");
        presetDebugCoordinateQueueButton = new Button("坐标调试入队");
        setDebugCoordinateTaskButton = new Button("当前任务设为坐标调试");
        windowCaptureExperimentButton = new Button("后台截图实验");
        backgroundAltQExperimentButton = new Button("后台按键 Alt+Q");
        backgroundAlt1ExperimentButton = new Button("后台按键 Alt+1");
        backgroundCenterClickExperimentButton = new Button("后台鼠标中心左键");
        backgroundCenterRightClickExperimentButton = new Button("后台鼠标中心右键");
        backgroundChildRightClickExperimentButton = new Button("子窗口中心右键");
        interactionMetricsDashboardButton = new Button("统计 Dashboard");
        playerNameOcrDebugButton = new Button("本地OCR测名字");
        saveMapLabelSampleButton = new Button("保存地图名样本");
        testMapLabelSampleButton = new Button("测试地图名");
        recordCameraLeftButton = new Button("记左边界");
        recordCameraRightButton = new Button("记右边界");
        recordCameraTopButton = new Button("记上边界");
        recordCameraBottomButton = new Button("记下边界");
        recordCameraCenterButton = new Button("记中心点");
        testProjectedPlayerPointButton = new Button("测角色点");
        recordPlayerPointCorrectionButton = new Button("记修正点");
        testCorrectedPlayerPointButton = new Button("测修正点");
        undoPlayerPointCorrectionButton = new Button("撤销上次记录");
        selectAllWindowsButton = new Button("全选窗口");
        selectRunningWindowsButton = new Button("选运行中");
        selectIdleWindowsButton = new Button("选空闲");
        selectProblemWindowsButton = new Button("选异常/停止");
        selectBoundWindowsButton = new Button("选已绑定");
        selectUnboundWindowsButton = new Button("选未绑定");
        clearWindowSelectionButton = new Button("取消选择");
        startCurrentTaskButton = new Button("启动当前任务");
        startWindowSelectedTaskButton = new Button("启动已选任务");
        startTeamRoleDebugButton = new Button("队伍识别测试");
        pauseSelectedWindowsButton = new Button("暂停选中窗口");
        resumeSelectedWindowsButton = new Button("继续选中窗口");
        pauseAllWindowsButton = new Button("暂停全部窗口");
        resumeAllWindowsButton = new Button("继续全部窗口");
        stopSelectedWindowsButton = new Button("停止选中窗口");
        stopAllWindowsButton = new Button("停止全部窗口");
        unregisterSelectedWindowsButton = new Button("移除选中窗口");
        unregisterAllWindowsButton = new Button("移除全部窗口");
        refreshWindowButton = new Button("刷新窗口表");
        windowSelectionMenuButton = new MenuButton("选择窗口");
        windowManageMenuButton = new MenuButton("窗口管理");
        runControlMenuButton = new MenuButton("运行控制");
        selectAllWindowsMenuItem = new MenuItem("全选窗口");
        selectRunningWindowsMenuItem = new MenuItem("选择运行中");
        selectIdleWindowsMenuItem = new MenuItem("选择空闲");
        selectProblemWindowsMenuItem = new MenuItem("选择异常/停止");
        selectBoundWindowsMenuItem = new MenuItem("选择已绑定");
        selectUnboundWindowsMenuItem = new MenuItem("选择未绑定");
        clearWindowSelectionMenuItem = new MenuItem("取消选择");
        unregisterSelectedWindowsMenuItem = new MenuItem("移除选中窗口");
        unregisterAllWindowsMenuItem = new MenuItem("移除全部窗口");
        pauseSelectedWindowsMenuItem = new MenuItem("暂停选中窗口");
        resumeSelectedWindowsMenuItem = new MenuItem("继续选中窗口");
        pauseAllWindowsMenuItem = new MenuItem("暂停全部窗口");
        resumeAllWindowsMenuItem = new MenuItem("继续全部窗口");
        stopSelectedWindowsMenuItem = new MenuItem("停止选中窗口");
        stopAllWindowsMenuItem = new MenuItem("停止全部窗口");
        windowSelectionMenuButton.getItems().setAll(
                selectAllWindowsMenuItem,
                selectRunningWindowsMenuItem,
                selectIdleWindowsMenuItem,
                selectProblemWindowsMenuItem,
                selectBoundWindowsMenuItem,
                selectUnboundWindowsMenuItem,
                new SeparatorMenuItem(),
                clearWindowSelectionMenuItem);
        windowManageMenuButton.getItems().setAll(
                unregisterSelectedWindowsMenuItem,
                unregisterAllWindowsMenuItem);
        runControlMenuButton.getItems().setAll(
                pauseSelectedWindowsMenuItem,
                resumeSelectedWindowsMenuItem,
                pauseAllWindowsMenuItem,
                resumeAllWindowsMenuItem,
                new SeparatorMenuItem(),
                stopSelectedWindowsMenuItem,
                stopAllWindowsMenuItem);
        darkModeCheckBox = new CheckBox("深色模式");
        playerHpSupplyCheckBox = new CheckBox("人物血");
        playerHpSupplyCheckBox.setSelected(botProperties.isPlayerHpSupplyEnabled());
        playerMpSupplyCheckBox = new CheckBox("人物法");
        playerMpSupplyCheckBox.setSelected(botProperties.isPlayerMpSupplyEnabled());
        petHpSupplyCheckBox = new CheckBox("召唤兽血");
        petHpSupplyCheckBox.setSelected(botProperties.isPetHpSupplyEnabled());
        petMpSupplyCheckBox = new CheckBox("召唤兽法");
        petMpSupplyCheckBox.setSelected(botProperties.isPetMpSupplyEnabled());
        playerHpThresholdComboBox = buildSupplyThresholdComboBox(botProperties.getPlayerHpSupplyThreshold());
        playerMpThresholdComboBox = buildSupplyThresholdComboBox(botProperties.getPlayerMpSupplyThreshold());
        petHpThresholdComboBox = buildSupplyThresholdComboBox(botProperties.getPetHpSupplyThreshold());
        petMpThresholdComboBox = buildSupplyThresholdComboBox(botProperties.getPetMpSupplyThreshold());
        applySupplyConfigButton = new Button("应用补给配置");
        xiuluoRunCountField = buildTaskRunCountField(botProperties.getXiuluoMaxRuns());
        wuhuanRunCountComboBox = buildWuhuanRunCountComboBox(botProperties.getWuhuanMaxRuns());
        fivefoldRunCountField = buildTaskRunCountField(botProperties.getFivefoldMaxRuns());
        tiantingRunCountField = buildTaskRunCountField(botProperties.getTiantingMaxRuns());
        zhuaguiRunCountField = buildTaskRunCountField(botProperties.getZhuaguiMaxRuns());
        syncTaskCountSummariesFromProperties();
        summonSkillCleanEnabledCheckBox = new CheckBox("启用三技能维护");
        summonSkillCleanEnabledCheckBox.setSelected(botProperties.isSummonSkillCleanEnabled());
        taskStartupPreparationEnabledCheckBox = new CheckBox("任务启动前置检查");
        taskStartupPreparationEnabledCheckBox.setSelected(botProperties.isTaskStartupPreparationEnabled());
        summonSkillIntervalMinutesComboBox = buildSummonSkillIntervalComboBox(botProperties.getSummonSkillCleanIntervalMs());
        applyGameConfigButton = new Button("应用游戏设置");
        windowSystemLabel = new Label("窗口：");
        windowActionHintLabel = new Label("操作提示：-");
        windowActionHintLabel.setWrapText(true);
        windowMetricLabel = new Label("-");
        runningMetricLabel = new Label("-");
        problemMetricLabel = new Label("-");
        queueSummaryLabel = new Label("待提交队列：-");
        queueSummaryLabel.setWrapText(true);
        selectedWindowCountLabel = new Label("已选窗口：0");
        selectedWindowDetailBox = new VBox(0);
        selectedWindowDetailBox.getStyleClass().add("detail-list");
        queueTaskList = new ListView<>();
        queueTaskList.setPrefHeight(92);
        queueTaskList.setMinHeight(64);
        queueTaskList.getStyleClass().add("queue-list");
        mapCalibratorMapNameField = new TextField(botProperties.getDebugMapCalibratorMapName());
        mapCalibratorMapNameField.setPromptText("地图校准名，例如：瑶池");
        mapCalibratorMapNameField.setPrefWidth(180);
        mapCalibratorHintLabel = new Label("地图测绘：同一个地图名会用于小地图名字样本和镜头边界；记录边界时先点按钮，然后3秒内把鼠标移到角色身上/脚下。旧“地图校准”任务仍用于 maps.json 小地图点击变换。");
        mapCalibratorHintLabel.setWrapText(true);
        mapCalibratorHintLabel.getStyleClass().add("queue-summary");
        logList.getStyleClass().add("command-log");
        refreshPendingTaskQueueView();
        windowTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<WindowTaskSnapshot>) change -> {
            windowTable.refresh();
            refreshSelectionDependentUi();
        });
        configureVisualStyles();
    }

    private void configureWindowComboBoxText() {
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
        queueTaskTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(TaskType taskType) {
                return taskType == null ? "-" : taskType.getDisplayName();
            }

            @Override
            public TaskType fromString(String string) {
                return TaskType.UNKNOWN;
            }
        });
        windowFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WindowTableFilter filter) {
                return filter == null ? "-" : filter.getDisplayName();
            }

            @Override
            public WindowTableFilter fromString(String string) {
                return WindowTableFilter.ALL;
            }
        });
    }

    private ComboBox<Integer> buildSupplyThresholdComboBox(int value) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(30, 50, 70);
        comboBox.setValue(normalizeSupplyThreshold(value));
        comboBox.setPrefWidth(70);
        return comboBox;
    }

    /*
     * Task counts are UI-level game settings: the combo stores the value in BotProperties, while
     * each task may opt in later. Keeping all counts in one panel avoids scattering per-task knobs
     * across the main control screen.
     */
    private TextField buildTaskRunCountField(int value) {
        TextField field = new TextField(String.valueOf(normalizeRunCount(value)));
        field.setPrefWidth(76);
        field.setPromptText("次数");
        return field;
    }

    private ComboBox<Integer> buildWuhuanRunCountComboBox(int value) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(1, 2);
        comboBox.setValue(normalizeWuhuanRunCount(value));
        comboBox.setPrefWidth(76);
        return comboBox;
    }

    /*
     * Summon-skill cleanup is time based in BotProperties, but the user-facing setting is minutes.
     * The conversion stays in the UI so the backend property remains millisecond-friendly.
     */
    private ComboBox<Integer> buildSummonSkillIntervalComboBox(long intervalMs) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(3, 5, 10, 15, 20, 30, 60);
        comboBox.setValue(normalizeSummonSkillIntervalMinutes(intervalMs));
        comboBox.setPrefWidth(86);
        return comboBox;
    }

    private Parent buildSupplyConfigPanel() {
        FlowPane playerRow = buildControlRow(
                new Label("人物补给"),
                playerHpSupplyCheckBox,
                playerHpThresholdComboBox,
                playerMpSupplyCheckBox,
                playerMpThresholdComboBox);
        FlowPane petRow = buildControlRow(
                new Label("召唤兽补给"),
                petHpSupplyCheckBox,
                petHpThresholdComboBox,
                petMpSupplyCheckBox,
                petMpThresholdComboBox,
                applySupplyConfigButton);
        return buildSection("补给配置", playerRow, petRow);
    }

    private void applySupplyConfigFromUi() {
        botProperties.setPlayerHpSupplyEnabled(playerHpSupplyCheckBox.isSelected());
        botProperties.setPlayerHpSupplyThreshold(normalizeSupplyThreshold(playerHpThresholdComboBox.getValue()));
        botProperties.setPlayerMpSupplyEnabled(playerMpSupplyCheckBox.isSelected());
        botProperties.setPlayerMpSupplyThreshold(normalizeSupplyThreshold(playerMpThresholdComboBox.getValue()));
        botProperties.setPetHpSupplyEnabled(petHpSupplyCheckBox.isSelected());
        botProperties.setPetHpSupplyThreshold(normalizeSupplyThreshold(petHpThresholdComboBox.getValue()));
        botProperties.setPetMpSupplyEnabled(petMpSupplyCheckBox.isSelected());
        botProperties.setPetMpSupplyThreshold(normalizeSupplyThreshold(petMpThresholdComboBox.getValue()));

        addWindowLog("补给配置已应用：人物血=" + supplyText(botProperties.isPlayerHpSupplyEnabled(), botProperties.getPlayerHpSupplyThreshold())
                + " 人物法=" + supplyText(botProperties.isPlayerMpSupplyEnabled(), botProperties.getPlayerMpSupplyThreshold())
                + " 召唤兽血=" + supplyText(botProperties.isPetHpSupplyEnabled(), botProperties.getPetHpSupplyThreshold())
                + " 召唤兽法=" + supplyText(botProperties.isPetMpSupplyEnabled(), botProperties.getPetMpSupplyThreshold()));
        renderLogList();
    }

    private void applyGameConfigFromUi() {
        int xiuluoRuns = readRunCountField(xiuluoRunCountField);
        int wuhuanRuns = normalizeWuhuanRunCount(wuhuanRunCountComboBox.getValue());
        int fivefoldRuns = readRunCountField(fivefoldRunCountField);
        int tiantingRuns = readRunCountField(tiantingRunCountField);
        int zhuaguiRuns = readRunCountField(zhuaguiRunCountField);
        botProperties.setXiuluoMaxRuns(xiuluoRuns);
        botProperties.setWuhuanMaxRuns(wuhuanRuns);
        botProperties.setFivefoldMaxRuns(fivefoldRuns);
        botProperties.setTiantingMaxRuns(tiantingRuns);
        botProperties.setZhuaguiMaxRuns(zhuaguiRuns);
        botProperties.setSummonSkillCleanEnabled(summonSkillCleanEnabledCheckBox.isSelected());
        botProperties.setTaskStartupPreparationEnabled(taskStartupPreparationEnabledCheckBox.isSelected());
        botProperties.setSummonSkillCleanIntervalMs(normalizeSummonSkillIntervalMinutes(
                summonSkillIntervalMinutesComboBox.getValue()) * 60_000L);
        syncTaskCountSummariesFromProperties();

        addWindowLog("游戏设置已应用：修罗=" + botProperties.getXiuluoMaxRuns()
                + " 五环=" + botProperties.getWuhuanMaxRuns()
                + " 五倍=" + botProperties.getFivefoldMaxRuns()
                + " 天庭=" + botProperties.getTiantingMaxRuns()
                + " 抓鬼=" + botProperties.getZhuaguiMaxRuns()
                + " 前置检查=" + (botProperties.isTaskStartupPreparationEnabled() ? "开" : "关")
                + " 三技能=" + (botProperties.isSummonSkillCleanEnabled() ? "开" : "关")
                + "/" + normalizeSummonSkillIntervalMinutes(botProperties.getSummonSkillCleanIntervalMs()) + "分钟");
        renderLogList();
    }

    private boolean syncDebugTaskConfigFromUi(List<TaskType> queue) {
        if (mapCalibratorMapNameField != null) {
            botProperties.setDebugMapCalibratorMapName(mapCalibratorMapNameField.getText() == null
                    ? ""
                    : mapCalibratorMapNameField.getText().trim());
        }
        if (queue == null || !queue.contains(TaskType.DEBUG_MAP_CALIBRATOR)) {
            return true;
        }
        if (hasText(botProperties.getDebugMapCalibratorMapName())) {
            return true;
        }

        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("地图校准");
        dialog.setHeaderText("请输入要写入 maps.json 的地图名");
        dialog.setContentText("地图名");
        Optional<String> result = dialog.showAndWait();
        String mapName = result.map(String::trim).orElse("");
        if (!hasText(mapName)) {
            addWindowLog("地图校准未启动：地图名为空");
            renderLogList();
            return false;
        }
        botProperties.setDebugMapCalibratorMapName(mapName);
        if (mapCalibratorMapNameField != null) {
            mapCalibratorMapNameField.setText(mapName);
        }
        return true;
    }

    private void addMapCalibratorUiHintIfNeeded(List<TaskType> queue) {
        if (queue == null || !queue.contains(TaskType.DEBUG_MAP_CALIBRATOR)) {
            return;
        }
        String mapName = botProperties.getDebugMapCalibratorMapName();
        addWindowLog("地图校准准备：" + nullToDash(mapName)
                + "；启动后先等5秒给你打开地图/放到点A，然后点A停3秒，听到提示后移到点B再停3秒。详细OCR日志看 logs/dhxy-console.log");
    }

    private String supplyText(boolean enabled, int threshold) {
        return (enabled ? "开" : "关") + "/" + normalizeSupplyThreshold(threshold) + "%";
    }

    private int normalizeSupplyThreshold(Integer threshold) {
        if (threshold == null || threshold <= 40) {
            return 30;
        }
        if (threshold <= 60) {
            return 50;
        }
        return 70;
    }

    private int normalizeRunCount(Integer count) {
        if (count == null || count < 1) {
            return 1;
        }
        return Math.min(count, 100);
    }

    private int normalizeWuhuanRunCount(Integer count) {
        return count != null && count >= 2 ? 2 : 1;
    }

    private int readRunCountField(TextField field) {
        int value = normalizeRunCount(parseInteger(field == null ? null : field.getText()));
        if (field != null) {
            field.setText(String.valueOf(value));
        }
        return value;
    }

    /*
     * Keep the compact task-tile count badges and the Settings page fields backed by the same
     * BotProperties values. XiuluoTask reads botProperties.getXiuluoMaxRuns() at execution time, so
     * a tile-only edit must be copied into BotProperties before the window task queue is submitted.
     */
    private void syncTaskCountSummariesFromProperties() {
        taskCountSummaries.put(TaskType.XIULUO, formatTaskCountSummary(botProperties.getXiuluoMaxRuns(), "次"));
        taskCountSummaries.put(TaskType.WUHuan, formatTaskCountSummary(botProperties.getWuhuanMaxRuns(), "轮"));
        refreshTaskTiles();
    }

    /*
     * Startup also runs this sync because the tile editor is separate from the Settings page. Without
     * this bridge, changing the main-screen Xiuluo count only changes the badge text and the task
     * still starts with the old/default maxRuns value.
     */
    private void syncTaskRunCountsFromTileEditor(List<TaskType> queue) {
        if (queue == null || queue.isEmpty()) {
            return;
        }
        queue.stream()
                .filter(taskType -> taskType != null && taskType != TaskType.UNKNOWN)
                .distinct()
                .forEach(taskType -> {
                    TaskCountDisplay display = parseTaskCountDisplay(taskCountSummaries.get(taskType));
                    syncTaskRunCountToProperties(taskType, display.value());
                });
        refreshTaskTiles();
    }

    /*
     * Copy one task count into the runtime configuration object and mirror the value back into the
     * Settings controls when they exist. Unsupported task types are deliberately ignored because
     * their badges may represent time or manual/debug modes rather than a game-task run count.
     */
    private void syncTaskRunCountToProperties(TaskType taskType, int value) {
        int normalized = normalizeRunCount(value);
        switch (taskType) {
            case XIULUO -> {
                botProperties.setXiuluoMaxRuns(normalized);
                if (xiuluoRunCountField != null) {
                    xiuluoRunCountField.setText(String.valueOf(normalized));
                }
                taskCountSummaries.put(TaskType.XIULUO, formatTaskCountSummary(normalized, "次"));
            }
            case WUHuan -> {
                int wuhuanRuns = normalizeWuhuanRunCount(normalized);
                botProperties.setWuhuanMaxRuns(wuhuanRuns);
                if (wuhuanRunCountComboBox != null) {
                    wuhuanRunCountComboBox.setValue(wuhuanRuns);
                }
                taskCountSummaries.put(TaskType.WUHuan, formatTaskCountSummary(wuhuanRuns, "轮"));
            }
            case AUTO_BATTLE, DEBUG_COORDINATE, DEBUG_MAP_CALIBRATOR, DEBUG_TEAM_ROLE,
                    DEBUG_XIULUO_STORY_OBJECTIVE, DEBUG_XIULUO_TASK_PANEL_OBJECTIVE,
                    DEBUG_XIULUO_MOCK_OBJECTIVE, UNKNOWN -> {
                // These badges are labels/durations/debug hints, not max-run task limits.
            }
        }
    }

    private String formatTaskCountSummary(int value, String unit) {
        return normalizeRunCount(value) + (unit == null || unit.isBlank() ? "次" : unit);
    }

    private Integer parseInteger(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int normalizeSummonSkillIntervalMinutes(Long intervalMs) {
        if (intervalMs == null || intervalMs <= 0) {
            return 20;
        }
        return normalizeSummonSkillIntervalMinutes((int) Math.round(intervalMs / 60_000.0));
    }

    private int normalizeSummonSkillIntervalMinutes(Integer minutes) {
        if (minutes == null || minutes <= 4) {
            return 3;
        }
        if (minutes <= 7) {
            return 5;
        }
        if (minutes <= 12) {
            return 10;
        }
        if (minutes <= 17) {
            return 15;
        }
        if (minutes <= 25) {
            return 20;
        }
        if (minutes <= 45) {
            return 30;
        }
        return 60;
    }

    private void configureVisualStyles() {
        addStyleClass(clearButton, "secondary-button");
        addStyleClass(clearWindowSelectionButton, "secondary-button");
        addStyleClass(registerWindowButton, "primary-button");
        addStyleClass(scanGameWindowsButton, "primary-button");
        addStyleClass(startIndependentWindowsButton, "primary-button");
        addStyleClass(startCurrentTaskButton, "primary-button");
        addStyleClass(startCurrentTaskButton, "start-action");
        addStyleClass(startWindowSelectedTaskButton, "primary-button");
        addStyleClass(startTeamRoleDebugButton, "primary-button");
        addStyleClass(startQueueButton, "primary-button");
        addStyleClass(pauseSelectedWindowsButton, "secondary-button");
        addStyleClass(resumeSelectedWindowsButton, "secondary-button");
        addStyleClass(pauseAllWindowsButton, "secondary-button");
        addStyleClass(resumeAllWindowsButton, "secondary-button");
        addStyleClass(stopSelectedWindowsButton, "danger-button");
        addStyleClass(stopSelectedWindowsButton, "bulk-danger-button");
        addStyleClass(stopAllWindowsButton, "danger-button");
        addStyleClass(unregisterSelectedWindowsButton, "danger-button");
        addStyleClass(unregisterAllWindowsButton, "danger-button");
        addStyleClass(applySupplyConfigButton, "primary-button");
        addStyleClass(applyGameConfigButton, "primary-button");
        addStyleClass(applySelectedTaskButton, "secondary-button");
        addStyleClass(refreshWindowButton, "secondary-button");
        addStyleClass(windowSelectionMenuButton, "secondary-button");
        addStyleClass(windowManageMenuButton, "secondary-button");
        addStyleClass(runControlMenuButton, "secondary-button");
        addStyleClass(registerTeamButton, "secondary-button");
        addStyleClass(presetDebugCoordinateQueueButton, "secondary-button");
        addStyleClass(setDebugCoordinateTaskButton, "secondary-button");
        addStyleClass(saveMapLabelSampleButton, "secondary-button");
        addStyleClass(testMapLabelSampleButton, "secondary-button");
        addStyleClass(recordCameraLeftButton, "secondary-button");
        addStyleClass(recordCameraRightButton, "secondary-button");
        addStyleClass(recordCameraTopButton, "secondary-button");
        addStyleClass(recordCameraBottomButton, "secondary-button");
        addStyleClass(recordCameraCenterButton, "secondary-button");
        addStyleClass(testProjectedPlayerPointButton, "secondary-button");
        addStyleClass(recordPlayerPointCorrectionButton, "secondary-button");
        addStyleClass(testCorrectedPlayerPointButton, "secondary-button");
        addStyleClass(undoPlayerPointCorrectionButton, "secondary-button");
        addStyleClass(darkModeCheckBox, "theme-toggle");
        addStyleClass(windowSystemLabel, "status-text");
        addStyleClass(windowActionHintLabel, "hint-text");
        addStyleClass(windowMetricLabel, "metric-value");
        addStyleClass(runningMetricLabel, "metric-value");
        addStyleClass(problemMetricLabel, "metric-value");
        addStyleClass(queueSummaryLabel, "queue-summary");
        addStyleClass(selectedWindowCountLabel, "task-order-text");
        addStyleClass(windowTable, "window-table");
    }

    private Parent buildTopBar() {
        Label title = new Label("DHXY Robot 控制台");
        title.getStyleClass().add("app-title");
        title.setMinWidth(220);
        title.setPrefWidth(220);
        Label emergencyStopLabel = new Label("紧急停止：Ctrl+Shift+F12");
        emergencyStopLabel.getStyleClass().add("emergency-label");
        darkModeCheckBox.setOnAction(event -> applyThemeMode());

        clearButton.setOnAction(event -> {
            clearWindowLogs();
            refreshDashboard();
        });

        HBox box = new HBox(10, title, clearButton, darkModeCheckBox, emergencyStopLabel);
        box.setMinHeight(44);
        box.setPrefHeight(44);
        box.setMaxHeight(44);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("top-bar");
        return box;
    }

    private void applyThemeMode() {
        if (rootPane == null) {
            return;
        }
        rootPane.getStyleClass().remove("theme-dark");
        if (darkModeCheckBox != null && darkModeCheckBox.isSelected()) {
            rootPane.getStyleClass().add("theme-dark");
        }
    }

    private Parent buildMainShell() {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");

        StackPane content = new StackPane();
        content.getStyleClass().add("side-content");

        ToggleGroup group = new ToggleGroup();
        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("side-nav");

        ToggleButton mainButton = buildSideNavButton("主控", group, buildWindowPanel(), content);
        sidebar.getChildren().addAll(
                mainButton,
                buildSideNavButton("设置", group, buildSettingsPanel(), content),
                buildSideNavButton("验证", group, buildAuthenticationPanel(), content),
                buildSideNavButton("调试", group, buildDiagnosticsPanel(), content),
                buildSideNavButton("日志", group, buildLogPanel(), content),
                buildSideNavButton("说明", group, buildAboutPanel(), content)
        );

        mainButton.setSelected(true);
        content.getChildren().setAll((Parent) mainButton.getUserData());
        shell.setLeft(sidebar);
        shell.setCenter(content);
        return shell;
    }

    private ToggleButton buildSideNavButton(String title, ToggleGroup group, Parent content, StackPane contentPane) {
        ToggleButton button = new ToggleButton(title);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setToggleGroup(group);
        button.setUserData(content);
        button.getStyleClass().add("side-nav-button");
        button.setOnAction(event -> {
            if (!button.isSelected()) {
                button.setSelected(true);
            }
            contentPane.getChildren().setAll(content);
        });
        return button;
    }

    private Parent buildWindowPanel() {
        buildWindowTableColumns();

        registerWindowButton.setOnAction(event -> registerOrRefreshWindowFromUi());
        registerTeamButton.setOnAction(event -> registerTeamFromUi());
        scanGameWindowsButton.setOnAction(event -> runWindowCommandInBackground(() ->
                gameWindowRegistrationService.registerDetectedGameWindows(windowTaskTypeComboBox.getValue())));
        startIndependentWindowsButton.setOnAction(event -> runWindowCommandInBackground(() ->
                gameWindowRegistrationService.scanRegisterAndStartIndependentWindows(windowTaskTypeComboBox.getValue())));
        applySelectedTaskButton.setOnAction(event -> applySelectedTaskToSelectedWindows());
        addCurrentTaskToQueueButton.setOnAction(event -> addTaskToQueue(windowTaskTypeComboBox.getValue()));
        addQueueTaskButton.setOnAction(event -> addSelectedTaskToQueue());
        removeQueueTaskButton.setOnAction(event -> removeSelectedQueueTask());
        moveQueueTaskUpButton.setOnAction(event -> moveSelectedQueueTask(-1));
        moveQueueTaskDownButton.setOnAction(event -> moveSelectedQueueTask(1));
        clearQueueButton.setOnAction(event -> clearPendingTaskQueue());
        startQueueButton.setOnAction(event -> startPendingTaskQueue());
        presetCurrentTaskQueueButton.setOnAction(event -> setPendingTaskQueue(windowTaskTypeComboBox.getValue()));
        presetFiveRingQueueButton.setOnAction(event -> setPendingTaskQueue(TaskType.WUHuan));
        presetAutoBattleQueueButton.setOnAction(event -> setPendingTaskQueue(TaskType.AUTO_BATTLE));
        presetFiveRingAutoBattleQueueButton.setOnAction(event -> setPendingTaskQueue(TaskType.WUHuan, TaskType.AUTO_BATTLE));
        selectAllWindowsButton.setOnAction(event -> selectAllWindows());
        selectRunningWindowsButton.setOnAction(event -> selectWindowsByState(WindowTaskSnapshot::isRunning));
        selectIdleWindowsButton.setOnAction(event -> selectWindowsByState(snapshot -> !snapshot.isBusy()));
        selectProblemWindowsButton.setOnAction(event -> selectWindowsByState(this::isProblemWindow));
        selectBoundWindowsButton.setOnAction(event -> selectWindowsByState(WindowTaskSnapshot::hasNativeBinding));
        selectUnboundWindowsButton.setOnAction(event -> selectWindowsByState(snapshot -> !snapshot.hasNativeBinding()));
        clearWindowSelectionButton.setOnAction(event -> clearWindowSelection());
        selectAllWindowsMenuItem.setOnAction(event -> selectAllWindows());
        selectRunningWindowsMenuItem.setOnAction(event -> selectWindowsByState(WindowTaskSnapshot::isRunning));
        selectIdleWindowsMenuItem.setOnAction(event -> selectWindowsByState(snapshot -> !snapshot.isBusy()));
        selectProblemWindowsMenuItem.setOnAction(event -> selectWindowsByState(this::isProblemWindow));
        selectBoundWindowsMenuItem.setOnAction(event -> selectWindowsByState(WindowTaskSnapshot::hasNativeBinding));
        selectUnboundWindowsMenuItem.setOnAction(event -> selectWindowsByState(snapshot -> !snapshot.hasNativeBinding()));
        clearWindowSelectionMenuItem.setOnAction(event -> clearWindowSelection());
        windowFilterComboBox.setOnAction(event -> refreshWindowPanel());
        windowSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshWindowPanel());
        startCurrentTaskButton.setText("启动");
        startCurrentTaskButton.setOnAction(event -> {
            TaskType selectedTaskType = windowTaskTypeComboBox == null ? null : windowTaskTypeComboBox.getValue();
            log.info("UI start button clicked: selectedTask={} pendingQueue={} disabled={}",
                    selectedTaskType, pendingTaskQueue, startCurrentTaskButton.isDisabled());
            startMainSelectedTasks();
        });
        startWindowSelectedTaskButton.setOnAction(event -> {
            if (!syncDebugTaskConfigFromUi(List.of(windowTaskTypeComboBox == null ? null : windowTaskTypeComboBox.getValue()))) {
                return;
            }
            List<String> windowIds = getSelectedWindowIds();
            warnUnavailableSelectedWindows("启动已选任务");
            runWindowCommandInBackground(() ->
                    windowTaskControlService.start(WindowTaskStartRequest.selectedTask(windowIds)));
        });
        startTeamRoleDebugButton.setOnAction(event -> {
            syncDebugTaskConfigFromUi(List.of(TaskType.DEBUG_TEAM_ROLE));
            List<String> windowIds = getSelectedWindowIds();
            warnUnavailableSelectedWindows("队伍识别测试");
            runWindowCommandInBackground(() ->
                    windowTaskControlService.start(WindowTaskStartRequest.sameTask(windowIds, TaskType.DEBUG_TEAM_ROLE)));
        });
        pauseSelectedWindowsButton.setOnAction(event -> togglePauseResumeSelectedWindows());
        resumeSelectedWindowsButton.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() ->
                    windowTaskControlService.resumeWindows(windowIds));
        });
        pauseAllWindowsButton.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::pauseAll));
        resumeAllWindowsButton.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::resumeAll));
        pauseSelectedWindowsMenuItem.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() ->
                    windowTaskControlService.pauseWindows(windowIds));
        });
        resumeSelectedWindowsMenuItem.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() ->
                    windowTaskControlService.resumeWindows(windowIds));
        });
        pauseAllWindowsMenuItem.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::pauseAll));
        resumeAllWindowsMenuItem.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::resumeAll));
        stopSelectedWindowsButton.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() ->
                    windowTaskControlService.stopWindows(windowIds));
        });
        stopAllWindowsButton.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::stopAll));
        stopSelectedWindowsMenuItem.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() ->
                    windowTaskControlService.stopWindows(windowIds));
        });
        stopAllWindowsMenuItem.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::stopAll));
        unregisterSelectedWindowsButton.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() ->
                    windowTaskControlService.unregisterWindows(windowIds));
        });
        unregisterAllWindowsButton.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::unregisterAll));
        unregisterSelectedWindowsMenuItem.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() ->
                    windowTaskControlService.unregisterWindows(windowIds));
        });
        unregisterAllWindowsMenuItem.setOnAction(event -> runWindowCommandInBackground(windowTaskControlService::unregisterAll));
        refreshWindowButton.setOnAction(event -> scanAndRefreshGameWindowsFromMain());
        applySupplyConfigButton.setOnAction(event -> applySupplyConfigFromUi());
        applyGameConfigButton.setOnAction(event -> applyGameConfigFromUi());
        playerNameOcrDebugButton.setOnAction(event -> runPlayerNameOcrDebug());

        HBox leftTools = new HBox(8, refreshWindowButton, windowFilterComboBox, windowSearchField);
        leftTools.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(windowSearchField, Priority.NEVER);

        selectAllWindowsButton.setText("全选");
        clearWindowSelectionButton.setText("取消选择");
        pauseSelectedWindowsButton.setText("暂停");
        stopSelectedWindowsButton.setText("停止");
        refreshWindowButton.setMinWidth(58);
        windowFilterComboBox.setMinWidth(92);
        windowSearchField.setPrefWidth(160);
        windowSearchField.setMinWidth(132);
        pauseSelectedWindowsButton.setMinWidth(62);
        stopSelectedWindowsButton.setMinWidth(86);
        clearWindowSelectionButton.setMinWidth(74);
        selectAllWindowsButton.setMinWidth(62);
        playerNameOcrDebugButton.setMinWidth(96);
        startCurrentTaskButton.setMinWidth(86);
        HBox rightTools = new HBox(6,
                pauseSelectedWindowsButton,
                stopSelectedWindowsButton,
                clearWindowSelectionButton,
                selectAllWindowsButton,
                playerNameOcrDebugButton,
                startCurrentTaskButton);
        rightTools.setAlignment(Pos.CENTER_RIGHT);
        HBox toolbar = new HBox(12, leftTools, rightTools);
        toolbar.getStyleClass().add("window-toolbar");
        HBox.setHgrow(leftTools, Priority.ALWAYS);

        Parent taskSelectorPanel = buildTaskSelectorPanel();
        Parent overviewPanel = buildSummaryMetricsPanel();
        selectedWindowDetailPanel = buildSelectedWindowDetailPanel();

        windowTable.setMinHeight(150);
        windowTable.setPrefHeight(260);

        VBox topControls = new VBox(8, overviewPanel);
        topControls.getStyleClass().add("main-top-controls");

        BorderPane workbench = new BorderPane();
        workbench.getStyleClass().add("main-workbench");
        workbench.setCenter(buildWindowWorkbenchPanel(toolbar, selectedWindowDetailPanel));

        VBox wrapper = new VBox(8,
                topControls,
                workbench,
                taskSelectorPanel);
        VBox.setVgrow(workbench, Priority.ALWAYS);
        wrapper.setFillWidth(true);
        wrapper.getStyleClass().add("tab-content");
        refreshSelectedWindowDetailVisibility();
        return wrapper;
    }

    private Parent buildWindowWorkbenchPanel(Parent toolbar, Parent detailPanel) {
        Label titleLabel = new Label("窗口与任务");
        titleLabel.getStyleClass().add("section-title");
        HBox titleRow = new HBox(10, titleLabel, selectedWindowCountLabel);
        titleRow.getStyleClass().add("panel-title-row");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        VBox panel = new VBox(8, titleRow, toolbar, windowActionHintLabel, windowTable);
        panel.getStyleClass().addAll("section-card", "window-work-panel");
        VBox.setVgrow(windowTable, Priority.ALWAYS);

        BorderPane split = new BorderPane();
        split.setCenter(panel);
        split.setRight(detailPanel);
        BorderPane.setMargin(detailPanel, new Insets(0, 0, 0, 8));
        return split;
    }

    private Parent buildSettingsPanel() {
        VBox wrapper = new VBox(8,
                buildTaskRunConfigPanel(),
                buildSummonSkillConfigPanel(),
                buildSupplyConfigPanel());
        wrapper.setFillWidth(true);
        wrapper.getStyleClass().add("tab-content");
        return wrapper;
    }

    private Parent buildTaskRunConfigPanel() {
        FlowPane firstRow = buildControlRow(
                new Label("修罗次数"), xiuluoRunCountField,
                new Label("五环次数"), wuhuanRunCountComboBox,
                new Label("五倍次数"), fivefoldRunCountField);
        FlowPane secondRow = buildControlRow(
                new Label("天庭次数"), tiantingRunCountField,
                new Label("抓鬼次数"), zhuaguiRunCountField,
                applyGameConfigButton);
        return buildSection("任务次数", firstRow, secondRow);
    }

    private Parent buildSummonSkillConfigPanel() {
        FlowPane summonRow = buildControlRow(
                taskStartupPreparationEnabledCheckBox,
                summonSkillCleanEnabledCheckBox,
                new Label("三技能间隔"),
                summonSkillIntervalMinutesComboBox,
                new Label("分钟"));
        return buildSection("召唤兽技能", summonRow);
    }

    private Parent buildAuthenticationPanel() {
        TextField licenseCodeField = new TextField();
        licenseCodeField.setPromptText("请输入 DHXY 授权码");
        licenseCodeField.setPrefWidth(260);

        Label statusLabel = new Label("未验证");
        Label expireLabel = new Label("暂无到期时间");
        Label actionLabel = new Label("无需操作");
        Label messageLabel = new Label("请输入 DHXY 授权码后点击验证。");
        messageLabel.setWrapText(true);

        Button verifyButton = new Button("验证");
        Button refreshButton = new Button("刷新");
        Button renewButton = new Button("续约30天");
        addStyleClass(verifyButton, "primary-button");
        addStyleClass(refreshButton, "secondary-button");
        addStyleClass(renewButton, "secondary-button");

        Consumer<LicenseAuthResult> renderResult = result -> {
            statusLabel.setText(result.success() ? "已授权" : "未授权");
            expireLabel.setText(result.expireText());
            actionLabel.setText(result.actionDisplayName());
            messageLabel.setText(result.message());
            addWindowLog("授权：" + result.code() + " " + result.message());
            renderLogList();
        };

        verifyButton.setOnAction(event -> runLicenseAction(
                "验证授权",
                () -> licenseAuthService.verify(licenseCodeField.getText()),
                renderResult,
                verifyButton,
                refreshButton,
                renewButton
        ));
        refreshButton.setOnAction(event -> runLicenseAction(
                "刷新授权",
                () -> licenseAuthService.refreshStatus(licenseCodeField.getText()),
                renderResult,
                verifyButton,
                refreshButton,
                renewButton
        ));
        renewButton.setOnAction(event -> runLicenseAction(
                "续约授权",
                () -> licenseAuthService.renew30Days(licenseCodeField.getText()),
                renderResult,
                verifyButton,
                refreshButton,
                renewButton
        ));

        FlowPane inputRow = buildControlRow(
                new Label("DHXY 授权码"),
                licenseCodeField,
                verifyButton,
                refreshButton,
                renewButton);
        FlowPane statusRow = buildControlRow(
                new Label("状态"),
                statusLabel,
                new Label("到期"),
                expireLabel,
                new Label("处理"),
                actionLabel);

        VBox wrapper = new VBox(8,
                buildSection("授权 / License", inputRow, statusRow, messageLabel),
                buildSection("验证码 / Authentication",
                        new Label("验证码处理入口后续放在这里。授权码验证和续约已经接入 license-worker。")));
        wrapper.getStyleClass().add("tab-content");
        return wrapper;
    }

    private void runLicenseAction(
            String actionName,
            Supplier<LicenseAuthResult> action,
            Consumer<LicenseAuthResult> onResult,
            Button... buttons
    ) {
        for (Button button : buttons) {
            button.setDisable(true);
        }

        Thread worker = new Thread(() -> {
            LicenseAuthResult result = action.get();
            javafx.application.Platform.runLater(() -> {
                onResult.accept(result);
                for (Button button : buttons) {
                    button.setDisable(false);
                }
            });
        }, "dhxy-license-ui-worker");
        worker.setDaemon(true);
        worker.start();

        addWindowLog("授权：" + actionName + "请求已发送");
        renderLogList();
    }

    private Parent buildDiagnosticsPanel() {
        presetDebugCoordinateQueueButton.setOnAction(event -> addTaskToQueue(TaskType.DEBUG_COORDINATE));
        setDebugCoordinateTaskButton.setOnAction(event -> {
            windowTaskTypeComboBox.setValue(TaskType.DEBUG_COORDINATE);
            queueTaskTypeComboBox.setValue(TaskType.DEBUG_COORDINATE);
            addWindowLog("调试：当前任务选择已切到坐标调试");
            renderLogList();
        });
        windowCaptureExperimentButton.setOnAction(event -> runWindowCaptureExperiment());
        backgroundAltQExperimentButton.setOnAction(event -> runWindowMessageInputExperiment("后台按键 Alt+Q",
                windowMessageInputExperimentService::postAltQ));
        backgroundAlt1ExperimentButton.setOnAction(event -> runWindowMessageInputExperiment("后台按键 Alt+1",
                windowMessageInputExperimentService::postAlt1));
        backgroundCenterClickExperimentButton.setOnAction(event -> runWindowMessageInputExperiment("后台鼠标中心左键",
                windowMessageInputExperimentService::clickClientCenter));
        backgroundCenterRightClickExperimentButton.setOnAction(event -> runWindowMessageInputExperiment("后台鼠标中心右键",
                windowMessageInputExperimentService::rightClickClientCenter));
        backgroundChildRightClickExperimentButton.setOnAction(event -> runWindowMessageInputExperiment("子窗口中心右键",
                windowMessageInputExperimentService::rightClickLargestChildCenter));
        interactionMetricsDashboardButton.setOnAction(event -> openInteractionMetricsDashboard());
        saveMapLabelSampleButton.setOnAction(event -> runMapSurveyCommand("保存地图名样本",
                (snapshot, mapName) -> mapSurveyService.saveMapLabelSample(snapshot, mapName)));
        testMapLabelSampleButton.setOnAction(event -> runMapSurveyCommand("测试地图名", false,
                (snapshot, mapName) -> mapSurveyService.recognizeCurrentMapLabel(snapshot)));
        recordCameraLeftButton.setOnAction(event -> runMapSurveyCommand("记录左边界",
                (snapshot, mapName) -> mapSurveyService.recordCameraBoundary(snapshot, mapName, MapSurveyService.CameraBoundaryDirection.LEFT)));
        recordCameraRightButton.setOnAction(event -> runMapSurveyCommand("记录右边界",
                (snapshot, mapName) -> mapSurveyService.recordCameraBoundary(snapshot, mapName, MapSurveyService.CameraBoundaryDirection.RIGHT)));
        recordCameraTopButton.setOnAction(event -> runMapSurveyCommand("记录上边界",
                (snapshot, mapName) -> mapSurveyService.recordCameraBoundary(snapshot, mapName, MapSurveyService.CameraBoundaryDirection.TOP)));
        recordCameraBottomButton.setOnAction(event -> runMapSurveyCommand("记录下边界",
                (snapshot, mapName) -> mapSurveyService.recordCameraBoundary(snapshot, mapName, MapSurveyService.CameraBoundaryDirection.BOTTOM)));
        recordCameraCenterButton.setOnAction(event -> runMapSurveyCommand("记录中心点",
                (snapshot, mapName) -> mapSurveyService.recordCenterAnchor(snapshot, mapName)));
        testProjectedPlayerPointButton.setOnAction(event -> runMapSurveyCommand("测试角色屏幕点", false,
                (snapshot, mapName) -> mapSurveyService.moveMouseToProjectedPlayerPointByCurrentMap(snapshot)));
        recordPlayerPointCorrectionButton.setOnAction(event -> runMapSurveyCommand("记录角色修正点", false,
                (snapshot, mapName) -> mapSurveyService.recordPlayerPointCorrectionByCurrentMap(snapshot)));
        testCorrectedPlayerPointButton.setOnAction(event -> runMapSurveyCommand("测试修正角色点", false,
                (snapshot, mapName) -> mapSurveyService.moveMouseToCorrectedPlayerPointByCurrentMap(snapshot)));
        undoPlayerPointCorrectionButton.setOnAction(event -> runMapSurveyCommand("撤销上次地图记录", false,
                (snapshot, mapName) -> mapSurveyService.undoLastMapSurveyRecordByCurrentMap(snapshot)));

        FlowPane taskDebugRow = buildControlRow(
                setDebugCoordinateTaskButton,
                presetDebugCoordinateQueueButton,
                new Label("坐标调试走现有任务/队列启动链路"));
        FlowPane screenshotDebugRow = buildControlRow(
                windowCaptureExperimentButton,
                new Label("对选中窗口保存后台截图样本"));
        FlowPane inputDebugRow = buildControlRow(
                backgroundAltQExperimentButton,
                backgroundAlt1ExperimentButton,
                backgroundCenterClickExperimentButton,
                backgroundCenterRightClickExperimentButton,
                backgroundChildRightClickExperimentButton,
                interactionMetricsDashboardButton,
                new Label("发送后台 WM_* 消息，并保存发送前/后 HWND 截图"));
        VBox wrapper = new VBox(8,
                buildSection("任务调试", taskDebugRow),
                buildSection("截图实验", screenshotDebugRow),
                buildSection("后台输入实验", inputDebugRow),
                buildSection("日志文件",
                        new Label("主日志：logs/dhxy-console.log"),
                        new Label("坐标/窗口诊断：logs/tracker-coordinate.log")),
                buildSection("后续入口占位",
                        new Label("NPC 首点调试、截图/OCR、模板匹配、窗口截图检查等入口后续放在这里。")));
        wrapper.setFillWidth(true);
        wrapper.getStyleClass().add("tab-content");
        return wrapper;
    }

    private Parent buildAboutPanel() {
        VBox wrapper = new VBox(8,
                buildSection("说明",
                        new Label("主控：日常窗口选择、任务启动、任务队列和窗口详情。"),
                        new Label("设置：游戏内任务次数、召唤兽技能和补给阈值。"),
                        new Label("验证：后续放验证码和人工验证相关入口。"),
                        new Label("调试：坐标调试、日志路径、截图/OCR 等诊断入口。"),
                        new Label("日志：窗口命令执行摘要和 UI 操作日志。"),
                        new Label("窗口详情从表格右侧浮出；主页面不再用整页滑动承载所有控件。")));
        wrapper.getStyleClass().add("tab-content");
        return wrapper;
    }

    private Parent buildSelectedWindowDetailPanel() {
        Label titleLabel = new Label("角色详情");
        titleLabel.getStyleClass().add("section-title");
        Button collapseButton = new Button("收起");
        collapseButton.getStyleClass().add("secondary-button");
        collapseButton.setOnAction(event -> {
            selectedWindowDetailExpanded = false;
            refreshSelectedWindowDetailVisibility();
        });
        HBox titleRow = new HBox(10, titleLabel, collapseButton);
        titleRow.getStyleClass().add("panel-title-row");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        VBox panel = new VBox(4, titleRow, selectedWindowDetailBox);
        panel.setMaxWidth(280);
        panel.getStyleClass().addAll("section-card", "detail-floating-panel");
        return panel;
    }

    private Parent buildSummaryMetricsPanel() {
        HBox metrics = new HBox(10,
                buildMetricCard("窗口", windowMetricLabel),
                buildMetricCard("运行中", runningMetricLabel),
                buildMetricCard("异常", problemMetricLabel));
        metrics.getStyleClass().add("summary-metrics");
        return metrics;
    }

    private Parent buildMetricCard(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        VBox card = new VBox(4, titleLabel, valueLabel);
        card.getStyleClass().add("metric-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private FlowPane buildControlRow(javafx.scene.Node... nodes) {
        FlowPane row = new FlowPane(8, 6);
        row.getChildren().addAll(nodes);
        row.getStyleClass().add("control-row");
        return row;
    }

    private Parent buildSection(String title, Parent... rows) {
        VBox box = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        box.getChildren().add(titleLabel);
        box.getChildren().addAll(rows);
        box.getStyleClass().add("section-card");
        return box;
    }

    private void buildWindowTableColumns() {
        windowTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(WindowTaskSnapshot snapshot, boolean empty) {
                super.updateItem(snapshot, empty);
                getStyleClass().removeAll("row-error", "row-stopped", "row-running", "row-accepting");
                if (empty || snapshot == null) {
                } else if (snapshot.getStatus() == WindowRuntimeStatus.ERROR) {
                    getStyleClass().add("row-error");
                } else if (snapshot.getStatus() == WindowRuntimeStatus.STOPPED) {
                    getStyleClass().add("row-stopped");
                } else if (snapshot.isRunning()) {
                    getStyleClass().add("row-running");
                } else if (snapshot.isAcceptingTaskQueue()) {
                    getStyleClass().add("row-accepting");
                }
            }
        });

        TableColumn<WindowTaskSnapshot, WindowTaskSnapshot> selectionCol = new TableColumn<>("");
        selectionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        selectionCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setFocusTraversable(false);
                checkBox.getStyleClass().add("window-select-check");
                checkBox.setMinSize(12, 12);
                checkBox.setPrefSize(12, 12);
                checkBox.setMaxSize(12, 12);
                checkBox.setOnAction(event -> {
                    WindowTaskSnapshot snapshot = getItem();
                    if (snapshot == null || getIndex() < 0) {
                        return;
                    }
                    if (checkBox.isSelected()) {
                        windowTable.getSelectionModel().select(getIndex());
                    } else {
                        windowTable.getSelectionModel().clearSelection(getIndex());
                    }
                    windowTable.refresh();
                    refreshSelectionDependentUi();
                });
            }

            @Override
            protected void updateItem(WindowTaskSnapshot snapshot, boolean empty) {
                super.updateItem(snapshot, empty);
                setText(null);
                if (empty || snapshot == null) {
                    setGraphic(null);
                    return;
                }
                checkBox.setSelected(windowTable.getSelectionModel().getSelectedItems().contains(snapshot));
                setGraphic(checkBox);
            }
        });
        selectionCol.setPrefWidth(28);
        selectionCol.setMinWidth(28);
        selectionCol.setMaxWidth(30);
        selectionCol.setSortable(false);
        selectionCol.setResizable(false);

        TableColumn<WindowTaskSnapshot, String> roleNameCol = new TableColumn<>("角色名");
        roleNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(parseWindowIdentity(cell.getValue()).roleName()));
        roleNameCol.setPrefWidth(106);

        TableColumn<WindowTaskSnapshot, WindowTaskSnapshot> baseCol = new TableColumn<>("Base");
        baseCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        baseCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(WindowTaskSnapshot snapshot, boolean empty) {
                super.updateItem(snapshot, empty);
                if (empty || snapshot == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(formatWindowBase(snapshot));
                setTooltip(new Tooltip(formatWindowBaseTooltip(snapshot)));
            }
        });
        baseCol.setPrefWidth(78);

        TableColumn<WindowTaskSnapshot, String> serverCol = new TableColumn<>("服务器");
        serverCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(parseWindowIdentity(cell.getValue()).serverName()));
        serverCol.setPrefWidth(84);

        TableColumn<WindowTaskSnapshot, String> playerIdCol = new TableColumn<>("ID");
        playerIdCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(parseWindowIdentity(cell.getValue()).playerId()));
        playerIdCol.setPrefWidth(84);

        TableColumn<WindowTaskSnapshot, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatusDisplayName()));
        statusCol.setPrefWidth(74);

        TableColumn<WindowTaskSnapshot, String> runningTaskCol = new TableColumn<>("运行任务");
        runningTaskCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRunningTaskDisplayName()));
        runningTaskCol.setPrefWidth(78);

        TableColumn<WindowTaskSnapshot, String> queueProgressCol = new TableColumn<>("进度");
        queueProgressCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRunningQueueProgressText()));
        queueProgressCol.setPrefWidth(52);

        TableColumn<WindowTaskSnapshot, WindowTaskSnapshot> actionsCol = new TableColumn<>("操作");
        actionsCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(WindowTaskSnapshot snapshot, boolean empty) {
                super.updateItem(snapshot, empty);
                if (empty || snapshot == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(buildRowActions(snapshot));
            }
        });
        actionsCol.setPrefWidth(116);

        windowTable.getColumns().setAll(List.of(selectionCol, roleNameCol, baseCol, serverCol, playerIdCol,
                statusCol, runningTaskCol, queueProgressCol, actionsCol));
        windowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private String formatWindowBase(WindowTaskSnapshot snapshot) {
        if (snapshot == null || snapshot.getNativeBinding() == null || !snapshot.getNativeBinding().hasGeometry()) {
            return "-";
        }
        double scale = coordinateHelper.getScaleRatio();
        int baseX = (int) (snapshot.getNativeBinding().getX() / scale);
        int baseY = (int) (snapshot.getNativeBinding().getY() / scale);
        return baseX + "," + baseY;
    }

    private String formatWindowBaseTooltip(WindowTaskSnapshot snapshot) {
        if (snapshot == null || snapshot.getNativeBinding() == null || !snapshot.getNativeBinding().hasGeometry()) {
            return "Base: -";
        }
        double scale = coordinateHelper.getScaleRatio();
        int baseX = (int) (snapshot.getNativeBinding().getX() / scale);
        int baseY = (int) (snapshot.getNativeBinding().getY() / scale);
        return "tracker base=" + baseX + "," + baseY
                + "\nnative rect=" + snapshot.getNativeBinding().getGeometryText()
                + "\nscale=" + String.format("%.2f", scale)
                + "\nhwnd=" + snapshot.getNativeHandle();
    }

    private Parent buildRowActions(WindowTaskSnapshot snapshot) {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("row-actions");
        if (snapshot.getStatus() == WindowRuntimeStatus.PAUSED) {
            actions.getChildren().add(rowActionButton("▶", "继续", "row-icon-button",
                    () -> runWindowCommandInBackground(() -> windowTaskControlService.resumeWindows(List.of(snapshot.getWindowId())))));
            actions.getChildren().add(rowActionButton("⏹", "停止", "row-stop-button",
                    () -> runWindowCommandInBackground(() -> windowTaskControlService.stopWindows(List.of(snapshot.getWindowId())))));
        } else if (snapshot.isRunning()) {
            actions.getChildren().add(rowActionButton("⏸", "暂停", "row-icon-button",
                    () -> runWindowCommandInBackground(() -> windowTaskControlService.pauseWindows(List.of(snapshot.getWindowId())))));
            actions.getChildren().add(rowActionButton("⏹", "停止", "row-stop-button",
                    () -> runWindowCommandInBackground(() -> windowTaskControlService.stopWindows(List.of(snapshot.getWindowId())))));
        } else if (isProblemWindow(snapshot)) {
            actions.getChildren().add(rowActionButton("↻", "重试", "row-icon-button",
                    () -> startWindows(List.of(snapshot.getWindowId()), "重试")));
            actions.getChildren().add(rowActionButton("⏹", "停止", "row-stop-button",
                    () -> runWindowCommandInBackground(() -> windowTaskControlService.stopWindows(List.of(snapshot.getWindowId())))));
        } else {
            actions.getChildren().add(rowActionButton("▶", "启动", "row-icon-button",
                    () -> startWindows(List.of(snapshot.getWindowId()), "启动")));
        }
        Button detailButton = new Button("详情");
        detailButton.getStyleClass().add("row-detail-button");
        detailButton.setOnAction(event -> {
            selectedWindowDetailExpanded = true;
            windowTable.getSelectionModel().clearSelection();
            windowTable.getSelectionModel().select(snapshot);
            refreshSelectionDependentUi();
        });
        actions.getChildren().add(detailButton);
        return actions;
    }

    private Button rowActionButton(String text, String tooltipText, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.setAccessibleText(tooltipText);
        button.getStyleClass().add(styleClass);
        button.setOnAction(event -> {
            if (action != null) {
                action.run();
            }
        });
        return button;
    }

    private Parent buildTaskSelectorPanel() {
        taskTileButtons.clear();
        taskSelectionSummaryLabel = new Label();
        taskSelectionSummaryLabel.getStyleClass().add("queue-summary");
        FlowPane taskTiles = new FlowPane(8, 8);
        taskTiles.getStyleClass().add("task-tile-grid");
        selectableTaskTypes().forEach(taskType -> taskTiles.getChildren().add(buildTaskTile(taskType)));
        taskCountEditorBar = buildTaskCountEditorBar();
        HBox mapCalibratorRow = new HBox(8, new Label("地图校准名"), mapCalibratorMapNameField);
        mapCalibratorRow.setAlignment(Pos.CENTER_LEFT);
        mapCalibratorRow.getStyleClass().add("task-selector-actions");
        FlowPane mapSurveyRow = buildControlRow(
                saveMapLabelSampleButton,
                testMapLabelSampleButton,
                recordCameraLeftButton,
                recordCameraRightButton,
                recordCameraTopButton,
                recordCameraBottomButton,
                recordCameraCenterButton,
                testProjectedPlayerPointButton,
                recordPlayerPointCorrectionButton,
                testCorrectedPlayerPointButton,
                undoPlayerPointCorrectionButton);

        Button clearTasksButton = new Button("清空任务选择");
        clearTasksButton.getStyleClass().add("secondary-button");
        clearTasksButton.setOnAction(event -> clearPendingTaskQueue());

        Button countShortcutButton = new Button("次数");
        countShortcutButton.getStyleClass().add("secondary-button");
        countShortcutButton.setOnAction(event -> openFirstTaskCountEditor());

        HBox titleActions = new HBox(8, taskSelectionSummaryLabel, countShortcutButton);
        titleActions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(taskSelectionSummaryLabel, Priority.ALWAYS);
        HBox titleRow = new HBox(8, new Label("任务选择"), titleActions);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().get(0).getStyleClass().add("section-title");
        HBox.setHgrow(titleActions, Priority.ALWAYS);
        titleRow.getStyleClass().add("panel-title-row");

        HBox actions = new HBox(8, clearTasksButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("task-selector-actions");

        VBox panel = new VBox(8, titleRow, taskTiles, taskCountEditorBar,
                mapCalibratorRow, mapSurveyRow, mapCalibratorHintLabel, actions);
        panel.getStyleClass().add("task-selector-panel");
        refreshTaskTiles();
        refreshTaskSelectorSummary();
        panel.getStyleClass().add("section-card");
        return panel;
    }

    private HBox buildTaskCountEditorBar() {
        taskCountEditorTitleLabel = new Label("次数");
        taskCountEditorTitleLabel.getStyleClass().add("task-count-editor-title");
        Button decreaseButton = new Button("-");
        decreaseButton.getStyleClass().add("count-step-button");
        configureCountStepButton(decreaseButton, -1);
        taskCountEditorField = new TextField("1");
        taskCountEditorField.getStyleClass().add("task-count-input");
        taskCountEditorField.setPrefWidth(48);
        Button increaseButton = new Button("+");
        increaseButton.getStyleClass().add("count-step-button");
        configureCountStepButton(increaseButton, 1);
        taskCountEditorUnitLabel = new Label("轮");
        taskCountEditorUnitLabel.getStyleClass().add("queue-summary");
        Button applyButton = new Button("应用");
        applyButton.getStyleClass().add("primary-button");
        applyButton.setOnAction(event -> applyTaskCountEditor());
        Button cancelButton = new Button("取消");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(event -> hideTaskCountEditor());

        HBox editor = new HBox(8, taskCountEditorTitleLabel, decreaseButton, taskCountEditorField,
                increaseButton, taskCountEditorUnitLabel, applyButton, cancelButton);
        editor.setAlignment(Pos.CENTER_LEFT);
        editor.getStyleClass().add("task-count-editor");
        editor.getStyleClass().add("task-count-editor-hidden");
        editor.setVisible(false);
        editor.setManaged(true);
        editor.setMinHeight(40);
        editor.setPrefHeight(40);
        return editor;
    }

    private void configureCountStepButton(Button button, int direction) {
        button.setOnAction(event -> {
            if (!taskCountHoldRepeated) {
                stepTaskCount(direction);
            }
            taskCountHoldRepeated = false;
        });
        button.setOnMousePressed(event -> startTaskCountHold(direction));
        button.setOnMouseReleased(event -> stopTaskCountHold());
        button.setOnMouseExited(event -> stopTaskCountHold());
    }

    private Parent buildTaskTile(TaskType taskType) {
        Label orderLabel = new Label();
        orderLabel.getStyleClass().add("task-order-badge");
        Label nameLabel = new Label(taskType.getDisplayName());
        nameLabel.getStyleClass().add("task-tile-name");
        nameLabel.setMinHeight(32);
        nameLabel.setPrefHeight(32);
        nameLabel.setMaxWidth(66);
        nameLabel.setWrapText(true);
        Label metaLabel = new Label(taskMetaText(taskType));
        metaLabel.getStyleClass().add("task-tile-meta");
        Label countLabel = new Label(taskCountSummaries.getOrDefault(taskType, "按需"));
        countLabel.getStyleClass().add("task-count-badge");
        countLabel.setMinHeight(18);
        countLabel.setPrefHeight(18);
        countLabel.setOnMouseClicked(event -> {
            event.consume();
            openTaskCountInlineEditor(taskType);
        });

        VBox textContent = new VBox(3, nameLabel, metaLabel, countLabel);
        VBox.setMargin(countLabel, new Insets(2, 0, 0, 0));
        textContent.setAlignment(Pos.TOP_CENTER);
        textContent.setPrefSize(66, 70);
        textContent.setMaxSize(66, 70);
        StackPane content = new StackPane(textContent, orderLabel);
        content.setPrefSize(82, 82);
        content.setMaxSize(82, 82);
        StackPane.setAlignment(orderLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(orderLabel, new Insets(6, 6, 0, 0));
        StackPane.setAlignment(textContent, Pos.TOP_CENTER);
        StackPane.setMargin(textContent, new Insets(12, 8, 0, 8));
        content.setAlignment(Pos.TOP_CENTER);
        Button tile = new Button();
        tile.setGraphic(content);
        tile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tile.setMinSize(82, 82);
        tile.setPrefSize(82, 82);
        tile.setMaxSize(82, 82);
        tile.getStyleClass().add("task-tile");
        tile.setUserData(taskType);
        tile.setOnAction(event -> toggleTaskSelection(taskType));
        taskTileButtons.add(tile);
        return tile;
    }

    private void openTaskCountInlineEditor(TaskType taskType) {
        if (taskType == null || taskType == TaskType.UNKNOWN || taskCountEditorBar == null) {
            return;
        }
        if (taskType == activeTaskCountType && taskCountEditorBar.isVisible()) {
            taskCountEditorField.requestFocus();
            taskCountEditorField.selectAll();
            return;
        }
        activeTaskCountType = taskType;
        TaskCountDisplay display = parseTaskCountDisplay(taskCountSummaries.getOrDefault(taskType, "按需"));
        taskCountEditorTitleLabel.setText(taskType.getDisplayName());
        taskCountEditorField.setText(String.valueOf(display.value()));
        taskCountEditorUnitLabel.setText(display.unit());
        taskCountEditorBar.getStyleClass().remove("task-count-editor-hidden");
        taskCountEditorBar.setVisible(true);
        taskCountEditorBar.setManaged(true);
    }

    private void openFirstTaskCountEditor() {
        TaskType taskType = pendingTaskQueue.isEmpty() ? TaskType.WUHuan : pendingTaskQueue.get(0);
        openTaskCountInlineEditor(taskType);
    }

    private void stepTaskCount(int delta) {
        if (taskCountEditorField == null) {
            return;
        }
        int value = parsePositiveInt(taskCountEditorField.getText(), 1);
        taskCountEditorField.setText(String.valueOf(Math.max(1, value + delta)));
    }

    private void startTaskCountHold(int direction) {
        stopTaskCountHold();
        taskCountHoldRepeated = false;
        taskCountHoldTimeline = new Timeline(new KeyFrame(Duration.millis(350), event -> {
            taskCountHoldRepeated = true;
            stepTaskCount(direction * 10);
        }));
        taskCountHoldTimeline.setDelay(Duration.millis(550));
        taskCountHoldTimeline.setCycleCount(Timeline.INDEFINITE);
        taskCountHoldTimeline.play();
    }

    private void stopTaskCountHold() {
        if (taskCountHoldTimeline != null) {
            taskCountHoldTimeline.stop();
            taskCountHoldTimeline = null;
        }
    }

    private void applyTaskCountEditor() {
        if (activeTaskCountType == null || taskCountEditorField == null || taskCountEditorUnitLabel == null) {
            hideTaskCountEditor();
            return;
        }
        int value = parsePositiveInt(taskCountEditorField.getText(), 1);
        String unit = taskCountEditorUnitLabel.getText() == null || taskCountEditorUnitLabel.getText().isBlank()
                ? "次"
                : taskCountEditorUnitLabel.getText().trim();
        taskCountSummaries.put(activeTaskCountType, value + unit);
        syncTaskRunCountToProperties(activeTaskCountType, value);
        refreshPendingTaskQueueView();
        refreshTaskTiles();
        hideTaskCountEditor();
    }

    private void hideTaskCountEditor() {
        activeTaskCountType = null;
        if (taskCountEditorBar != null) {
            if (!taskCountEditorBar.getStyleClass().contains("task-count-editor-hidden")) {
                taskCountEditorBar.getStyleClass().add("task-count-editor-hidden");
            }
            taskCountEditorBar.setVisible(false);
            taskCountEditorBar.setManaged(true);
        }
    }

    private TaskCountDisplay parseTaskCountDisplay(String text) {
        if (text == null || text.isBlank() || "手动".equals(text) || "按需".equals(text)) {
            return new TaskCountDisplay(1, "次");
        }
        String trimmed = text.trim();
        String digits = trimmed.replaceAll("\\D+", "");
        int value = parsePositiveInt(digits, 1);
        String unit = trimmed.replaceAll("\\d+", "").trim();
        return new TaskCountDisplay(value, unit.isBlank() ? "次" : unit);
    }

    private int parsePositiveInt(String text, int fallback) {
        try {
            int value = Integer.parseInt(text == null ? "" : text.trim());
            return Math.max(1, value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class TaskCountDisplay {

        int value;

        String unit;

    }

    private void openTaskCountEditor(TaskType taskType) {
        if (taskType == null || taskType == TaskType.UNKNOWN) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(taskCountSummaries.getOrDefault(taskType, "按需"));
        dialog.setTitle("任务参数");
        dialog.setHeaderText(taskType.getDisplayName() + " 次数/时长");
        dialog.setContentText("显示为");
        Optional<String> result = dialog.showAndWait();
        result.map(String::trim)
                .filter(value -> !value.isBlank())
                .ifPresent(value -> {
                    taskCountSummaries.put(taskType, value);
                    refreshPendingTaskQueueView();
                    addWindowLog("任务参数摘要已更新：" + taskType.getDisplayName() + " = " + value);
                    renderLogList();
                });
    }

    private String taskMetaText(TaskType taskType) {
        return switch (taskType) {
            case WUHuan -> "日常";
            case AUTO_BATTLE -> "挂机";
            case DEBUG_COORDINATE, DEBUG_MAP_CALIBRATOR -> "诊断";
            case DEBUG_TEAM_ROLE -> "识别";
            case DEBUG_XIULUO_STORY_OBJECTIVE, DEBUG_XIULUO_TASK_PANEL_OBJECTIVE, DEBUG_XIULUO_MOCK_OBJECTIVE -> "修罗";
            default -> "任务";
        };
    }

    private void toggleTaskSelection(TaskType taskType) {
        if (taskType == null || taskType == TaskType.UNKNOWN) {
            return;
        }
        if (windowTaskTypeComboBox != null && windowTaskTypeComboBox.getValue() != taskType) {
            windowTaskTypeComboBox.setValue(taskType);
        }
        if (pendingTaskQueue.contains(taskType)) {
            pendingTaskQueue.remove(taskType);
        } else {
            pendingTaskQueue.add(taskType);
        }
        refreshPendingTaskQueueView();
        refreshControlStates();
    }

    private void refreshTaskTiles() {
        for (Button tile : taskTileButtons) {
            Object userData = tile.getUserData();
            if (!(userData instanceof TaskType taskType)) {
                continue;
            }
            int index = pendingTaskQueue.indexOf(taskType);
            tile.getStyleClass().remove("task-tile-selected");
            Parent graphic = (Parent) tile.getGraphic();
            Label orderLabel = (Label) graphic.lookup(".task-order-badge");
            Label countLabel = (Label) graphic.lookup(".task-count-badge");
            if (countLabel != null) {
                countLabel.setText(taskCountSummaries.getOrDefault(taskType, "按需"));
            }
            if (index >= 0) {
                tile.getStyleClass().add("task-tile-selected");
                if (orderLabel != null) {
                    orderLabel.setText(String.valueOf(index + 1));
                    orderLabel.setVisible(true);
                    orderLabel.setManaged(true);
                }
            } else if (orderLabel != null) {
                orderLabel.setText("");
                orderLabel.setVisible(false);
                orderLabel.setManaged(false);
            }
        }
    }

    private void refreshTaskSelectorSummary() {
        if (taskSelectionSummaryLabel == null) {
            return;
        }
        if (pendingTaskQueue.isEmpty()) {
            taskSelectionSummaryLabel.setText("还没有选择任务");
            return;
        }
        taskSelectionSummaryLabel.setText("已选择：" + pendingTaskQueue.stream()
                .map(TaskType::getDisplayName)
                .toList());
    }

    private Parent buildLogPanel() {
        VBox wrapper = new VBox(6, new Label("窗口命令日志"), logList);
        wrapper.setPadding(new Insets(12, 0, 0, 0));
        wrapper.setPrefHeight(170);
        wrapper.setMinHeight(120);
        return wrapper;
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
        windowTaskControlService.stopAll();
    }

    private void registerOrRefreshWindowFromUi() {
        WindowRegistrationRequest request = WindowRegistrationRequest.of(
                windowIdField.getText(),
                WindowRole.UNKNOWN,
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
            refreshSelectionDependentUi();
        }
    }

    private void clearWindowSelection() {
        if (windowTable != null) {
            windowTable.getSelectionModel().clearSelection();
            refreshSelectionDependentUi();
        }
    }

    private void selectWindowsByState(java.util.function.Predicate<WindowTaskSnapshot> predicate) {
        if (windowTable == null || predicate == null) {
            return;
        }
        windowTable.getSelectionModel().clearSelection();
        for (WindowTaskSnapshot snapshot : windowTable.getItems()) {
            if (predicate.test(snapshot)) {
                windowTable.getSelectionModel().select(snapshot);
            }
        }
        refreshSelectionDependentUi();
    }

    private boolean isProblemWindow(WindowTaskSnapshot snapshot) {
        return snapshot != null
                && (snapshot.getStatus() == WindowRuntimeStatus.ERROR
                || snapshot.getStatus() == WindowRuntimeStatus.STOPPED);
    }

    private boolean matchesWindowSearch(WindowTaskSnapshot snapshot) {
        if (snapshot == null || windowSearchField == null || windowSearchField.getText() == null
                || windowSearchField.getText().isBlank()) {
            return true;
        }
        String keyword = windowSearchField.getText().trim().toLowerCase();
        WindowIdentityView identity = parseWindowIdentity(snapshot);
        return List.of(
                        snapshot.getWindowId(),
                        snapshot.getNativeTitle(),
                        identity.roleName(),
                        identity.serverName(),
                        identity.playerId())
                .stream()
                .filter(value -> value != null)
                .map(String::toLowerCase)
                .anyMatch(value -> value.contains(keyword));
    }

    private void applySelectedTaskToSelectedWindows() {
        TaskType taskType = windowTaskTypeComboBox.getValue();
        if (taskType == null || taskType == TaskType.UNKNOWN) {
            addWindowLog("未选择有效任务，无法设置选中窗口任务");
            renderLogList();
            return;
        }
        List<WindowRegistrationRequest> requests = getSelectedWindowSnapshots().stream()
                .map(snapshot -> WindowRegistrationRequest.of(
                        snapshot.getWindowId(),
                        snapshot.getRole(),
                        snapshot.getRoleName(),
                        taskType,
                        snapshot.getNativeBinding()))
                .toList();
        if (requests.isEmpty()) {
            addWindowLog("没有选中的窗口，无法设置任务");
            renderLogList();
            return;
        }
        runWindowCommandInBackground(() -> windowTaskControlService.registerWindows(requests));
    }

    private Parent buildTaskQueuePanel() {
        FlowPane queueInputRow = buildControlRow(
                new Label("任务队列"), queueTaskTypeComboBox,
                addCurrentTaskToQueueButton,
                addQueueTaskButton);
        FlowPane queueManageRow = buildControlRow(
                removeQueueTaskButton,
                moveQueueTaskUpButton,
                moveQueueTaskDownButton,
                clearQueueButton,
                startQueueButton);
        FlowPane queuePresetRow = buildControlRow(
                presetCurrentTaskQueueButton,
                presetFiveRingQueueButton,
                presetAutoBattleQueueButton,
                presetFiveRingAutoBattleQueueButton);
        return buildSection("任务队列", queueSummaryLabel, queueInputRow, queueManageRow, queuePresetRow, queueTaskList);
    }

    private void addSelectedTaskToQueue() {
        addTaskToQueue(queueTaskTypeComboBox.getValue());
    }

    private void addTaskToQueue(TaskType taskType) {
        if (taskType == null || taskType == TaskType.UNKNOWN) {
            addWindowLog("任务队列未选择有效任务");
            renderLogList();
            return;
        }
        pendingTaskQueue.add(taskType);
        refreshPendingTaskQueueView();
        refreshControlStates();
    }

    private void removeSelectedQueueTask() {
        if (queueTaskList == null || pendingTaskQueue.isEmpty()) {
            return;
        }
        int selectedIndex = queueTaskList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < pendingTaskQueue.size()) {
            pendingTaskQueue.remove(selectedIndex);
            refreshPendingTaskQueueView();
            selectQueueIndex(Math.min(selectedIndex, pendingTaskQueue.size() - 1));
            refreshControlStates();
        }
    }

    private void moveSelectedQueueTask(int direction) {
        if (queueTaskList == null || pendingTaskQueue.size() < 2 || direction == 0) {
            return;
        }
        int selectedIndex = queueTaskList.getSelectionModel().getSelectedIndex();
        int targetIndex = selectedIndex + direction;
        if (selectedIndex < 0 || selectedIndex >= pendingTaskQueue.size()
                || targetIndex < 0 || targetIndex >= pendingTaskQueue.size()) {
            return;
        }
        TaskType selectedTask = pendingTaskQueue.remove(selectedIndex);
        pendingTaskQueue.add(targetIndex, selectedTask);
        refreshPendingTaskQueueView();
        selectQueueIndex(targetIndex);
        refreshControlStates();
    }

    private void clearPendingTaskQueue() {
        pendingTaskQueue.clear();
        refreshPendingTaskQueueView();
        refreshControlStates();
    }

    private void setPendingTaskQueue(TaskType... taskTypes) {
        pendingTaskQueue.clear();
        if (taskTypes != null) {
            for (TaskType taskType : taskTypes) {
                if (taskType != null && taskType != TaskType.UNKNOWN) {
                    pendingTaskQueue.add(taskType);
                }
            }
        }
        refreshPendingTaskQueueView();
        selectQueueIndex(0);
        refreshControlStates();
    }

    private void startPendingTaskQueue() {
        if (!syncDebugTaskConfigFromUi(pendingTaskQueue)) {
            return;
        }
        if (pendingTaskQueue.isEmpty()) {
            addWindowLog("任务队列为空，无法启动");
            renderLogList();
            return;
        }
        syncTaskRunCountsFromTileEditor(pendingTaskQueue);
        addMapCalibratorUiHintIfNeeded(pendingTaskQueue);
        WindowTaskQueue queue = WindowTaskQueue.of(pendingTaskQueue);
        List<String> windowIds = getSelectedWindowIds();
        warnUnavailableSelectedWindows("启动队列");
        runWindowCommandInBackground(() ->
                windowTaskControlService.start(WindowTaskStartRequest.sameQueue(windowIds, queue)));
    }

    private void startMainSelectedTasks() {
        if (!syncDebugTaskConfigFromUi(pendingTaskQueue)) {
            return;
        }
        TaskType selectedTaskType = windowTaskTypeComboBox == null ? null : windowTaskTypeComboBox.getValue();
        log.info("UI startMainSelectedTasks entered: selectedTask={} pendingQueue={} selectedWindows={}",
                selectedTaskType, pendingTaskQueue, getSelectedWindowIds());
        if (pendingTaskQueue.isEmpty()) {
            addWindowLog("还没有选择任务，无法启动");
            renderLogList();
            return;
        }
        syncTaskRunCountsFromTileEditor(pendingTaskQueue);
        addMapCalibratorUiHintIfNeeded(pendingTaskQueue);
        List<String> selectedWindowIds = getSelectedWindowIds();
        WindowTaskQueue queue = WindowTaskQueue.of(pendingTaskQueue);
        TaskType defaultTaskType = pendingTaskQueue.get(0);
        addWindowLog("启动：自动刷新/发现游戏窗口，然后启动可接任务窗口");
        renderLogList();
        runWindowCommandInBackground(() -> {
            log.info("Start selected task flow: refresh/register start defaultTask={} selectedWindows={}",
                    defaultTaskType, selectedWindowIds);
            WindowTaskCommandResult scanResult = gameWindowRegistrationService.registerDetectedGameWindows(defaultTaskType);
            log.info("Start selected task flow: register result requested={} success={} failed={} message={}",
                    scanResult.getRequestedCount(), scanResult.getSuccessCount(), scanResult.getFailedCount(), scanResult.getMessage());
            List<WindowTaskSnapshot> latestSnapshots = windowTaskControlService.getSnapshots();
            log.info("Start selected task flow: snapshot count={} accepting={}",
                    latestSnapshots.size(),
                    latestSnapshots.stream()
                            .filter(WindowTaskSnapshot::isAcceptingTaskQueue)
                            .map(WindowTaskSnapshot::getWindowId)
                            .toList());
            List<String> targetWindowIds = latestSnapshots.stream()
                    .filter(WindowTaskSnapshot::isAcceptingTaskQueue)
                    .filter(snapshot -> selectedWindowIds.isEmpty()
                            || selectedWindowIds.contains(snapshot.getWindowId()))
                    .map(WindowTaskSnapshot::getWindowId)
                    .filter(id -> id != null && !id.isBlank())
                    .distinct()
                    .toList();
            log.info("Start selected task flow: targetWindowIds={}", targetWindowIds);
            if (selectedWindowIds.isEmpty()) {
                pendingAutoSelectedWindowIds = targetWindowIds;
            }
            if (targetWindowIds.isEmpty()) {
                String message = selectedWindowIds.isEmpty()
                        ? "已自动刷新窗口，但没有可启动窗口"
                        : "已自动刷新窗口，但选中的窗口已失效或当前不可接任务，未启动旧绑定";
                return WindowTaskCommandResult.empty(message, latestSnapshots.isEmpty() ? scanResult.getSnapshots() : latestSnapshots);
            }
            log.info("Start selected task flow: submit start queue={} targets={}", queue.toLogText(), targetWindowIds);
            return windowTaskControlService.start(WindowTaskStartRequest.sameQueue(targetWindowIds, queue));
        });
    }

    private void scanAndRefreshGameWindowsFromMain() {
        TaskType defaultTaskType = windowTaskTypeComboBox == null ? TaskType.WUHuan : windowTaskTypeComboBox.getValue();
        if (defaultTaskType == null || defaultTaskType == TaskType.UNKNOWN) {
            defaultTaskType = TaskType.WUHuan;
        }
        TaskType scanTaskType = defaultTaskType;
        addWindowLog("刷新：正在扫描游戏窗口并更新绑定...");
        setActionHint("正在扫描游戏窗口...");
        renderLogList();
        runWindowCommandInBackground(() -> {
            log.info("Main refresh scan/register started: taskType={}", scanTaskType);
            return gameWindowRegistrationService.registerDetectedGameWindows(scanTaskType);
        });
    }

    private void startWindows(List<String> windowIds, String actionName) {
        if (!syncDebugTaskConfigFromUi(pendingTaskQueue)) {
            return;
        }
        if (pendingTaskQueue.isEmpty()) {
            addWindowLog(actionName + "失败：还没有选择任务");
            renderLogList();
            return;
        }
        syncTaskRunCountsFromTileEditor(pendingTaskQueue);
        addMapCalibratorUiHintIfNeeded(pendingTaskQueue);
        WindowTaskQueue queue = WindowTaskQueue.of(pendingTaskQueue);
        runWindowCommandInBackground(() ->
                windowTaskControlService.start(WindowTaskStartRequest.sameQueue(windowIds, queue)));
    }

    private void togglePauseResumeSelectedWindows() {
        List<String> windowIds = getSelectedWindowIds();
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        boolean shouldResume = shouldShowResumeAction(selected);
        runWindowCommandInBackground(() -> shouldResume
                ? windowTaskControlService.resumeWindows(windowIds)
                : windowTaskControlService.pauseWindows(windowIds));
    }

    private void refreshPendingTaskQueueView() {
        refreshPendingTaskQueueSummary();
        refreshTaskTiles();
        refreshTaskSelectorSummary();
        if (queueTaskList == null) {
            return;
        }
        if (pendingTaskQueue.isEmpty()) {
            queueTaskList.getItems().setAll("当前队列：-");
            return;
        }
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < pendingTaskQueue.size(); i++) {
            TaskType taskType = pendingTaskQueue.get(i);
            rows.add((i + 1) + ". " + taskType.getDisplayName() + " (" + taskType.getCode() + ")");
        }
        queueTaskList.getItems().setAll(rows);
    }

    private void refreshPendingTaskQueueSummary() {
        if (queueSummaryLabel == null) {
            return;
        }
        if (pendingTaskQueue.isEmpty()) {
            queueSummaryLabel.setText("待提交队列：空");
            return;
        }
        String queueText = pendingTaskQueue.stream()
                .map(TaskType::getDisplayName)
                .toList()
                .toString();
        queueSummaryLabel.setText("待提交队列：" + pendingTaskQueue.size() + " 个 | " + queueText);
    }

    private void selectQueueIndex(int index) {
        if (queueTaskList == null || index < 0 || index >= pendingTaskQueue.size()) {
            return;
        }
        queueTaskList.getSelectionModel().select(index);
    }

    private void runWindowCaptureExperiment() {
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        if (selected.isEmpty()) {
            addWindowLog("截图实验失败：请先选择至少一个已注册窗口");
            renderLogList();
            return;
        }

        runWindowCommandInBackground(() -> {
            List<WindowCaptureExperimentService.WindowCaptureExperimentResult> results =
                    windowCaptureExperimentService.captureSelectedWindows(selected);
            int successCount = 0;
            List<WindowTaskCommandDetail> details = new ArrayList<>();
            for (WindowCaptureExperimentService.WindowCaptureExperimentResult result : results) {
                if (result.isSuccess()) {
                    successCount++;
                    details.add(WindowTaskCommandDetail.success(result.getWindowId(), result.toDetailMessage()));
                } else {
                    details.add(WindowTaskCommandDetail.failed(result.getWindowId(), result.toDetailMessage()));
                }
            }
            String message = "后台截图实验完成，输出目录：images/temp/window_capture_experiment";
            return WindowTaskCommandResult.of(selected.size(), successCount, message,
                    windowTaskControlService.getSnapshots(), List.of(), details);
        });
    }

    private void openInteractionMetricsDashboard() {
        try {
            Path dashboard = windowInteractionMetricsService.writeDashboardNow();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(dashboard.toUri());
                addWindowLog("统计 Dashboard 已打开：" + dashboard);
            } else {
                addWindowLog("统计 Dashboard 已生成：" + dashboard);
            }
        } catch (Exception e) {
            addWindowLog("统计 Dashboard 打开失败：" + e.getMessage());
        }
        renderLogList();
    }

    private void runWindowMessageInputExperiment(String actionName, WindowMessageExperimentCommand command) {
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        if (selected.isEmpty()) {
            addWindowLog(actionName + "失败：请先选择至少一个已注册窗口");
            renderLogList();
            return;
        }

        runWindowCommandInBackground(() -> {
            List<WindowMessageInputExperimentService.WindowMessageInputExperimentResult> results = command.execute(selected);
            int successCount = 0;
            List<WindowTaskCommandDetail> details = new ArrayList<>();
            for (WindowMessageInputExperimentService.WindowMessageInputExperimentResult result : results) {
                if (result.isPosted()) {
                    successCount++;
                    details.add(WindowTaskCommandDetail.success(result.getWindowId(), result.toDetailMessage()));
                } else {
                    details.add(WindowTaskCommandDetail.failed(result.getWindowId(), result.toDetailMessage()));
                }
            }
            String message = actionName + "完成，输出目录：images/temp/window_input_experiment";
            return WindowTaskCommandResult.of(selected.size(), successCount, message,
                    windowTaskControlService.getSnapshots(), List.of(), details);
        });
    }

    private void runMapSurveyCommand(String actionName, MapSurveyCommand command) {
        runMapSurveyCommand(actionName, true, command);
    }

    private void runPlayerNameOcrDebug() {
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        if (selected.size() != 1) {
            addWindowLog("本地OCR测名字失败：请选择一个窗口，当前已选 " + selected.size());
            renderLogList();
            return;
        }

        WindowTaskSnapshot target = selected.get(0);
        WindowIdentityView identity = parseWindowIdentity(target);
        String expectedName = identity.roleName();
        addWindowLog("本地OCR测名字开始：窗口=" + nullToDash(target.getWindowId())
                + " 期望角色名=" + nullToDash(expectedName));
        renderLogList();

        runWindowCommandInBackground(() -> {
            PlayerNameOcrDebugService.DebugResult result =
                    playerNameOcrDebugService.debugLocalNameOcr(target, expectedName);
            javafx.application.Platform.runLater(() -> showPlayerNameOcrDebugDialog(result));
            WindowTaskCommandDetail detail = result.success()
                    ? WindowTaskCommandDetail.success(result.windowId(), result.toDetailMessage())
                    : WindowTaskCommandDetail.failed(result.windowId(), result.toDetailMessage());
            return WindowTaskCommandResult.of(1, result.success() ? 1 : 0,
                    "本地OCR测名字" + (result.success() ? "成功" : "失败") + "：" + result.message(),
                    windowTaskControlService.getSnapshots(), List.of(), List.of(detail));
        });
    }

    private void showPlayerNameOcrDebugDialog(PlayerNameOcrDebugService.DebugResult result) {
        if (result == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("本地OCR测名字");
        alert.setHeaderText(result.success() ? "识别完成" : "识别失败");

        TextArea textArea = new TextArea(formatPlayerNameOcrDebugDialogText(result));
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefColumnCount(88);
        textArea.setPrefRowCount(14);
        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(820);
        alert.setResizable(true);
        alert.show();
    }

    private String formatPlayerNameOcrDebugDialogText(PlayerNameOcrDebugService.DebugResult result) {
        String scan = result.scanRect() == null
                ? "-"
                : result.scanRect().x() + "," + result.scanRect().y()
                + " " + result.scanRect().width() + "x" + result.scanRect().height();
        return "窗口: " + nullToDash(result.windowId())
                + "\n期望角色名: " + nullToDash(result.expectedName())
                + "\n结果: " + nullToDash(result.message())
                + "\n\n新算法：紫字分割增强 OCR:"
                + formatOcrVariant(result.purpleEnhanced())
                + "\n\n新算法：黄字/NPC名分割增强 OCR:"
                + formatOcrVariant(result.yellowEnhanced())
                + "\n\n定位:"
                + "\n  scan=" + scan
                + "\n  rel=" + formatPoint(result.anchorRelative())
                + "\n  abs=" + formatPoint(result.anchorAbsolute())
                + "\n  mouseMoved=" + result.mouseMoved()
                + "\n\n图片:"
                + "\n  purpleSegmentedEnhanced=" + nullToDash(result.purpleEnhanced().path())
                + "\n  yellowSegmentedEnhanced=" + nullToDash(result.yellowEnhanced().path());
    }

    private String formatOcrVariant(PlayerNameOcrDebugService.OcrVariant variant) {
        if (variant == null) {
            return "\n  blackPixels=0\n  words=0\n  text=-";
        }
        return "\n  blackPixels=" + variant.blackPixelCount()
                + "\n  words=" + variant.wordCount()
                + "\n  text=" + nullToDash(variant.wordsSummary());
    }

    private String formatPoint(java.awt.Point point) {
        return point == null ? "-" : point.x + "," + point.y;
    }

    private void runMapSurveyCommand(String actionName, boolean requireMapName, MapSurveyCommand command) {
        if (mapCalibratorMapNameField != null) {
            botProperties.setDebugMapCalibratorMapName(mapCalibratorMapNameField.getText() == null
                    ? ""
                    : mapCalibratorMapNameField.getText().trim());
        }
        String mapName = botProperties.getDebugMapCalibratorMapName();
        if (requireMapName && !hasText(mapName)) {
            addWindowLog(actionName + "失败：请先填写地图校准名");
            renderLogList();
            return;
        }

        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        if (selected.size() != 1) {
            addWindowLog(actionName + "失败：请选择一个窗口，当前已选 " + selected.size());
            renderLogList();
            return;
        }

        WindowTaskSnapshot target = selected.get(0);
        String mapLogText = hasText(mapName) ? mapName : "自动识别";
        addWindowLog(actionName + "开始：地图=" + mapLogText + " 窗口=" + nullToDash(target.getWindowId()));
        if (actionName.startsWith("记录")) {
            addWindowLog(actionName + "提示：点完按钮后有3秒准备时间，请把鼠标移到角色身上/脚下");
        }
        renderLogList();
        runWindowCommandInBackground(() -> {
            MapSurveyService.SurveyResult result = command.execute(target, mapName);
            WindowTaskCommandDetail detail = result.success()
                    ? WindowTaskCommandDetail.success(result.windowId(), result.message())
                    : WindowTaskCommandDetail.failed(result.windowId(), result.message());
            String message = actionName + (result.success() ? "成功" : "失败") + "：" + result.message();
            return WindowTaskCommandResult.of(1, result.success() ? 1 : 0, message,
                    windowTaskControlService.getSnapshots(), List.of(), List.of(detail));
        });
    }

    private List<String> getSelectedWindowIds() {
        if (windowTable == null) {
            return List.of();
        }
        return getSelectedWindowSnapshots().stream()
                .map(WindowTaskSnapshot::getWindowId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    private List<WindowTaskSnapshot> getSelectedWindowSnapshots() {
        if (windowTable == null) {
            return List.of();
        }
        return List.copyOf(windowTable.getSelectionModel().getSelectedItems());
    }

    private void warnUnavailableSelectedWindows(String actionName) {
        List<String> unavailableWindowIds = getSelectedWindowSnapshots().stream()
                .filter(snapshot -> !snapshot.isAcceptingTaskQueue())
                .map(WindowTaskSnapshot::getWindowId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (!unavailableWindowIds.isEmpty()) {
            addWindowLog(actionName + "提示：以下窗口当前不可接任务，后端可能拒绝提交："
                    + String.join(", ", unavailableWindowIds));
            renderLogList();
        }
    }

    private void runWindowCommandInBackground(WindowCommand command) {
        log.info("Window command scheduled");
        setWindowButtonsDisabled(true);
        Thread worker = new Thread(() -> {
            long startedAt = System.currentTimeMillis();
            log.info("Window command worker started");
            WindowTaskCommandResult result;
            try {
                result = command.execute();
            } catch (Exception e) {
                log.error("Window command failed", e);
                result = WindowTaskCommandResult.empty("窗口命令异常：" + e.getMessage(), windowTaskControlService.getSnapshots());
            }
            log.info("Window command worker finished: requested={} success={} failed={} elapsedMs={} message={}",
                    result == null ? null : result.getRequestedCount(),
                    result == null ? null : result.getSuccessCount(),
                    result == null ? null : result.getFailedCount(),
                    System.currentTimeMillis() - startedAt,
                    result == null ? null : result.getMessage());
            WindowTaskCommandResult finalResult = result;
            javafx.application.Platform.runLater(() -> {
                log.info("Window command UI update: resultMessage={}",
                        finalResult == null ? null : finalResult.getMessage());
                handleWindowCommandResult(finalResult);
                setWindowButtonsDisabled(false);
            });
        }, "window-task-ui-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void setWindowButtonsDisabled(boolean disabled) {
        windowCommandRunning = disabled;
        List.of(registerWindowButton, registerTeamButton, scanGameWindowsButton, startIndependentWindowsButton,
                        applySelectedTaskButton,
                        addCurrentTaskToQueueButton, addQueueTaskButton, removeQueueTaskButton, moveQueueTaskUpButton, moveQueueTaskDownButton,
                        clearQueueButton, startQueueButton, presetCurrentTaskQueueButton, presetFiveRingQueueButton,
                        presetAutoBattleQueueButton, presetFiveRingAutoBattleQueueButton,
                        presetDebugCoordinateQueueButton, setDebugCoordinateTaskButton, windowCaptureExperimentButton,
                        backgroundAltQExperimentButton, backgroundCenterClickExperimentButton, backgroundCenterRightClickExperimentButton,
                        backgroundChildRightClickExperimentButton,
                        playerNameOcrDebugButton, saveMapLabelSampleButton, testMapLabelSampleButton,
                        recordCameraLeftButton, recordCameraRightButton, recordCameraTopButton, recordCameraBottomButton,
                        recordCameraCenterButton, testProjectedPlayerPointButton,
                        recordPlayerPointCorrectionButton, testCorrectedPlayerPointButton, undoPlayerPointCorrectionButton,
                        selectAllWindowsButton, selectRunningWindowsButton, selectIdleWindowsButton,
                        selectProblemWindowsButton, selectBoundWindowsButton, selectUnboundWindowsButton, clearWindowSelectionButton,
                        startCurrentTaskButton, startWindowSelectedTaskButton, startTeamRoleDebugButton,
                        pauseSelectedWindowsButton, resumeSelectedWindowsButton, pauseAllWindowsButton, resumeAllWindowsButton,
                        stopSelectedWindowsButton, stopAllWindowsButton, unregisterSelectedWindowsButton,
                        unregisterAllWindowsButton, refreshWindowButton,
                        windowSelectionMenuButton, windowManageMenuButton, runControlMenuButton)
                .forEach(button -> {
                    if (button != null) {
                        button.setDisable(disabled);
                    }
                });
        refreshControlStates();
    }

    private void handleWindowCommandResult(WindowTaskCommandResult result) {
        if (result == null) {
            refreshWindowPanel();
            return;
        }
        addWindowLog("命令结果：" + result.getMessage()
                + " | 请求=" + result.getRequestedCount()
                + " 成功=" + result.getSuccessCount()
                + " 失败=" + result.getFailedCount());
        if (result.hasAssignments()) {
            result.getAssignments().forEach(assignment -> addWindowLog("任务分配：" + nullToDash(assignment.getWindowId())
                    + " -> " + assignment.getTaskDisplayName()
                    + " | " + assignment.getReason()));
        }
        if (result.hasDetails()) {
            for (WindowTaskCommandDetail detail : result.getDetails()) {
                addWindowLog(formatCommandDetail(detail));
            }
        }
        refreshWindowPanel();
        applyPendingAutoSelection();
        renderLogList();
    }

    private void applyPendingAutoSelection() {
        if (pendingAutoSelectedWindowIds == null || pendingAutoSelectedWindowIds.isEmpty() || windowTable == null) {
            return;
        }
        List<String> ids = pendingAutoSelectedWindowIds;
        pendingAutoSelectedWindowIds = List.of();
        windowTable.getSelectionModel().clearSelection();
        for (WindowTaskSnapshot window : windowTable.getItems()) {
            if (ids.contains(window.getWindowId())) {
                windowTable.getSelectionModel().select(window);
            }
        }
        refreshSelectionDependentUi();
    }

    private String formatCommandDetail(WindowTaskCommandDetail detail) {
        if (detail == null) {
            return "[失败] - | -";
        }
        StringBuilder builder = new StringBuilder()
                .append(detail.isSuccess() ? "[成功] " : "[失败] ")
                .append(nullToDash(detail.getWindowId()))
                .append(" | ")
                .append(nullToDash(detail.getMessage()));
        if (hasText(detail.getTaskQueueDisplayText()) && !"-".equals(detail.getTaskQueueDisplayText())) {
            builder.append(" | 队列=").append(detail.getTaskQueueDisplayText());
        }
        if (hasText(detail.getSubmitStatusDisplayName()) && !"-".equals(detail.getSubmitStatusDisplayName())) {
            builder.append(" | 提交=").append(detail.getSubmitStatusDisplayName());
        }
        if (hasText(detail.getTaskQueueFailurePolicyDisplayName()) && !"-".equals(detail.getTaskQueueFailurePolicyDisplayName())) {
            builder.append(" | 失败策略=").append(detail.getTaskQueueFailurePolicyDisplayName());
        }
        return builder.toString();
    }

    private void addWindowLog(String message) {
        String text = "[" + UI_LOG_TIME_FORMATTER.format(LocalTime.now()) + "] " + nullToDash(message);
        windowCommandLogs.add(0, text);
        while (windowCommandLogs.size() > MAX_WINDOW_COMMAND_LOGS) {
            windowCommandLogs.remove(windowCommandLogs.size() - 1);
        }
    }

    private void clearWindowLogs() {
        windowCommandLogs.clear();
    }

    private void refreshDashboard() {
        refreshWindowPanel();
        renderLogList();
    }

    private void refreshWindowPanel() {
        if (windowTable == null || windowSystemLabel == null) {
            return;
        }
        WindowSystemSnapshot snapshot = windowTaskControlService.getSystemSnapshot();
        List<String> selectedWindowIds = getSelectedWindowIds();
        WindowTableFilter filter = windowFilterComboBox == null || windowFilterComboBox.getValue() == null
                ? WindowTableFilter.ALL
                : windowFilterComboBox.getValue();
        List<WindowTaskSnapshot> visibleWindows = snapshot.getWindows().stream()
                .filter(filter::matches)
                .filter(this::matchesWindowSearch)
                .toList();
        long visibleAcceptingCount = visibleWindows.stream()
                .filter(WindowTaskSnapshot::isAcceptingTaskQueue)
                .count();
        long visibleBoundCount = visibleWindows.stream()
                .filter(WindowTaskSnapshot::hasNativeBinding)
                .count();
        long problemCount = snapshot.getWindows().stream()
                .filter(this::isProblemWindow)
                .count();
        windowTable.getItems().setAll(visibleWindows);
        for (WindowTaskSnapshot window : windowTable.getItems()) {
            if (selectedWindowIds.contains(window.getWindowId())) {
                windowTable.getSelectionModel().select(window);
            }
        }
        windowSystemLabel.setText("已注册 " + snapshot.getRegisteredWindowCount()
                + " / " + snapshot.getMaxWindowCount()
                + "，已选 " + selectedWindowIds.size()
                + "，当前显示 " + visibleWindows.size()
                + "，可接 " + visibleAcceptingCount
                + "，已绑定 " + visibleBoundCount
                + "，剩余 " + snapshot.getRemainingWindowCapacity());
        if (windowMetricLabel != null) {
            windowMetricLabel.setText(snapshot.getRegisteredWindowCount() + " / " + snapshot.getMaxWindowCount());
        }
        if (runningMetricLabel != null) {
            runningMetricLabel.setText(String.valueOf(snapshot.getRunningWindowCount()));
        }
        if (problemMetricLabel != null) {
            problemMetricLabel.setText(String.valueOf(problemCount));
        }
        refreshSelectionDependentUi();
    }

    private void refreshSelectionDependentUi() {
        refreshSelectedWindowDetailVisibility();
        refreshSelectedWindowDetail();
        refreshControlStates();
    }

    private void refreshSelectedWindowDetailVisibility() {
        if (selectedWindowDetailPanel == null) {
            return;
        }
        boolean hasSelection = !getSelectedWindowSnapshots().isEmpty();
        boolean shouldShow = selectedWindowDetailExpanded && hasSelection;
        selectedWindowDetailPanel.setVisible(shouldShow);
        selectedWindowDetailPanel.setManaged(shouldShow);
    }

    private void refreshSelectedWindowDetail() {
        if (selectedWindowDetailBox == null) {
            return;
        }
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        selectedWindowDetailBox.getChildren().clear();
        if (selected.isEmpty()) {
            return;
        }
        WindowTaskSnapshot snapshot = selected.get(0);
        WindowIdentityView identity = parseWindowIdentity(snapshot);
        String selectedPrefix = selected.size() == 1
                ? nullToDash(snapshot.getWindowId())
                : selected.size() + " 个已选，当前显示 " + nullToDash(snapshot.getWindowId());
        String processId = snapshot.getNativeProcessId() <= 0 ? "-" : String.valueOf(snapshot.getNativeProcessId());
        addDetailRow("窗口", selectedPrefix);
        addDetailRow("角色", identity.roleName() + " · " + identity.serverName() + " · " + identity.playerId());
        addDetailRow("状态", snapshot.getStatusDisplayName()
                + " · 可接任务 " + (snapshot.isAcceptingTaskQueue() ? "是" : "否"));
        addDetailRow("绑定", "hwnd=" + nullToDash(snapshot.getNativeHandle()) + " · pid=" + processId);
        addDetailRow("当前", snapshot.getRunningTaskDisplayName()
                + " · 进度 " + snapshot.getRunningQueueProgressText());
        addDetailRow("上次执行", nullToDash(snapshot.getLastQueueDisplayText())
                + " · " + snapshot.getLastQueueResultDisplayName());
        addDetailRow("最近任务", snapshot.getLastTaskDisplayName()
                + " · " + snapshot.getLastResultDisplayName());
        addDetailRow("结束时间", formatDateTime(snapshot.getLastFinishedAt()));
        addDetailRow("消息", firstNotBlank(snapshot.getLastQueueMessage(), snapshot.getLastResultMessage(), snapshot.getLastMessage()));
        addDetailRow("标题", nullToDash(snapshot.getNativeTitle()));
    }

    private void addDetailRow(String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("detail-key");
        Label valueLabel = new Label(nullToDash(value));
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("detail-value");
        HBox row = new HBox(10, keyLabel, valueLabel);
        row.getStyleClass().add("detail-row");
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        selectedWindowDetailBox.getChildren().add(row);
    }

    private void renderLogList() {
        if (logList == null) {
            return;
        }
        logList.getItems().setAll(windowCommandLogs);
    }

    private void refreshControlStates() {
        if (windowCommandRunning) {
            setActionHint("窗口命令执行中...");
            return;
        }
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        int selectedCount = selected.size();
        long acceptingCount = selected.stream()
                .filter(WindowTaskSnapshot::isAcceptingTaskQueue)
                .count();
        boolean hasSelection = selectedCount > 0;
        boolean hasQueue = !pendingTaskQueue.isEmpty();
        boolean showResumeAction = shouldShowResumeAction(selected);
        if (selectedWindowCountLabel != null) {
            selectedWindowCountLabel.setText("已选窗口：" + selectedCount);
        }

        setButtonDisabled(applySelectedTaskButton, !hasSelection);
        setButtonDisabled(startCurrentTaskButton, !hasQueue);
        setButtonDisabled(startWindowSelectedTaskButton, !hasSelection);
        setButtonDisabled(startTeamRoleDebugButton, !hasSelection);
        setButtonDisabled(startQueueButton, !hasSelection || !hasQueue);
        setButtonDisabled(windowCaptureExperimentButton, !hasSelection);
        setButtonDisabled(playerNameOcrDebugButton, !hasSelection);
        setButtonDisabled(backgroundAltQExperimentButton, !hasSelection);
        setButtonDisabled(backgroundCenterClickExperimentButton, !hasSelection);
        setButtonDisabled(backgroundCenterRightClickExperimentButton, !hasSelection);
        setButtonDisabled(backgroundChildRightClickExperimentButton, !hasSelection);
        if (pauseSelectedWindowsButton != null) {
            pauseSelectedWindowsButton.setText(showResumeAction ? "继续" : "暂停");
        }
        setButtonDisabled(pauseSelectedWindowsButton, !hasSelection);
        setButtonDisabled(resumeSelectedWindowsButton, !hasSelection);
        setButtonDisabled(stopSelectedWindowsButton, !hasSelection);
        setButtonDisabled(unregisterSelectedWindowsButton, !hasSelection);
        setMenuItemDisabled(pauseSelectedWindowsMenuItem, !hasSelection);
        setMenuItemDisabled(resumeSelectedWindowsMenuItem, !hasSelection);
        setMenuItemDisabled(stopSelectedWindowsMenuItem, !hasSelection);
        setMenuItemDisabled(unregisterSelectedWindowsMenuItem, !hasSelection);
        setButtonDisabled(removeQueueTaskButton, pendingTaskQueue.isEmpty());
        setButtonDisabled(moveQueueTaskUpButton, pendingTaskQueue.size() < 2);
        setButtonDisabled(moveQueueTaskDownButton, pendingTaskQueue.size() < 2);
        setButtonDisabled(clearQueueButton, pendingTaskQueue.isEmpty());

        if (!hasSelection) {
            setActionHint(windowSystemLabel == null ? "请选择窗口后再启动、停止或移除。" :
                    windowSystemLabel.getText() + "。点击启动会自动刷新窗口，并启动可接任务窗口。");
        } else if (acceptingCount == 0) {
            setActionHint("已选 " + selectedCount + " 个窗口，但当前没有窗口可接任务；可停止运行中窗口或等待任务结束。");
        } else if (!hasQueue) {
            setActionHint("已选 " + selectedCount + " 个窗口，可接任务 " + acceptingCount
                    + " 个；可直接启动当前任务，或先加入队列。");
        } else {
            setActionHint("已选 " + selectedCount + " 个窗口，可接任务 " + acceptingCount
                    + " 个；队列任务 " + pendingTaskQueue.size() + " 个。");
        }
    }

    private void setButtonDisabled(Button button, boolean disabled) {
        if (button != null) {
            button.setDisable(disabled);
        }
    }

    private void setMenuItemDisabled(MenuItem item, boolean disabled) {
        if (item != null) {
            item.setDisable(disabled);
        }
    }

    private void setActionHint(String text) {
        if (windowActionHintLabel != null) {
            windowActionHintLabel.setText("操作提示：" + nullToDash(text));
        }
    }

    private boolean shouldShowResumeAction(List<WindowTaskSnapshot> selected) {
        if (selected == null || selected.isEmpty()) {
            return false;
        }
        boolean hasPausedSelection = selected.stream()
                .anyMatch(snapshot -> snapshot.getStatus() == WindowRuntimeStatus.PAUSED);
        boolean hasActiveNonPausedSelection = selected.stream()
                .anyMatch(snapshot -> snapshot.getStatus() == WindowRuntimeStatus.RUNNING
                        || snapshot.getStatus() == WindowRuntimeStatus.QUEUED
                        || snapshot.getStatus() == WindowRuntimeStatus.STOPPING);
        return hasPausedSelection && !hasActiveNonPausedSelection;
    }

    private void addStyleClass(Node node, String styleClass) {
        if (node != null && styleClass != null && !node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : UI_DATE_TIME_FORMATTER.format(value);
    }

    private WindowIdentityView parseWindowIdentity(WindowTaskSnapshot snapshot) {
        if (snapshot == null) {
            return WindowIdentityView.empty();
        }
        if (hasText(snapshot.getPlayerName()) || hasText(snapshot.getServerName()) || hasText(snapshot.getPlayerId())) {
            return new WindowIdentityView(
                    nullToDash(snapshot.getPlayerName()),
                    nullToDash(snapshot.getServerName()),
                    nullToDash(snapshot.getPlayerId())
            );
        }
        String title = firstNotBlank(snapshot.getNativeTitle(), snapshot.getRoleName());
        if (title != null) {
            Matcher matcher = WINDOW_IDENTITY_PATTERN.matcher(title);
            if (matcher.find()) {
                return new WindowIdentityView(
                        nullToDash(matcher.group(2)),
                        nullToDash(matcher.group(1)),
                        nullToDash(matcher.group(3))
                );
            }
        }
        return new WindowIdentityView(nullToDash(snapshot.getRoleName()), "-", "-");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNotBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        if (hasText(second)) {
            return second;
        }
        return null;
    }

    private String firstNotBlank(String first, String second, String third) {
        String value = firstNotBlank(first, second);
        return hasText(value) ? value : firstNotBlank(third, null);
    }

    private List<TaskType> selectableTaskTypes() {
        return List.of(TaskType.values()).stream()
                .filter(taskType -> taskType != TaskType.UNKNOWN)
                .toList();
    }

    @FunctionalInterface
    private interface WindowCommand {
        WindowTaskCommandResult execute();
    }

    @FunctionalInterface
    private interface WindowMessageExperimentCommand {
        List<WindowMessageInputExperimentService.WindowMessageInputExperimentResult> execute(List<WindowTaskSnapshot> selected);
    }

    @FunctionalInterface
    private interface MapSurveyCommand {
        MapSurveyService.SurveyResult execute(WindowTaskSnapshot selected, String mapName);
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class WindowIdentityView {

        String roleName;

        String serverName;

        String playerId;

        private static WindowIdentityView empty() {
            return new WindowIdentityView("-", "-", "-");
        }
    

    }

    private enum WindowTableFilter {
        ALL("全部") {
            @Override
            boolean matches(WindowTaskSnapshot snapshot) {
                return true;
            }
        },
        RUNNING("运行中") {
            @Override
            boolean matches(WindowTaskSnapshot snapshot) {
                return snapshot != null && snapshot.isRunning();
            }
        },
        IDLE("空闲") {
            @Override
            boolean matches(WindowTaskSnapshot snapshot) {
                return snapshot != null && !snapshot.isBusy();
            }
        },
        PROBLEM("异常/停止") {
            @Override
            boolean matches(WindowTaskSnapshot snapshot) {
                return snapshot != null
                        && (snapshot.getStatus() == WindowRuntimeStatus.ERROR
                        || snapshot.getStatus() == WindowRuntimeStatus.STOPPED);
            }
        },
        BOUND("已绑定") {
            @Override
            boolean matches(WindowTaskSnapshot snapshot) {
                return snapshot != null && snapshot.hasNativeBinding();
            }
        },
        UNBOUND("未绑定") {
            @Override
            boolean matches(WindowTaskSnapshot snapshot) {
                return snapshot != null && !snapshot.hasNativeBinding();
            }
        };

        private final String displayName;

        WindowTableFilter(String displayName) {
            this.displayName = displayName;
        }

        String getDisplayName() {
            return displayName;
        }

        abstract boolean matches(WindowTaskSnapshot snapshot);
    }
}
