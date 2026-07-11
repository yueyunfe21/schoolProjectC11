package com.bot.dhxy.ui;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import com.bot.dhxy.auth.LicenseAuthResult;
import com.bot.dhxy.auth.LicenseAuthService;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.metrics.AutomationMetricsService;
import com.bot.dhxy.model.maintenance.CommonBoxRole;
import com.bot.dhxy.service.CommonBoxService;
import com.bot.dhxy.vision.MapSurveyService;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.update.AppVersionService;
import com.bot.dhxy.window.control.WindowRegistrationBatchBuilder;
import com.bot.dhxy.window.control.WindowSystemSnapshot;
import com.bot.dhxy.window.control.WindowTaskCommandDetail;
import com.bot.dhxy.window.control.WindowTaskCommandResult;
import com.bot.dhxy.window.control.WindowTaskControlService;
import com.bot.dhxy.window.control.WindowTaskStartRequest;
import com.bot.dhxy.window.discovery.GameWindowRegistrationService;
import com.bot.dhxy.window.diagnostics.WindowInteractionMetricsService;
import com.bot.dhxy.window.execution.WindowTaskFailurePolicy;
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
import javafx.scene.control.TableView;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MainWindowController {

    private static final DateTimeFormatter UI_LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter UI_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private static final Pattern WINDOW_IDENTITY_PATTERN = Pattern.compile("-\\s+(.+?)\\s+-\\s+(.+?)\\s*[\\(（]ID[:：]\\s*(\\d+)[\\)）]");
    private static final int MAX_WINDOW_COMMAND_LOGS = 120;
    private static final List<String> SELECTED_WINDOW_DETAIL_KEYS = List.of(
            "窗口", "角色", "状态", "绑定", "当前", "上次执行", "最近任务", "结束时间", "消息", "标题");

    private final WindowTaskControlService windowTaskControlService;
    private final WindowRegistrationBatchBuilder windowRegistrationBatchBuilder;
    private final GameWindowRegistrationService gameWindowRegistrationService;
    private final WindowInteractionMetricsService windowInteractionMetricsService;
    private final AutomationMetricsService automationMetricsService;
    private final LicenseAuthService licenseAuthService;
    private final BotProperties botProperties;
    private final CommonBoxService commonBoxService;
    private final GameUiSettingsStore gameUiSettingsStore;
    private final CloudDecisionDevSidecarService cloudDecisionDevSidecarService;
    private final MapSurveyService mapSurveyService;
    private final CoordinateHelper coordinateHelper;
    private final AppVersionService appVersionService;
    private final WindowSelectionMemory windowSelectionMemory = new WindowSelectionMemory();

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
    private Button interactionMetricsDashboardButton;
    private Button automationMetricsDashboardButton;
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
    private ToggleButton darkModeCheckBox;
    private CheckBox playerHpSupplyCheckBox;
    private CheckBox playerMpSupplyCheckBox;
    private CheckBox petHpSupplyCheckBox;
    private CheckBox petMpSupplyCheckBox;
    private ComboBox<Integer> playerHpThresholdComboBox;
    private ComboBox<Integer> playerMpThresholdComboBox;
    private ComboBox<Integer> petHpThresholdComboBox;
    private ComboBox<Integer> petMpThresholdComboBox;
    private TextField xiuluoRunCountField;
    private ComboBox<Integer> wuhuanRunCountComboBox;
    private TextField fivefoldRunCountField;
    private CheckBox summonSkillCleanEnabledCheckBox;
    private CheckBox taskStartupPreparationEnabledCheckBox;
    private CheckBox xiuluoMaintenanceRunImmediatelyCheckBox;
    private CheckBox xiuluoHealPetMaintenanceEnabledCheckBox;
    private CheckBox xiuluoRepairEquipmentMaintenanceEnabledCheckBox;
    private CheckBox leaderCommonBoxEnabledCheckBox;
    private CheckBox memberCommonBoxEnabledCheckBox;
    private ComboBox<Integer> summonSkillIntervalMinutesComboBox;
    private ComboBox<Integer> xiuluoHealPetIntervalMinutesComboBox;
    private ComboBox<Integer> xiuluoRepairEquipmentIntervalMinutesComboBox;
    private Button applySettingsButton;
    private Button clearButton;
    private Button taskCountShortcutButton;
    private Label settingsEditLockLabel;
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
    private TextField mapSurveyMapNameField;
    private Label mapSurveyHintLabel;
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
    private final Map<String, Label> selectedWindowDetailValueLabels = new LinkedHashMap<>();
    private List<String> pendingAutoSelectedWindowIds = List.of();
    private TaskType activeTaskCountType;
    private Timeline taskCountHoldTimeline;
    private Timeline applySettingsFeedbackTimeline;
    private boolean taskCountHoldRepeated;
    private boolean windowCommandRunning;
    private boolean selectedWindowDetailExpanded;
    private boolean restoringWindowTableSelection;

    private Map<TaskType, String> createDefaultTaskCountSummaries() {
        Map<TaskType, String> summaries = new EnumMap<>(TaskType.class);
        summaries.put(TaskType.WUHuan_V2, "1轮");
        summaries.put(TaskType.WUBEI, "1次");
        summaries.put(TaskType.XIULUO_V2, "1次");
        summaries.put(TaskType.AUTO_BATTLE, "无限");
        summaries.put(TaskType.SLEEP_COMPUTER, "收尾");
        return summaries;
    }

    public Parent buildView() {
        gameUiSettingsStore.loadInto(botProperties);
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
        windowTaskTypeComboBox.getItems().setAll(selectableTaskTypes());
        windowTaskTypeComboBox.setValue(TaskType.WUHuan_V2);

        queueTaskTypeComboBox = new ComboBox<>();
        queueTaskTypeComboBox.getItems().setAll(selectableTaskTypes());
        queueTaskTypeComboBox.setValue(TaskType.WUHuan_V2);

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
        interactionMetricsDashboardButton = new Button("统计 Dashboard");
        automationMetricsDashboardButton = new Button("业务 Dashboard");
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
        pauseSelectedWindowsButton = new Button("暂停选中窗口");
        resumeSelectedWindowsButton = new Button("继续选中窗口");
        pauseAllWindowsButton = new Button("暂停全部窗口");
        resumeAllWindowsButton = new Button("继续全部窗口");
        stopSelectedWindowsButton = new Button("停止选中窗口");
        stopAllWindowsButton = new Button("停止全部窗口");
        unregisterSelectedWindowsButton = new Button("移除选中窗口");
        unregisterAllWindowsButton = new Button("移除全部窗口");
        refreshWindowButton = new Button("刷新");
        FontIcon refreshIcon = new FontIcon("fas-sync-alt");
        refreshIcon.setIconSize(13);
        refreshWindowButton.setGraphic(refreshIcon);
        refreshWindowButton.setContentDisplay(ContentDisplay.LEFT);
        refreshWindowButton.setGraphicTextGap(6);
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
        darkModeCheckBox = new ToggleButton("深色模式");
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
        xiuluoRunCountField = buildTaskRunCountField(botProperties.getXiuluoMaxRuns());
        wuhuanRunCountComboBox = buildWuhuanRunCountComboBox(botProperties.getWuhuanMaxRuns());
        fivefoldRunCountField = buildTaskRunCountField(botProperties.getFivefoldMaxRuns());
        syncTaskCountSummariesFromProperties();
        summonSkillCleanEnabledCheckBox = new CheckBox("删除技能");
        summonSkillCleanEnabledCheckBox.setSelected(botProperties.isSummonSkillCleanEnabled());
        taskStartupPreparationEnabledCheckBox = new CheckBox("任务启动前置检查");
        taskStartupPreparationEnabledCheckBox.setSelected(botProperties.isTaskStartupPreparationEnabled());
        xiuluoMaintenanceRunImmediatelyCheckBox = new CheckBox("修罗启动维护");
        xiuluoMaintenanceRunImmediatelyCheckBox.setSelected(botProperties.isXiuluoMaintenanceRunImmediatelyOnStart());
        summonSkillIntervalMinutesComboBox = buildSummonSkillIntervalComboBox(botProperties.getSummonSkillCleanIntervalMs());
        xiuluoHealPetMaintenanceEnabledCheckBox = new CheckBox("启用巫医");
        xiuluoHealPetMaintenanceEnabledCheckBox.setSelected(botProperties.getXiuluoHealPetMaintenanceIntervalMs() > 0);
        xiuluoRepairEquipmentMaintenanceEnabledCheckBox = new CheckBox("启用修理");
        xiuluoRepairEquipmentMaintenanceEnabledCheckBox.setSelected(botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs() > 0);
        leaderCommonBoxEnabledCheckBox = new CheckBox("队长要盒子");
        leaderCommonBoxEnabledCheckBox.setSelected(botProperties.isLeaderCommonBoxEnabled());
        memberCommonBoxEnabledCheckBox = new CheckBox("队员要盒子");
        memberCommonBoxEnabledCheckBox.setSelected(botProperties.isMemberCommonBoxEnabled());
        xiuluoHealPetIntervalMinutesComboBox = buildMaintenanceIntervalComboBox(botProperties.getXiuluoHealPetMaintenanceIntervalMs());
        xiuluoRepairEquipmentIntervalMinutesComboBox = buildMaintenanceIntervalComboBox(botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs());
        bindMaintenanceIntervalToggle(xiuluoHealPetMaintenanceEnabledCheckBox, xiuluoHealPetIntervalMinutesComboBox);
        bindMaintenanceIntervalToggle(xiuluoRepairEquipmentMaintenanceEnabledCheckBox, xiuluoRepairEquipmentIntervalMinutesComboBox);
        applySettingsButton = new Button("应用设置");
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
        mapSurveyMapNameField = new TextField(botProperties.getMapSurveyMapName());
        mapSurveyMapNameField.setPromptText("地图名，例如：瑶池");
        mapSurveyMapNameField.setPrefWidth(180);
        mapSurveyHintLabel = new Label("地图测绘：同一个地图名会用于小地图名字样本和镜头边界；记录边界时先点按钮，然后3秒内把鼠标移到角色身上/脚下。");
        mapSurveyHintLabel.setWrapText(true);
        mapSurveyHintLabel.getStyleClass().add("queue-summary");
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

    /*
     * 巫医和修理是全局任务维护开关，任务侧用 intervalMs <= 0 表示禁用。它们不要复用
     * 三技能的短间隔选项，避免用户误以为几分钟级别也适合跑 NPC 维护。
     */
    private ComboBox<Integer> buildMaintenanceIntervalComboBox(long intervalMs) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(30, 60, 120, 240);
        comboBox.setValue(normalizeMaintenanceIntervalMinutes(intervalMs));
        comboBox.setPrefWidth(86);
        return comboBox;
    }

    private void bindMaintenanceIntervalToggle(CheckBox checkBox, ComboBox<Integer> comboBox) {
        comboBox.setDisable(!checkBox.isSelected());
        checkBox.selectedProperty().addListener((observable, oldValue, selected) -> comboBox.setDisable(!selected));
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
                petMpThresholdComboBox);
        return buildSection("补给配置", playerRow, petRow);
    }

    private void applySettingsFromUi() {
        if (isSettingsEditLocked()) {
            rejectSettingsEditWhileBusy("设置");
            return;
        }
        int xiuluoRuns = readRunCountField(xiuluoRunCountField);
        int wuhuanRuns = normalizeWuhuanRunCount(wuhuanRunCountComboBox.getValue());
        int fivefoldRuns = readRunCountField(fivefoldRunCountField);
        botProperties.setXiuluoMaxRuns(xiuluoRuns);
        botProperties.setWuhuanMaxRuns(wuhuanRuns);
        botProperties.setFivefoldMaxRuns(fivefoldRuns);
        botProperties.setSummonSkillCleanEnabled(summonSkillCleanEnabledCheckBox.isSelected());
        botProperties.setTaskStartupPreparationEnabled(taskStartupPreparationEnabledCheckBox.isSelected());
        botProperties.setXiuluoMaintenanceRunImmediatelyOnStart(xiuluoMaintenanceRunImmediatelyCheckBox.isSelected());
        botProperties.setSummonSkillCleanIntervalMs(normalizeSummonSkillIntervalMinutes(
                summonSkillIntervalMinutesComboBox.getValue()) * 60_000L);
        botProperties.setXiuluoHealPetMaintenanceIntervalMs(xiuluoHealPetMaintenanceEnabledCheckBox.isSelected()
                ? normalizeMaintenanceIntervalMinutes(xiuluoHealPetIntervalMinutesComboBox.getValue()) * 60_000L
                : 0L);
        botProperties.setXiuluoRepairEquipmentMaintenanceIntervalMs(xiuluoRepairEquipmentMaintenanceEnabledCheckBox.isSelected()
                ? normalizeMaintenanceIntervalMinutes(xiuluoRepairEquipmentIntervalMinutesComboBox.getValue()) * 60_000L
                : 0L);
        botProperties.setPlayerHpSupplyEnabled(playerHpSupplyCheckBox.isSelected());
        botProperties.setPlayerHpSupplyThreshold(normalizeSupplyThreshold(playerHpThresholdComboBox.getValue()));
        botProperties.setPlayerMpSupplyEnabled(playerMpSupplyCheckBox.isSelected());
        botProperties.setPlayerMpSupplyThreshold(normalizeSupplyThreshold(playerMpThresholdComboBox.getValue()));
        botProperties.setPetHpSupplyEnabled(petHpSupplyCheckBox.isSelected());
        botProperties.setPetHpSupplyThreshold(normalizeSupplyThreshold(petHpThresholdComboBox.getValue()));
        botProperties.setPetMpSupplyEnabled(petMpSupplyCheckBox.isSelected());
        botProperties.setPetMpSupplyThreshold(normalizeSupplyThreshold(petMpThresholdComboBox.getValue()));
        boolean previousLeaderBoxEnabled = botProperties.isLeaderCommonBoxEnabled();
        boolean previousMemberBoxEnabled = botProperties.isMemberCommonBoxEnabled();
        botProperties.setLeaderCommonBoxEnabled(leaderCommonBoxEnabledCheckBox.isSelected());
        botProperties.setMemberCommonBoxEnabled(memberCommonBoxEnabledCheckBox.isSelected());
        if (previousLeaderBoxEnabled && !botProperties.isLeaderCommonBoxEnabled()) {
            commonBoxService.clearPendingForRole(CommonBoxRole.LEADER, "ui:leader-common-box-off");
        }
        if (previousMemberBoxEnabled && !botProperties.isMemberCommonBoxEnabled()) {
            commonBoxService.clearPendingForRole(CommonBoxRole.MEMBER, "ui:member-common-box-off");
        }
        syncTaskCountSummariesFromProperties();
        gameUiSettingsStore.save(botProperties);
        showApplySettingsFeedback();

        addWindowLog("设置已应用：修罗=" + botProperties.getXiuluoMaxRuns()
                + " 五环=" + botProperties.getWuhuanMaxRuns()
                + " 五倍=" + botProperties.getFivefoldMaxRuns()
                + " 前置检查=" + (botProperties.isTaskStartupPreparationEnabled() ? "开" : "关")
                + " 修罗启动维护=" + (botProperties.isXiuluoMaintenanceRunImmediatelyOnStart() ? "开" : "关")
                + " 三技能=" + (botProperties.isSummonSkillCleanEnabled() ? "开" : "关")
                + "/" + normalizeSummonSkillIntervalMinutes(botProperties.getSummonSkillCleanIntervalMs()) + "分钟"
                + " 巫医=" + formatMaintenanceIntervalText(botProperties.getXiuluoHealPetMaintenanceIntervalMs())
                + " 修理=" + formatMaintenanceIntervalText(botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs())
                + " 队长盒子=" + (botProperties.isLeaderCommonBoxEnabled() ? "开" : "关")
                + " 队员盒子=" + (botProperties.isMemberCommonBoxEnabled() ? "开" : "关")
                + " 人物血=" + supplyText(botProperties.isPlayerHpSupplyEnabled(), botProperties.getPlayerHpSupplyThreshold())
                + " 人物法=" + supplyText(botProperties.isPlayerMpSupplyEnabled(), botProperties.getPlayerMpSupplyThreshold())
                + " 召唤兽血=" + supplyText(botProperties.isPetHpSupplyEnabled(), botProperties.getPetHpSupplyThreshold())
                + " 召唤兽法=" + supplyText(botProperties.isPetMpSupplyEnabled(), botProperties.getPetMpSupplyThreshold()));
        renderLogList();
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
        taskCountSummaries.put(TaskType.XIULUO_V2, formatTaskCountSummary(botProperties.getXiuluoMaxRuns(), "次"));
        taskCountSummaries.put(TaskType.WUBEI, formatTaskCountSummary(botProperties.getFivefoldMaxRuns(), "次"));
        taskCountSummaries.put(TaskType.WUHuan_V2, formatTaskCountSummary(botProperties.getWuhuanMaxRuns(), "轮"));
        taskCountSummaries.put(TaskType.AUTO_BATTLE, "无限");
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
        gameUiSettingsStore.save(botProperties);
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
            case XIULUO, XIULUO_V2 -> {
                botProperties.setXiuluoMaxRuns(normalized);
                if (xiuluoRunCountField != null) {
                    xiuluoRunCountField.setText(String.valueOf(normalized));
                }
                taskCountSummaries.put(TaskType.XIULUO, formatTaskCountSummary(normalized, "次"));
                taskCountSummaries.put(TaskType.XIULUO_V2, formatTaskCountSummary(normalized, "次"));
            }
            case WUHuan_V2 -> {
                int wuhuanRuns = normalizeWuhuanRunCount(normalized);
                botProperties.setWuhuanMaxRuns(wuhuanRuns);
                if (wuhuanRunCountComboBox != null) {
                    wuhuanRunCountComboBox.setValue(wuhuanRuns);
                }
                taskCountSummaries.put(TaskType.WUHuan_V2, formatTaskCountSummary(wuhuanRuns, "轮"));
            }
            case WUBEI -> {
                botProperties.setFivefoldMaxRuns(normalized);
                if (fivefoldRunCountField != null) {
                    fivefoldRunCountField.setText(String.valueOf(normalized));
                }
                taskCountSummaries.put(TaskType.WUBEI, formatTaskCountSummary(normalized, "次"));
            }
            case AUTO_BATTLE, UNKNOWN -> {
                // These badges are labels/durations, not max-run task limits.
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

    private int normalizeMaintenanceIntervalMinutes(Long intervalMs) {
        if (intervalMs == null || intervalMs <= 0) {
            return 30;
        }
        return normalizeMaintenanceIntervalMinutes((int) Math.round(intervalMs / 60_000.0));
    }

    private int normalizeMaintenanceIntervalMinutes(Integer minutes) {
        if (minutes == null || minutes <= 45) {
            return 30;
        }
        if (minutes <= 90) {
            return 60;
        }
        if (minutes <= 180) {
            return 120;
        }
        return 240;
    }

    private String formatMaintenanceIntervalText(long intervalMs) {
        if (intervalMs <= 0) {
            return "关";
        }
        return normalizeMaintenanceIntervalMinutes(intervalMs) + "分钟";
    }

    private void configureVisualStyles() {
        addStyleClass(clearButton, "secondary-button");
        addStyleClass(clearWindowSelectionButton, "secondary-button");
        addStyleClass(clearWindowSelectionButton, "toolbar-action-button");
        addStyleClass(registerWindowButton, "primary-button");
        addStyleClass(scanGameWindowsButton, "primary-button");
        addStyleClass(startIndependentWindowsButton, "primary-button");
        addStyleClass(startCurrentTaskButton, "primary-button");
        addStyleClass(startCurrentTaskButton, "start-action");
        addStyleClass(startCurrentTaskButton, "toolbar-action-button");
        addStyleClass(startWindowSelectedTaskButton, "primary-button");
        addStyleClass(startQueueButton, "primary-button");
        addStyleClass(pauseSelectedWindowsButton, "secondary-button");
        addStyleClass(resumeSelectedWindowsButton, "secondary-button");
        addStyleClass(pauseAllWindowsButton, "secondary-button");
        addStyleClass(resumeAllWindowsButton, "secondary-button");
        addStyleClass(stopSelectedWindowsButton, "danger-button");
        addStyleClass(stopSelectedWindowsButton, "bulk-danger-button");
        addStyleClass(stopAllWindowsButton, "danger-button");
        addStyleClass(stopAllWindowsButton, "bulk-danger-button");
        addStyleClass(selectAllWindowsButton, "toolbar-action-button");
        addStyleClass(unregisterSelectedWindowsButton, "danger-button");
        addStyleClass(unregisterAllWindowsButton, "danger-button");
        addStyleClass(applySettingsButton, "primary-button");
        addStyleClass(applySelectedTaskButton, "secondary-button");
        addStyleClass(refreshWindowButton, "secondary-button");
        addStyleClass(refreshWindowButton, "refresh-window-button");
        addStyleClass(windowSelectionMenuButton, "secondary-button");
        addStyleClass(windowManageMenuButton, "secondary-button");
        addStyleClass(runControlMenuButton, "secondary-button");
        addStyleClass(registerTeamButton, "secondary-button");
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
        Label pauseHotkeyLabel = new Label("暂停 Ctrl+Shift+F11");
        pauseHotkeyLabel.getStyleClass().addAll("hotkey-badge", "hotkey-pause");
        Label emergencyStopLabel = new Label("紧急停止 Ctrl+Shift+F12");
        emergencyStopLabel.getStyleClass().addAll("hotkey-badge", "hotkey-stop");
        FontIcon moonIcon = new FontIcon("fas-moon");
        moonIcon.setIconSize(14);
        darkModeCheckBox.setGraphic(moonIcon);
        darkModeCheckBox.setContentDisplay(ContentDisplay.LEFT);
        darkModeCheckBox.setGraphicTextGap(8);
        darkModeCheckBox.setOnAction(event -> applyThemeMode());

        clearButton.setOnAction(event -> {
            clearWindowLogs();
            refreshDashboard();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, title, pauseHotkeyLabel, emergencyStopLabel, spacer, darkModeCheckBox);
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
        boolean dark = darkModeCheckBox != null && darkModeCheckBox.isSelected();
        if (dark) {
            rootPane.getStyleClass().add("theme-dark");
        }
        WindowsTitleBarTheme.applyToWindowTitle("DHXY Robot 控制台", dark);
    }

    private Parent buildMainShell() {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");

        StackPane content = new StackPane();
        content.getStyleClass().add("side-content");

        ToggleGroup group = new ToggleGroup();
        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("side-nav");

        ToggleButton mainButton = buildSideNavButton("主控", "fas-home", group, buildWindowPanel(), content);
        VBox navButtons = new VBox(6,
                mainButton,
                buildSideNavButton("设置", "fas-cog", group, buildSettingsPanel(), content),
                buildSideNavButton("验证", "fas-shield-alt", group, buildAuthenticationPanel(), content),
                buildSideNavButton("调试", "fas-bug", group, buildDiagnosticsPanel(), content),
                buildSideNavButton("日志", "fas-list-alt", group, buildLogPanel(), content),
                buildSideNavButton("说明", "fas-info-circle", group, buildAboutPanel(), content));
        navButtons.getStyleClass().add("side-nav-buttons");

        Label versionLabel = new Label("v" + appVersionService.currentVersion());
        versionLabel.getStyleClass().add("side-version-label");
        VBox sideFooter = new VBox(6, versionLabel);
        sideFooter.getStyleClass().add("side-footer");
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(
                navButtons,
                spacer,
                sideFooter
        );

        mainButton.setSelected(true);
        content.getChildren().setAll((Parent) mainButton.getUserData());
        shell.setLeft(sidebar);
        shell.setCenter(content);
        return shell;
    }

    private ToggleButton buildSideNavButton(
            String title,
            String iconLiteral,
            ToggleGroup group,
            Parent content,
            StackPane contentPane) {
        ToggleButton button = new ToggleButton(title);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(15);
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(10);
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
        presetFiveRingQueueButton.setOnAction(event -> setPendingTaskQueue(TaskType.WUHuan_V2));
        presetAutoBattleQueueButton.setOnAction(event -> setPendingTaskQueue(TaskType.AUTO_BATTLE));
        presetFiveRingAutoBattleQueueButton.setOnAction(event -> setPendingTaskQueue(TaskType.WUHuan_V2, TaskType.AUTO_BATTLE));
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
            handleMainStartPauseButton();
        });
        startWindowSelectedTaskButton.setOnAction(event -> {
            List<String> windowIds = getSelectedWindowIds();
            warnUnavailableSelectedWindows("启动已选任务");
            runWindowCommandInBackground(() -> {
                List<String> acceptingWindowIds = filterAcceptingWindowIds(windowIds);
                if (acceptingWindowIds.isEmpty()) {
                    return WindowTaskCommandResult.empty("选中的窗口当前不可接任务，未启动任务",
                            windowTaskControlService.getSnapshots());
                }
                WindowTaskCommandResult cloudGate = ensureCloudDecisionDevReadyForTaskStart();
                if (cloudGate != null) {
                    return cloudGate;
                }
                return windowTaskControlService.start(WindowTaskStartRequest.selectedTask(acceptingWindowIds));
            });
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
        applySettingsButton.setOnAction(event -> applySettingsFromUi());

        HBox leftTools = new HBox(8, refreshWindowButton, windowFilterComboBox, windowSearchField);
        leftTools.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(windowSearchField, Priority.NEVER);

        selectAllWindowsButton.setText("全选");
        clearWindowSelectionButton.setText("取消选择");
        pauseSelectedWindowsButton.setText("暂停");
        stopSelectedWindowsButton.setText("停止所选");
        stopAllWindowsButton.setText("停止全部");
        refreshWindowButton.setMinWidth(58);
        windowFilterComboBox.setMinWidth(92);
        windowSearchField.setPrefWidth(160);
        windowSearchField.setMinWidth(132);
        pauseSelectedWindowsButton.setMinWidth(62);
        stopSelectedWindowsButton.setMinWidth(76);
        stopAllWindowsButton.setMinWidth(76);
        clearWindowSelectionButton.setMinWidth(74);
        selectAllWindowsButton.setMinWidth(62);
        startCurrentTaskButton.setMinWidth(86);
        HBox rightTools = new HBox(6,
                stopAllWindowsButton,
                clearWindowSelectionButton,
                selectAllWindowsButton,
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
        settingsEditLockLabel = new Label("设置只允许在所有任务停止后修改。");
        settingsEditLockLabel.getStyleClass().add("queue-summary");
        VBox wrapper = new VBox(8,
                settingsEditLockLabel,
                buildSettingsActionPanel(),
                buildTaskRunConfigPanel(),
                buildSummonSkillConfigPanel(),
                buildMaintenanceConfigPanel(),
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
        return buildSection("任务次数", firstRow);
    }

    private Parent buildSettingsActionPanel() {
        FlowPane actionRow = buildControlRow(applySettingsButton);
        return buildSection("应用设置", actionRow);
    }

    private void showApplySettingsFeedback() {
        if (applySettingsButton == null) {
            return;
        }
        if (applySettingsFeedbackTimeline != null) {
            applySettingsFeedbackTimeline.stop();
        }
        applySettingsButton.setText("已应用");
        addStyleClass(applySettingsButton, "settings-applied");
        if (settingsEditLockLabel != null) {
            settingsEditLockLabel.setText("设置已保存：新任务会使用刚应用的配置。");
        }
        applySettingsFeedbackTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            applySettingsButton.setText("应用设置");
            removeStyleClass(applySettingsButton, "settings-applied");
            refreshSettingsEditLock();
        }));
        applySettingsFeedbackTimeline.play();
    }

    private Parent buildSummonSkillConfigPanel() {
        FlowPane summonRow = buildControlRow(
                summonSkillCleanEnabledCheckBox,
                new Label("时间间隔"),
                summonSkillIntervalMinutesComboBox,
                new Label("分钟"));
        return buildSection("召唤兽技能", summonRow);
    }

    private Parent buildMaintenanceConfigPanel() {
        FlowPane maintenanceRow = buildControlRow(
                xiuluoHealPetMaintenanceEnabledCheckBox,
                new Label("巫医间隔"),
                xiuluoHealPetIntervalMinutesComboBox,
                new Label("分钟"),
                xiuluoRepairEquipmentMaintenanceEnabledCheckBox,
                new Label("修理间隔"),
                xiuluoRepairEquipmentIntervalMinutesComboBox,
                new Label("分钟"),
                leaderCommonBoxEnabledCheckBox,
                memberCommonBoxEnabledCheckBox);
        return buildSection("任务维护", maintenanceRow);
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
        interactionMetricsDashboardButton.setOnAction(event -> openInteractionMetricsDashboard());
        automationMetricsDashboardButton.setOnAction(event -> openAutomationMetricsDashboard());
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

        FlowPane dashboardRow = buildControlRow(
                interactionMetricsDashboardButton,
                automationMetricsDashboardButton,
                new Label("打开本地输入/业务统计页面"));
        FlowPane mapSurveyNameRow = buildControlRow(
                new Label("地图名"),
                mapSurveyMapNameField,
                new Label("测绘按钮读取这里"));
        FlowPane mapSurveySampleRow = buildControlRow(
                saveMapLabelSampleButton,
                testMapLabelSampleButton,
                new Label("保存/测试小地图左上角地图名模板"));
        FlowPane mapSurveyBoundaryRow = buildControlRow(
                recordCameraLeftButton,
                recordCameraRightButton,
                recordCameraTopButton,
                recordCameraBottomButton,
                recordCameraCenterButton);
        FlowPane mapSurveyPlayerPointRow = buildControlRow(
                testProjectedPlayerPointButton,
                recordPlayerPointCorrectionButton,
                testCorrectedPlayerPointButton,
                undoPlayerPointCorrectionButton);
        FlowPane logFileRow = buildControlRow(
                clearButton,
                new Label("主日志：logs/dhxy-console.log"),
                new Label("坐标/窗口诊断：logs/tracker-coordinate.log"));
        VBox wrapper = new VBox(8,
                buildSection("统计面板", dashboardRow),
                buildSection("地图测绘",
                        mapSurveyNameRow,
                        mapSurveySampleRow,
                        mapSurveyBoundaryRow,
                        mapSurveyPlayerPointRow,
                        mapSurveyHintLabel),
                buildSection("日志文件", logFileRow));
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
                        new Label("窗口详情从表格右侧浮出；主页面不再用整页滑动承载所有控件。"),
                        new Label("当前版本：" + appVersionService.currentVersion())));
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
        TableColumn<WindowTaskSnapshot, WindowTaskSnapshot> selectionCol = new TableColumn<>("");
        selectionCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        selectionCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final StackPane checkBoxHitArea = new StackPane(checkBox);

            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setPickOnBounds(true);
                getStyleClass().add("window-select-cell");
                checkBox.setFocusTraversable(false);
                checkBox.setMouseTransparent(true);
                checkBox.getStyleClass().add("window-select-check");
                checkBox.setMinSize(18, 18);
                checkBox.setPrefSize(18, 18);
                checkBox.setMaxSize(18, 18);
                checkBoxHitArea.setAlignment(Pos.CENTER);
                checkBoxHitArea.setPickOnBounds(true);
                checkBoxHitArea.setMinSize(34, 28);
                checkBoxHitArea.setPrefSize(34, 28);
                checkBoxHitArea.setMaxSize(34, 28);
                addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    if (event.getButton() != MouseButton.PRIMARY || getItem() == null || isEmpty()
                            || event.getClickCount() < 1) {
                        return;
                    }
                    setRowSelectedFromCell(!checkBox.isSelected());
                    event.consume();
                });
            }

            private void setRowSelectedFromCell(boolean selected) {
                WindowTaskSnapshot snapshot = getItem();
                if (snapshot == null || getIndex() < 0) {
                    return;
                }
                checkBox.setSelected(selected);
                if (selected) {
                    windowSelectionMemory.select(snapshot.getWindowId());
                    windowTable.getSelectionModel().select(getIndex());
                } else {
                    windowSelectionMemory.deselect(snapshot.getWindowId());
                    windowTable.getSelectionModel().clearSelection(getIndex());
                }
                refreshSelectionDependentUi();
            }

            @Override
            protected void updateItem(WindowTaskSnapshot snapshot, boolean empty) {
                super.updateItem(snapshot, empty);
                setText(null);
                if (empty || snapshot == null) {
                    setGraphic(null);
                    return;
                }
                checkBox.setSelected(windowSelectionMemory.isSelected(snapshot.getWindowId()));
                setGraphic(checkBoxHitArea);
            }
        });
        selectionCol.setPrefWidth(50);
        selectionCol.setMinWidth(50);
        selectionCol.setMaxWidth(54);
        selectionCol.setSortable(false);
        selectionCol.setResizable(false);

        TableColumn<WindowTaskSnapshot, String> roleNameCol = new TableColumn<>("角色名");
        roleNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(parseWindowIdentity(cell.getValue()).roleName()));
        roleNameCol.setPrefWidth(102);

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
        baseCol.setMaxWidth(92);

        TableColumn<WindowTaskSnapshot, String> serverCol = new TableColumn<>("服务器");
        serverCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(parseWindowIdentity(cell.getValue()).serverName()));
        serverCol.setPrefWidth(84);

        TableColumn<WindowTaskSnapshot, String> playerIdCol = new TableColumn<>("ID");
        playerIdCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(parseWindowIdentity(cell.getValue()).playerId()));
        playerIdCol.setPrefWidth(92);

        TableColumn<WindowTaskSnapshot, WindowTaskSnapshot> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            private static final List<String> STATUS_STYLE_CLASSES = List.of(
                    "status-idle", "status-queued", "status-running", "status-paused",
                    "status-stopping", "status-stopped", "status-error", "status-completed");
            private final Label statusBadge = new Label();

            {
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                getStyleClass().add("status-cell");
                statusBadge.getStyleClass().add("status-pill");
            }

            @Override
            protected void updateItem(WindowTaskSnapshot snapshot, boolean empty) {
                super.updateItem(snapshot, empty);
                setText(null);
                statusBadge.getStyleClass().removeAll(STATUS_STYLE_CLASSES);
                if (empty || snapshot == null) {
                    setGraphic(null);
                    return;
                }
                statusBadge.setText(statusDisplayName(snapshot));
                statusBadge.getStyleClass().add(statusBadgeStyleClass(snapshot));
                setGraphic(statusBadge);
            }
        });
        statusCol.setPrefWidth(86);

        TableColumn<WindowTaskSnapshot, String> runningTaskCol = new TableColumn<>("运行任务");
        runningTaskCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(runningTaskDisplayName(cell.getValue())));
        runningTaskCol.setPrefWidth(86);

        TableColumn<WindowTaskSnapshot, String> queueProgressCol = new TableColumn<>("进度");
        queueProgressCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRunningTaskProgressText()));
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
        actionsCol.setPrefWidth(58);

        windowTable.getColumns().setAll(List.of(selectionCol, roleNameCol, serverCol, playerIdCol,
                statusCol, runningTaskCol, queueProgressCol, actionsCol));
        windowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private String statusDisplayName(WindowTaskSnapshot snapshot) {
        if (isTaskProgressComplete(snapshot)) {
            return "已完成";
        }
        return snapshot == null ? WindowRuntimeStatus.IDLE.getDisplayName() : snapshot.getStatusDisplayName();
    }

    private String runningTaskDisplayName(WindowTaskSnapshot snapshot) {
        if (isTaskProgressComplete(snapshot)) {
            return "无";
        }
        return snapshot == null ? "-" : snapshot.getRunningTaskDisplayName();
    }

    private String statusBadgeStyleClass(WindowTaskSnapshot snapshot) {
        if (isTaskProgressComplete(snapshot)) {
            return "status-completed";
        }
        WindowRuntimeStatus status = snapshot == null ? WindowRuntimeStatus.IDLE : snapshot.getStatus();
        return switch (status) {
            case IDLE -> "status-idle";
            case QUEUED -> "status-queued";
            case RUNNING -> "status-running";
            case PAUSED -> "status-paused";
            case STOPPING -> "status-stopping";
            case STOPPED -> "status-stopped";
            case ERROR -> "status-error";
        };
    }

    private boolean isTaskProgressComplete(WindowTaskSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.getStatus() == WindowRuntimeStatus.ERROR || snapshot.getStatus() == WindowRuntimeStatus.STOPPED) {
            return false;
        }
        String progress = snapshot.getRunningTaskProgressText();
        if (progress == null) {
            return false;
        }
        String[] parts = progress.trim().split("/");
        if (parts.length != 2) {
            return false;
        }
        try {
            int completed = Integer.parseInt(parts[0].trim());
            int total = Integer.parseInt(parts[1].trim());
            return total > 0 && completed >= total;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("row-actions");
        if (snapshot.getStatus() == WindowRuntimeStatus.PAUSED) {
            actions.getChildren().add(rowActionButton("fas-play", "继续该窗口任务", "row-icon-button",
                    () -> runWindowCommandInBackground(() -> windowTaskControlService.resumeWindows(List.of(snapshot.getWindowId())))));
        } else if (snapshot.isRunning()) {
            actions.getChildren().add(rowActionButton("fas-pause", "暂停该窗口任务", "row-pause-button",
                    () -> runWindowCommandInBackground(() -> windowTaskControlService.pauseWindows(List.of(snapshot.getWindowId())))));
        } else if (isProblemWindow(snapshot)) {
            actions.getChildren().add(rowActionButton("fas-play", "启动该窗口任务", "row-icon-button",
                    () -> startWindows(List.of(snapshot.getWindowId()), "启动")));
        } else {
            actions.getChildren().add(rowActionButton("fas-play", "启动该窗口任务", "row-icon-button",
                    () -> startWindows(List.of(snapshot.getWindowId()), "启动")));
        }
        actions.getChildren().add(rowActionButton("fas-stop", "停止该窗口任务", "row-stop-button",
                () -> runWindowCommandInBackground(() -> windowTaskControlService.stopWindows(List.of(snapshot.getWindowId())))));
        return actions;
    }

    private Button rowActionButton(String iconLiteral, String tooltipText, String styleClass, Runnable action) {
        Button button = new Button();
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(14);
        button.setGraphic(icon);
        button.setAccessibleText(tooltipText);
        button.setTooltip(new Tooltip(tooltipText));
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
        Button clearTasksButton = new Button("清空任务选择");
        clearTasksButton.getStyleClass().add("secondary-button");
        clearTasksButton.setOnAction(event -> clearPendingTaskQueue());

        taskCountShortcutButton = new Button("次数");
        taskCountShortcutButton.getStyleClass().add("secondary-button");
        taskCountShortcutButton.setOnAction(event -> openLatestTaskCountEditor());

        HBox titleActions = new HBox(8, taskSelectionSummaryLabel, taskCountShortcutButton);
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

        VBox panel = new VBox(8, titleRow, taskTiles, taskCountEditorBar, actions);
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
        Label nameLabel = new Label(taskTileDisplayName(taskType));
        nameLabel.getStyleClass().add("task-tile-name");
        nameLabel.setMinHeight(32);
        nameLabel.setPrefHeight(32);
        nameLabel.setMaxWidth(66);
        nameLabel.setWrapText(true);
        Label metaLabel = buildTaskMetaLabel(taskType);
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
        if (isSettingsEditLocked()) {
            rejectSettingsEditWhileBusy("任务次数");
            return;
        }
        if (taskType == null || taskType == TaskType.UNKNOWN || taskCountEditorBar == null) {
            return;
        }
        if (!isEditableTaskCount(taskType)) {
            addWindowLog(taskType.getDisplayName() + "不需要设置次数。");
            renderLogList();
            return;
        }
        if (taskType == activeTaskCountType && taskCountEditorBar.isVisible()) {
            taskCountEditorField.requestFocus();
            taskCountEditorField.selectAll();
            return;
        }
        activeTaskCountType = taskType;
        TaskCountDisplay display = parseTaskCountDisplay(taskCountSummaries.getOrDefault(taskType, "按需"));
        int value = normalizeInlineTaskCount(taskType, display.value());
        taskCountEditorTitleLabel.setText(taskType.getDisplayName());
        taskCountEditorField.setText(String.valueOf(value));
        taskCountEditorUnitLabel.setText(display.unit());
        taskCountEditorBar.getStyleClass().remove("task-count-editor-hidden");
        taskCountEditorBar.setVisible(true);
        taskCountEditorBar.setManaged(true);
    }

    private void openLatestTaskCountEditor() {
        TaskType taskType = pendingTaskQueue.isEmpty()
                ? TaskType.WUHuan_V2
                : pendingTaskQueue.get(pendingTaskQueue.size() - 1);
        openTaskCountInlineEditor(taskType);
    }

    private void stepTaskCount(int delta) {
        if (taskCountEditorField == null) {
            return;
        }
        int value = parsePositiveInt(taskCountEditorField.getText(), 1);
        taskCountEditorField.setText(String.valueOf(normalizeInlineTaskCount(activeTaskCountType, value + delta)));
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
        if (isSettingsEditLocked()) {
            rejectSettingsEditWhileBusy("任务次数");
            hideTaskCountEditor();
            return;
        }
        if (activeTaskCountType == null || taskCountEditorField == null || taskCountEditorUnitLabel == null) {
            hideTaskCountEditor();
            return;
        }
        int value = normalizeInlineTaskCount(activeTaskCountType, parsePositiveInt(taskCountEditorField.getText(), 1));
        String unit = taskCountEditorUnitLabel.getText() == null || taskCountEditorUnitLabel.getText().isBlank()
                ? "次"
                : taskCountEditorUnitLabel.getText().trim();
        taskCountSummaries.put(activeTaskCountType, value + unit);
        syncTaskRunCountToProperties(activeTaskCountType, value);
        gameUiSettingsStore.save(botProperties);
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

    private boolean isEditableTaskCount(TaskType taskType) {
        return switch (taskType) {
            case XIULUO, XIULUO_V2, WUHuan_V2, WUBEI -> true;
            case SLEEP_COMPUTER -> false;
            default -> false;
        };
    }

    private int normalizeInlineTaskCount(TaskType taskType, int value) {
        if (taskType == TaskType.WUHuan_V2) {
            return normalizeWuhuanRunCount(value);
        }
        return normalizeRunCount(value);
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

    private String taskMetaText(TaskType taskType) {
        return switch (taskType) {
            case SLEEP_COMPUTER -> "系统";
            default -> "任务";
        };
    }

    private String taskTileDisplayName(TaskType taskType) {
        return taskType == TaskType.AUTO_BATTLE ? "挂机" : taskType.getDisplayName();
    }

    private Label buildTaskMetaLabel(TaskType taskType) {
        Label label = new Label();
        label.getStyleClass().add("task-tile-meta");
        label.getStyleClass().add(taskMetaStyleClass(taskType));
        if (taskType == TaskType.WUBEI) {
            label.setText("5X");
            label.getStyleClass().add("task-tile-meta-text-icon");
            label.setAccessibleText("五倍");
            return label;
        }
        String iconLiteral = taskMetaIconLiteral(taskType);
        if (iconLiteral == null) {
            label.setText(taskMetaText(taskType));
            return label;
        }
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(16);
        label.setGraphic(icon);
        label.setAccessibleText(taskMetaAccessibleText(taskType));
        label.getStyleClass().add("task-tile-meta-icon");
        return label;
    }

    private String taskMetaIconLiteral(TaskType taskType) {
        return switch (taskType) {
            case WUHuan_V2 -> "fas-circle-notch";
            case XIULUO_V2 -> "fas-ghost";
            case AUTO_BATTLE -> "fas-infinity";
            case SLEEP_COMPUTER -> "fas-moon";
            default -> null;
        };
    }

    private String taskMetaStyleClass(TaskType taskType) {
        return switch (taskType) {
            case WUHuan_V2 -> "task-meta-wuhuan";
            case WUBEI -> "task-meta-wubei";
            case XIULUO_V2 -> "task-meta-xiuluo";
            case AUTO_BATTLE -> "task-meta-auto";
            case SLEEP_COMPUTER -> "task-meta-sleep";
            default -> "task-meta-default";
        };
    }

    private String taskMetaAccessibleText(TaskType taskType) {
        return switch (taskType) {
            case WUHuan_V2 -> "五环";
            case WUBEI -> "五倍";
            case XIULUO_V2 -> "修罗";
            case AUTO_BATTLE -> "挂机";
            case SLEEP_COMPUTER -> "睡眠计算机";
            default -> taskMetaText(taskType);
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
                .collect(Collectors.joining(" -> ")));
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
            windowSelectionMemory.replace(windowTable.getItems().stream()
                    .map(WindowTaskSnapshot::getWindowId)
                    .toList());
            restoreWindowTableSelectionFromMemory();
            refreshSelectionDependentUi();
        }
    }

    private void clearWindowSelection() {
        if (windowTable != null) {
            windowSelectionMemory.clear();
            restoreWindowTableSelectionFromMemory();
            refreshSelectionDependentUi();
        }
    }

    private void selectWindowsByState(java.util.function.Predicate<WindowTaskSnapshot> predicate) {
        if (windowTable == null || predicate == null) {
            return;
        }
        windowSelectionMemory.replace(windowTable.getItems().stream()
                .filter(predicate)
                .map(WindowTaskSnapshot::getWindowId)
                .toList());
        restoreWindowTableSelectionFromMemory();
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

    private WindowTaskQueue buildPendingTaskQueueForSubmit() {
        WindowTaskQueue queue = WindowTaskQueue.of(pendingTaskQueue);
        if (pendingTaskQueue.contains(TaskType.SLEEP_COMPUTER)) {
            return queue.withFailurePolicy(WindowTaskFailurePolicy.STOP_ON_FAILURE);
        }
        return queue;
    }

    private void startPendingTaskQueue() {
        if (pendingTaskQueue.isEmpty()) {
            addWindowLog("任务队列为空，无法启动");
            renderLogList();
            return;
        }
        syncTaskRunCountsFromTileEditor(pendingTaskQueue);
        WindowTaskQueue queue = buildPendingTaskQueueForSubmit();
        List<String> windowIds = getSelectedWindowIds();
        warnUnavailableSelectedWindows("启动队列");
        runWindowCommandInBackground(() -> {
            List<String> acceptingWindowIds = filterAcceptingWindowIds(windowIds);
            if (acceptingWindowIds.isEmpty()) {
                return WindowTaskCommandResult.empty("选中的窗口当前不可接任务，未启动队列",
                        windowTaskControlService.getSnapshots());
            }
            WindowTaskCommandResult cloudGate = ensureCloudDecisionDevReadyForTaskStart();
            if (cloudGate != null) {
                return cloudGate;
            }
            return windowTaskControlService.start(WindowTaskStartRequest.sameQueue(acceptingWindowIds, queue));
        });
    }

    private void handleMainStartPauseButton() {
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        if (shouldPauseFromMainStartButton(selected)) {
            List<String> windowIds = getSelectedWindowIds();
            runWindowCommandInBackground(() -> windowTaskControlService.pauseWindows(windowIds));
            return;
        }
        startMainSelectedTasks();
    }

    private void startMainSelectedTasks() {
        List<WindowTaskSnapshot> selectedSnapshots = getSelectedWindowSnapshots();
        boolean hasPausedSelection = hasPausedSelection(selectedSnapshots);
        TaskType selectedTaskType = windowTaskTypeComboBox == null ? null : windowTaskTypeComboBox.getValue();
        log.info("UI startMainSelectedTasks entered: selectedTask={} pendingQueue={} selectedWindows={}",
                selectedTaskType, pendingTaskQueue, getSelectedWindowIds());
        if (pendingTaskQueue.isEmpty() && !hasPausedSelection) {
            addWindowLog("还没有选择任务，无法启动");
            renderLogList();
            return;
        }
        syncTaskRunCountsFromTileEditor(pendingTaskQueue);
        List<String> selectedWindowIds = getSelectedWindowIds();
        WindowTaskQueue queue = buildPendingTaskQueueForSubmit();
        TaskType defaultTaskType = pendingTaskQueue.isEmpty() ? selectedTaskType : pendingTaskQueue.get(0);
        addWindowLog(pendingTaskQueue.isEmpty()
                ? "启动：继续暂停中的选中窗口"
                : "启动：自动刷新/发现游戏窗口，然后启动可接任务窗口");
        renderLogList();
        runWindowCommandInBackground(() -> {
            List<String> pausedWindowIds = selectedSnapshots.stream()
                    .filter(snapshot -> snapshot.getStatus() == WindowRuntimeStatus.PAUSED)
                    .map(WindowTaskSnapshot::getWindowId)
                    .filter(id -> id != null && !id.isBlank())
                    .distinct()
                    .toList();
            if (!pausedWindowIds.isEmpty()) {
                log.info("Start selected task flow: resume paused selected windows={}", pausedWindowIds);
                WindowTaskCommandResult resumeResult = windowTaskControlService.resumeWindows(pausedWindowIds);
                if (pendingTaskQueue.isEmpty()) {
                    return resumeResult;
                }
            }
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
            WindowTaskCommandResult cloudGate = ensureCloudDecisionDevReadyForTaskStart();
            if (cloudGate != null) {
                return cloudGate;
            }
            log.info("Start selected task flow: submit start queue={} targets={}", queue.toLogText(), targetWindowIds);
            return windowTaskControlService.start(WindowTaskStartRequest.sameQueue(targetWindowIds, queue));
        });
    }

    private void scanAndRefreshGameWindowsFromMain() {
        TaskType defaultTaskType = windowTaskTypeComboBox == null ? TaskType.WUHuan_V2 : windowTaskTypeComboBox.getValue();
        if (defaultTaskType == null || defaultTaskType == TaskType.UNKNOWN) {
            defaultTaskType = TaskType.WUHuan_V2;
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
        if (pendingTaskQueue.isEmpty()) {
            addWindowLog(actionName + "失败：还没有选择任务");
            renderLogList();
            return;
        }
        syncTaskRunCountsFromTileEditor(pendingTaskQueue);
        WindowTaskQueue queue = buildPendingTaskQueueForSubmit();
        runWindowCommandInBackground(() -> {
            List<String> acceptingWindowIds = filterAcceptingWindowIds(windowIds);
            if (acceptingWindowIds.isEmpty()) {
                return WindowTaskCommandResult.empty(actionName + "失败：窗口当前不可接任务",
                        windowTaskControlService.getSnapshots());
            }
            WindowTaskCommandResult cloudGate = ensureCloudDecisionDevReadyForTaskStart();
            if (cloudGate != null) {
                return cloudGate;
            }
            return windowTaskControlService.start(WindowTaskStartRequest.sameQueue(acceptingWindowIds, queue));
        });
    }

    private WindowTaskCommandResult ensureCloudDecisionDevReadyForTaskStart() {
        CloudDecisionDevSidecarService.StartupResult result =
                cloudDecisionDevSidecarService.ensureReadyForTaskStart();
        if (result.skipped()) {
            log.debug("UI task start cloud sidecar gate skipped: {}", result.message());
            return null;
        }
        if (result.available()) {
            log.info("UI task start cloud sidecar gate passed: {} startedProcess={}",
                    result.message(), result.startedProcess());
            return null;
        }
        String message = result.message() + "，未启动任务";
        log.warn("UI task start cloud sidecar gate blocked: {}", result.message());
        javafx.application.Platform.runLater(() -> {
            addWindowLog("Cloud决策端点：" + message);
            renderLogList();
        });
        return WindowTaskCommandResult.empty(message, windowTaskControlService.getSnapshots());
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

    private void openAutomationMetricsDashboard() {
        try {
            Path dashboard = automationMetricsService.writeDashboardNow();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(dashboard.toUri());
                addWindowLog("业务 Dashboard 已打开：" + dashboard);
            } else {
                addWindowLog("业务 Dashboard 已生成：" + dashboard);
            }
        } catch (Exception e) {
            addWindowLog("业务 Dashboard 打开失败：" + e.getMessage());
        }
        renderLogList();
    }

    private void runMapSurveyCommand(String actionName, MapSurveyCommand command) {
        runMapSurveyCommand(actionName, true, command);
    }

    private void runMapSurveyCommand(String actionName, boolean requireMapName, MapSurveyCommand command) {
        if (mapSurveyMapNameField != null) {
            botProperties.setMapSurveyMapName(mapSurveyMapNameField.getText() == null
                    ? ""
                    : mapSurveyMapNameField.getText().trim());
        }
        String mapName = botProperties.getMapSurveyMapName();
        if (requireMapName && !hasText(mapName)) {
            addWindowLog(actionName + "失败：请先填写地图名");
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
        return windowSelectionMemory.selectedIds();
    }

    private List<WindowTaskSnapshot> getSelectedWindowSnapshots() {
        List<String> selectedWindowIds = getSelectedWindowIds();
        if (selectedWindowIds.isEmpty()) {
            return List.of();
        }
        Map<String, WindowTaskSnapshot> snapshotsById = new LinkedHashMap<>();
        for (WindowTaskSnapshot snapshot : windowTaskControlService.getSystemSnapshot().getWindows()) {
            if (snapshot != null && snapshot.getWindowId() != null && !snapshot.getWindowId().isBlank()) {
                snapshotsById.put(snapshot.getWindowId(), snapshot);
            }
        }
        return selectedWindowIds.stream()
                .map(snapshotsById::get)
                .filter(snapshot -> snapshot != null)
                .toList();
    }

    private void rememberVisibleWindowTableSelection() {
        if (windowTable == null) {
            return;
        }
        List<String> visibleWindowIds = windowTable.getItems().stream()
                .map(WindowTaskSnapshot::getWindowId)
                .toList();
        List<String> selectedVisibleWindowIds = windowTable.getSelectionModel().getSelectedItems().stream()
                .map(WindowTaskSnapshot::getWindowId)
                .toList();
        windowSelectionMemory.replaceVisibleSelection(visibleWindowIds, selectedVisibleWindowIds);
    }

    private void restoreWindowTableSelectionFromMemory() {
        if (windowTable == null) {
            return;
        }
        restoringWindowTableSelection = true;
        try {
            windowTable.getSelectionModel().clearSelection();
            for (WindowTaskSnapshot window : windowTable.getItems()) {
                if (windowSelectionMemory.isSelected(window.getWindowId())) {
                    windowTable.getSelectionModel().select(window);
                }
            }
            windowTable.refresh();
        } finally {
            restoringWindowTableSelection = false;
        }
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

    private List<String> filterAcceptingWindowIds(List<String> windowIds) {
        if (windowIds == null || windowIds.isEmpty()) {
            return List.of();
        }
        return windowTaskControlService.getSnapshots().stream()
                .filter(WindowTaskSnapshot::isAcceptingTaskQueue)
                .map(WindowTaskSnapshot::getWindowId)
                .filter(id -> id != null && !id.isBlank())
                .filter(windowIds::contains)
                .distinct()
                .toList();
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
                        saveMapLabelSampleButton, testMapLabelSampleButton,
                        recordCameraLeftButton, recordCameraRightButton, recordCameraTopButton, recordCameraBottomButton,
                        recordCameraCenterButton, testProjectedPlayerPointButton,
                        recordPlayerPointCorrectionButton, testCorrectedPlayerPointButton, undoPlayerPointCorrectionButton,
                        selectAllWindowsButton, selectRunningWindowsButton, selectIdleWindowsButton,
                        selectProblemWindowsButton, selectBoundWindowsButton, selectUnboundWindowsButton, clearWindowSelectionButton,
                        startCurrentTaskButton, startWindowSelectedTaskButton,
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
        windowSelectionMemory.replace(ids);
        restoreWindowTableSelectionFromMemory();
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
        windowSelectionMemory.retainKnownIds(snapshot.getWindows().stream()
                .map(WindowTaskSnapshot::getWindowId)
                .toList());
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
        restoreWindowTableSelectionFromMemory();
        windowSystemLabel.setText("已注册 " + snapshot.getRegisteredWindowCount()
                + " / " + snapshot.getMaxWindowCount()
                + "，已选 " + windowSelectionMemory.size()
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
        ensureSelectedWindowDetailRows();
        List<WindowTaskSnapshot> selected = getSelectedWindowSnapshots();
        if (selected.isEmpty()) {
            updateDetailRows(Map.of());
            return;
        }
        WindowTaskSnapshot snapshot = selected.get(0);
        WindowIdentityView identity = parseWindowIdentity(snapshot);
        String selectedPrefix = selected.size() == 1
                ? nullToDash(snapshot.getWindowId())
                : selected.size() + " 个已选，当前显示 " + nullToDash(snapshot.getWindowId());
        String processId = snapshot.getNativeProcessId() <= 0 ? "-" : String.valueOf(snapshot.getNativeProcessId());
        Map<String, String> values = new LinkedHashMap<>();
        values.put("窗口", selectedPrefix);
        values.put("角色", identity.roleName() + " · " + identity.serverName() + " · " + identity.playerId());
        values.put("状态", snapshot.getStatusDisplayName()
                + " · 可接任务 " + (snapshot.isAcceptingTaskQueue() ? "是" : "否"));
        values.put("绑定", "hwnd=" + nullToDash(snapshot.getNativeHandle()) + " · pid=" + processId);
        values.put("当前", snapshot.getRunningTaskDisplayName()
                + " · 进度 " + snapshot.getRunningTaskProgressText());
        values.put("上次执行", nullToDash(snapshot.getLastQueueDisplayText())
                + " · " + snapshot.getLastQueueResultDisplayName());
        values.put("最近任务", snapshot.getLastTaskDisplayName()
                + " · " + snapshot.getLastResultDisplayName());
        values.put("结束时间", formatDateTime(snapshot.getLastFinishedAt()));
        values.put("消息", firstNotBlank(snapshot.getLastQueueMessage(), snapshot.getLastResultMessage(), snapshot.getLastMessage()));
        values.put("标题", nullToDash(snapshot.getNativeTitle()));
        updateDetailRows(values);
    }

    private void ensureSelectedWindowDetailRows() {
        if (!selectedWindowDetailValueLabels.isEmpty()) {
            return;
        }
        /*
         * This panel refreshes every second. Reusing the same JavaFX nodes avoids accumulating
         * millions of detached Label/HBox objects in the JavaFX/CSS heap during long debug runs.
         */
        selectedWindowDetailBox.getChildren().clear();
        for (String key : SELECTED_WINDOW_DETAIL_KEYS) {
            addDetailRow(key);
        }
    }

    private void updateDetailRows(Map<String, String> values) {
        for (Map.Entry<String, Label> entry : selectedWindowDetailValueLabels.entrySet()) {
            entry.getValue().setText(nullToDash(values.get(entry.getKey())));
        }
    }

    private void addDetailRow(String key) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("detail-key");
        Label valueLabel = new Label("-");
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("detail-value");
        HBox row = new HBox(10, keyLabel, valueLabel);
        row.getStyleClass().add("detail-row");
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        selectedWindowDetailBox.getChildren().add(row);
        selectedWindowDetailValueLabels.put(key, valueLabel);
    }

    private void renderLogList() {
        if (logList == null) {
            return;
        }
        logList.getItems().setAll(windowCommandLogs);
    }

    private void refreshControlStates() {
        refreshSettingsEditLock();
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
        boolean mainStartShouldPause = shouldPauseFromMainStartButton(selected);
        boolean mainStartEnabled = mainStartShouldPause
                || hasQueue
                || (hasSelection && hasPausedSelection(selected));
        boolean showResumeAction = shouldShowResumeAction(selected);
        boolean hasBusyWindow = windowTaskControlService.getSystemSnapshot().getWindows().stream()
                .anyMatch(WindowTaskSnapshot::isBusy);
        if (selectedWindowCountLabel != null) {
            selectedWindowCountLabel.setText("已选窗口：" + selectedCount);
        }

        setButtonDisabled(applySelectedTaskButton, !hasSelection);
        if (startCurrentTaskButton != null) {
            startCurrentTaskButton.setText(mainStartShouldPause ? "暂停" : "启动");
            updateMainStartButtonStyle(mainStartShouldPause);
        }
        setButtonDisabled(startCurrentTaskButton, !mainStartEnabled);
        setButtonDisabled(startWindowSelectedTaskButton, !hasSelection);
        setButtonDisabled(startQueueButton, !hasSelection || !hasQueue);
        if (pauseSelectedWindowsButton != null) {
            pauseSelectedWindowsButton.setText(showResumeAction ? "继续" : "暂停");
        }
        setButtonDisabled(pauseSelectedWindowsButton, !hasSelection);
        setButtonDisabled(resumeSelectedWindowsButton, !hasSelection);
        setButtonDisabled(stopSelectedWindowsButton, !hasSelection);
        setButtonDisabled(stopAllWindowsButton, !hasBusyWindow);
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

    private void refreshSettingsEditLock() {
        boolean locked = isSettingsEditLocked();
        setSettingsControlsDisabled(locked);
        if (settingsEditLockLabel != null) {
            settingsEditLockLabel.setText(locked
                    ? "设置已锁定：请先停止全部任务，等待窗口不再运行/暂停后再修改。"
                    : "设置可修改：改完请点应用；任务运行或暂停期间会锁定配置。");
        }
    }

    private boolean isSettingsEditLocked() {
        if (windowCommandRunning) {
            return true;
        }
        WindowSystemSnapshot snapshot = windowTaskControlService.getSystemSnapshot();
        return snapshot.getWindows().stream().anyMatch(WindowTaskSnapshot::isBusy);
    }

    private void setSettingsControlsDisabled(boolean disabled) {
        setNodeDisabled(xiuluoRunCountField, disabled);
        setNodeDisabled(wuhuanRunCountComboBox, disabled);
        setNodeDisabled(fivefoldRunCountField, disabled);
        setNodeDisabled(taskStartupPreparationEnabledCheckBox, disabled);
        setNodeDisabled(xiuluoMaintenanceRunImmediatelyCheckBox, disabled);
        setNodeDisabled(summonSkillCleanEnabledCheckBox, disabled);
        setNodeDisabled(summonSkillIntervalMinutesComboBox, disabled);
        setNodeDisabled(xiuluoHealPetMaintenanceEnabledCheckBox, disabled);
        setNodeDisabled(xiuluoHealPetIntervalMinutesComboBox,
                disabled || !xiuluoHealPetMaintenanceEnabledCheckBox.isSelected());
        setNodeDisabled(xiuluoRepairEquipmentMaintenanceEnabledCheckBox, disabled);
        setNodeDisabled(xiuluoRepairEquipmentIntervalMinutesComboBox,
                disabled || !xiuluoRepairEquipmentMaintenanceEnabledCheckBox.isSelected());
        setNodeDisabled(leaderCommonBoxEnabledCheckBox, disabled);
        setNodeDisabled(memberCommonBoxEnabledCheckBox, disabled);
        setNodeDisabled(playerHpSupplyCheckBox, disabled);
        setNodeDisabled(playerHpThresholdComboBox, disabled);
        setNodeDisabled(playerMpSupplyCheckBox, disabled);
        setNodeDisabled(playerMpThresholdComboBox, disabled);
        setNodeDisabled(petHpSupplyCheckBox, disabled);
        setNodeDisabled(petHpThresholdComboBox, disabled);
        setNodeDisabled(petMpSupplyCheckBox, disabled);
        setNodeDisabled(petMpThresholdComboBox, disabled);
        setNodeDisabled(applySettingsButton, disabled);
        setNodeDisabled(taskCountShortcutButton, disabled);
        setNodeDisabled(taskCountEditorBar, disabled);
    }

    private void setNodeDisabled(Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    private void rejectSettingsEditWhileBusy(String settingName) {
        addWindowLog(settingName + "未修改：请先停止全部任务，运行/暂停期间锁定配置。");
        renderLogList();
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

    private boolean shouldPauseFromMainStartButton(List<WindowTaskSnapshot> selected) {
        return selected != null
                && !selected.isEmpty()
                && selected.stream().allMatch(snapshot -> snapshot.getStatus() == WindowRuntimeStatus.RUNNING);
    }

    private boolean hasPausedSelection(List<WindowTaskSnapshot> selected) {
        return selected != null
                && selected.stream().anyMatch(snapshot -> snapshot.getStatus() == WindowRuntimeStatus.PAUSED);
    }

    private void updateMainStartButtonStyle(boolean pauseMode) {
        if (startCurrentTaskButton == null) {
            return;
        }
        if (pauseMode) {
            removeStyleClass(startCurrentTaskButton, "start-action");
            addStyleClass(startCurrentTaskButton, "pause-action");
        } else {
            removeStyleClass(startCurrentTaskButton, "pause-action");
            addStyleClass(startCurrentTaskButton, "start-action");
        }
    }

    private void addStyleClass(Node node, String styleClass) {
        if (node != null && styleClass != null && !node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private void removeStyleClass(Node node, String styleClass) {
        if (node != null && styleClass != null) {
            node.getStyleClass().remove(styleClass);
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
                .filter(taskType -> taskType != TaskType.XIULUO)
                .toList();
    }

    @FunctionalInterface
    private interface WindowCommand {
        WindowTaskCommandResult execute();
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
