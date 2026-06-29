package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.dialog.WindowDialogPreparationProvider;
import com.bot.dhxy.window.model.WindowDialogInterest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 修罗-owned watcher preparation for the final enter-battle option.
 */
@Component
@RequiredArgsConstructor
public class XiuluoDialogPreparationProvider implements WindowDialogPreparationProvider {

    private final DialogService dialogService;

    @Override
    public boolean supports(TaskType taskType, DialogOperation operation) {
        return taskType == TaskType.XIULUO_V2 && operation == DialogOperation.XIULUO_ENTER_BATTLE;
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
        return dialogService.prepareGreenTemplateOption(
                source + ":enterBattle",
                DialogOperation.XIULUO_ENTER_BATTLE,
                XiuluoDialogCatalog.enterBattleSpecs(),
                true,
                null,
                suppliedDetection);
    }
}
