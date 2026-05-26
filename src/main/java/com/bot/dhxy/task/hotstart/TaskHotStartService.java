package com.bot.dhxy.task.hotstart;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.service.BattleRadarService;
import com.bot.dhxy.service.DialogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskHotStartService {

    private final BattleRadarService battleRadarService;
    private final GameContext gameContext;
    private final DialogService dialogService;

    public TaskHotStartSnapshot snapshot(String taskCode, String source) {
        String safeTaskCode = taskCode == null || taskCode.isBlank() ? "unknown" : taskCode;
        String safeSource = source == null || source.isBlank() ? "unknown" : source;

        boolean inCombat = battleRadarService.checkAndSyncCombatState()
                && gameContext.getCurrentActionState() == GameContext.ActionState.IN_COMBAT;
        if (inCombat) {
            TaskHotStartSnapshot snapshot = new TaskHotStartSnapshot(
                    safeTaskCode, safeSource, TaskHotStartScreenState.IN_COMBAT, DialogService.DialogType.NONE);
            log.info("task hot-start snapshot: task={} source={} state={}",
                    snapshot.taskCode(), snapshot.source(), snapshot.state());
            return snapshot;
        }

        DialogService.DialogType dialogType = dialogService.detectDialogTypeNoFocus(
                "hot-start:" + safeTaskCode + ":" + safeSource);
        TaskHotStartScreenState state = switch (dialogType) {
            case OPTION -> TaskHotStartScreenState.OPTION_DIALOG;
            case STORY -> TaskHotStartScreenState.STORY_DIALOG;
            case NONE -> TaskHotStartScreenState.NONE;
        };
        TaskHotStartSnapshot snapshot = new TaskHotStartSnapshot(safeTaskCode, safeSource, state, dialogType);
        log.info("task hot-start snapshot: task={} source={} state={} dialogType={}",
                snapshot.taskCode(), snapshot.source(), snapshot.state(), snapshot.dialogType());
        return snapshot;
    }
}
