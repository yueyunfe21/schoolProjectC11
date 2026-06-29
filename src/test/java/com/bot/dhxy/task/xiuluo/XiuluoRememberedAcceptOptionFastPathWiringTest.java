package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR75 修罗 remembered accept-option latency.
 *
 * <p>When the Xiuluo phase machine already knows the current dialog is the accept OPTION dialog,
 * remembered accept-option clicking must preserve {@code verifyDialogType=false}. That lets
 * DialogService use its existing remembered-point fast path without paying another full dialog
 * detection pass. Unknown/recovery callers that still pass true must keep the normal detection
 * behavior.</p>
 */
public class XiuluoRememberedAcceptOptionFastPathWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String request = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java"), StandardCharsets.UTF_8);
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);
        String dialog = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/DialogService.java"), StandardCharsets.UTF_8);

        String oldFactory = between(request,
                "public static DialogHandleRequest handleRememberedChoiceOption(String sourceTask,",
                "public static DialogHandleRequest handleBusinessOption(");
        require(oldFactory.contains("boolean verifyDialogType"),
                "CR75 requires a remembered-choice factory overload that can preserve verifyDialogType");
        require(oldFactory.contains(".verifyDialogType(verifyDialogType)"),
                "remembered-choice request must write the supplied verifyDialogType into the request");

        String knownDialogMethod = between(task,
                "private Optional<XiuluoStepOutcome> handleKnownXiuluoOptionDialog(",
                "private Optional<XiuluoStepOutcome> tryRememberedAcceptTaskOption(");
        require(knownDialogMethod.contains("tryRememberedAcceptTaskOption(context, state, source, verifyDialogType)"),
                "Xiuluo known-option flow must pass verifyDialogType into remembered accept option");

        String rememberedMethod = between(task,
                "private Optional<XiuluoStepOutcome> tryRememberedAcceptTaskOption(",
                "private XiuluoStepOutcome continueAfterAcceptOptionClicked(");
        require(rememberedMethod.contains("boolean verifyDialogType"),
                "remembered accept helper must accept verifyDialogType");
        require(rememberedMethod.contains("handleRememberedChoiceOption("),
                "remembered accept helper must still use the remembered-choice request");
        require(rememberedMethod.contains("OPTION_ACCEPT_TASK, verifyDialogType"),
                "remembered accept request must pass the caller's verifyDialogType to the factory");
        require(rememberedMethod.contains("recordDialogChoiceFailure("),
                "remembered accept misses must still record dialog choice failure");
        require(rememberedMethod.contains("fallback to template"),
                "remembered accept misses must still fall back to template matching");

        String fastPath = between(dialog,
                "if (request.getOptionPolicy() == com.bot.dhxy.service.dialog.DialogOptionPolicy.CLICK_REMEMBERED_POINT",
                "// Stage 2: classify once");
        require(fastPath.contains("!request.isVerifyDialogType()"),
                "DialogService fast path must remain gated by verifyDialogType=false");
        require(fastPath.contains("dialog remembered option fast path without detect"),
                "fast path log must remain available for fresh runtime validation");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
