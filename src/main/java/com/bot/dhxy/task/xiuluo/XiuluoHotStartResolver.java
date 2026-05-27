package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.task.hotstart.TaskHotStartScreenState;
import com.bot.dhxy.task.hotstart.TaskHotStartService;
import com.bot.dhxy.task.hotstart.TaskHotStartSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Determines where Xiuluo V2 can safely enter the phase machine from the current screen.
 *
 * <p>This resolver must not execute task actions. It only classifies the current screen and returns
 * the phase that should run next. Later we will plug in objective parsing and option-dialog matching
 * here, but the output should remain a {@link XiuluoRoundContext}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XiuluoHotStartResolver {

    private static final String TASK_CODE = "xiuluo_v2";

    private final TaskHotStartService taskHotStartService;

    /**
     * Resolve the initial phase for one Xiuluo round.
     *
     * @param round one-based round number being started.
     * @param allowTaskPanelFallback true only on real startup, when existing task-panel objective
     *                               text may belong to an accepted unfinished Xiuluo task.
     * @return round state whose phase is the hot-start insertion point.
     */
    public XiuluoRoundContext resolve(int round, boolean allowTaskPanelFallback) {
        TaskHotStartSnapshot snapshot = taskHotStartService.snapshot(TASK_CODE, "xiuluo-v2:round-start");
        TaskHotStartScreenState screenState = snapshot.state();
        log.info("[xiuluo-v2] hot-start snapshot: round={} state={} dialogType={} allowTaskPanelFallback={}",
                round, screenState, snapshot.dialogType(), allowTaskPanelFallback);

        return switch (screenState) {
            case IN_COMBAT -> XiuluoRoundContext.builder()
                    .phase(XiuluoPhase.WAIT_COMBAT)
                    .round(round)
                    .source("hot-start:in-combat")
                    .build();
            case STORY_DIALOG -> XiuluoRoundContext.builder()
                    .phase(XiuluoPhase.READ_OBJECTIVE)
                    .round(round)
                    .source("hot-start:story-dialog")
                    .build();
            case OPTION_DIALOG -> XiuluoRoundContext.builder()
                    .phase(XiuluoPhase.ACCEPT_TASK_DIALOG)
                    .round(round)
                    .source("hot-start:option-dialog")
                    .build();
            /*
             * NONE only proves that no dialog/combat is currently taking over the screen. It does
             * not prove an unfinished Xiuluo task exists, so normal startup must begin from the
             * accept-task chain. Task-panel takeover needs a positive objective read before we can
             * safely jump into READ_OBJECTIVE/NAVIGATE_TO_TARGET.
             */
            case NONE -> XiuluoRoundContext.start(round);
        };
    }
}
