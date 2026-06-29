package com.bot.dhxy.task.wubei;

import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.MemoryService;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.dialog.WindowDialogPreparationProvider;
import com.bot.dhxy.window.model.WindowDialogInterest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 五倍-owned dialog preparation provider.
 *
 * <p>This keeps 五倍 templates and choice memory out of the generic window runner. The provider
 * prepares only the operation requested by the task interest, so unrelated dialogs observed by the
 * runner are not forced through every 五倍 template.</p>
 */
@Component
@RequiredArgsConstructor
public class WubeiDialogPreparationProvider implements WindowDialogPreparationProvider {

    private final DialogService dialogService;
    private final MemoryService memoryService;

    @Override
    public boolean supports(TaskType taskType, DialogOperation operation) {
        return taskType == TaskType.WUBEI
                && (operation == DialogOperation.WUBEI_ACCEPT_TASK
                || operation == DialogOperation.WUBEI_ENTER_BATTLE
                || operation == DialogOperation.WUBEI_PROBE_STORY);
    }

    @Override
    public Optional<PreparedDialogAction> prepare(WindowDialogInterest interest,
                                                  DialogOperation operation,
                                                  String source) {
        return prepare(interest, operation, source, null);
    }

    @Override
    public Optional<PreparedDialogAction> prepare(WindowDialogInterest interest,
                                                  DialogOperation operation,
                                                  String source,
                                                  DialogDetection suppliedDetection) {
        if (!supports(interest == null ? null : interest.getTaskType(), operation)) {
            return Optional.empty();
        }
        if (operation == DialogOperation.WUBEI_ACCEPT_TASK) {
            Optional<PreparedDialogAction> remembered = prepareRememberedAcceptOption(
                    source + ":acceptMemory", suppliedDetection);
            return remembered.isPresent()
                    ? remembered
                    : dialogService.prepareGreenTemplateOption(
                    source + ":acceptTask",
                    DialogOperation.WUBEI_ACCEPT_TASK,
                    WubeiDialogCatalog.acceptTaskSpecs(),
                    true,
                    null,
                    suppliedDetection);
        }
        if (operation == DialogOperation.WUBEI_ENTER_BATTLE) {
            return dialogService.prepareGreenTemplateOption(
                    source + ":enterBattle",
                    DialogOperation.WUBEI_ENTER_BATTLE,
                    WubeiDialogCatalog.enterBattleSpecs(),
                    true,
                    null,
                    suppliedDetection);
        }
        if (operation == DialogOperation.WUBEI_PROBE_STORY) {
            boolean absentAllowed = interest != null && interest.isAbsentAllowed(System.currentTimeMillis());
            return dialogService.prepareWhiteStoryTemplateOrAbsent(
                    source + ":probeStory",
                    DialogOperation.WUBEI_PROBE_STORY,
                    WubeiDialogCatalog.probeStorySpecs(),
                    WubeiDialogCatalog.STORY_PROBE_NO_TARGET,
                    absentAllowed ? WubeiDialogCatalog.STORY_PROBE_ABSENT : null,
                    absentAllowed ? WubeiDialogCatalog.STORY_ABSENT_TEXT : null,
                    suppliedDetection);
        }
        return Optional.empty();
    }

    private Optional<PreparedDialogAction> prepareRememberedAcceptOption(String source,
                                                                         DialogDetection suppliedDetection) {
        Optional<MemoryService.DialogChoiceEntry> remembered =
                memoryService.findStableTaskDialogChoice(
                        WubeiDialogCatalog.TASK_CODE,
                        "acceptTask",
                        WubeiDialogCatalog.ACCEPT_NPC_NAME);
        if (remembered.isEmpty()) {
            return Optional.empty();
        }
        MemoryService.DialogChoiceEntry entry = remembered.get();
        return dialogService.prepareRememberedChoiceOption(
                source,
                DialogOperation.WUBEI_ACCEPT_TASK,
                WubeiDialogCatalog.OPTION_ACCEPT_TASK,
                entry.getRelativeX(),
                entry.getRelativeY(),
                entry.getOptionText(),
                false,
                suppliedDetection);
    }
}
