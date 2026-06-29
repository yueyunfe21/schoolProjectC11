package com.bot.dhxy.service.dialog;

import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.WhiteTemplateSpec;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;
import java.util.List;

@Value
@Builder
public class DialogHandleRequest {
    private static final String WUHUAN_SHOE_SHOP_BUY_OPTION_TEMPLATE =
            "images/template/dialog/wuhuan/wuhuan_shop_buy_option.png";
    private static final String WUHUAN_SHOE_SHOP_BUY_OPTION_KEYWORD = "买点";

    String sourceTask;
    DialogOperation operation;
    Point initialClick;

    @Builder.Default
    DialogStoryPolicy storyPolicy = DialogStoryPolicy.IGNORE;

    @Builder.Default
    DialogOptionPolicy optionPolicy = DialogOptionPolicy.IGNORE;

    @Builder.Default
    DialogFallbackPolicy fallbackPolicy = DialogFallbackPolicy.RETURN_UNRESOLVED;

    String targetKeyword;
    String itemToGive;
    Integer knownBagIndex;
    Integer rememberedRelativeX;
    Integer rememberedRelativeY;
    List<GreenTemplateClickSpec> greenTemplateSpecs;
    List<WhiteTemplateSpec> whiteTemplateSpecs;

    @Builder.Default
    boolean allowFallbackOptionClick = false;

    @Builder.Default
    boolean includeCleanupBusinessOptions = true;

    @Builder.Default
    boolean allowFullMaintenanceBroadcastFallback = true;

    @Builder.Default
    boolean verifyDialogType = true;

    @Builder.Default
    boolean hidePlayerNamesBeforeCapture = false;

    public static DialogHandleRequest inspect(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.INSPECT)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.IGNORE)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .allowFallbackOptionClick(false)
                .build();
    }

    public static DialogHandleRequest giveItemIfAvailable(String sourceTask, String itemToGive, Integer knownBagIndex) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.GIVE_ITEM_IF_AVAILABLE)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.GIVE_ITEM_IF_AVAILABLE)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .itemToGive(itemToGive)
                .knownBagIndex(knownBagIndex)
                .allowFallbackOptionClick(false)
                .build();
    }

    public static DialogHandleRequest handleKeywordOption(String sourceTask, String targetKeyword, boolean allowFallbackOptionClick) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLICK_KEYWORD)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_KEYWORD)
                .fallbackPolicy(allowFallbackOptionClick
                        ? DialogFallbackPolicy.CLICK_FIRST_OPTION
                        : DialogFallbackPolicy.RETURN_UNRESOLVED)
                .targetKeyword(targetKeyword)
                .allowFallbackOptionClick(allowFallbackOptionClick)
                .build();
    }

    public static DialogHandleRequest handleRouteKeywordOption(String sourceTask,
                                                               String targetKeyword,
                                                               boolean allowFallbackOptionClick) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.ROUTE_TRANSFER)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_KEYWORD)
                .fallbackPolicy(allowFallbackOptionClick
                        ? DialogFallbackPolicy.CLICK_FIRST_OPTION
                        : DialogFallbackPolicy.RETURN_UNRESOLVED)
                .targetKeyword(targetKeyword)
                .allowFallbackOptionClick(allowFallbackOptionClick)
                .build();
    }

    public static DialogHandleRequest handleRememberedRouteOption(String sourceTask,
                                                                  int relativeX,
                                                                  int relativeY,
                                                                  String targetKeyword) {
        return handleRememberedChoiceOption(sourceTask, relativeX, relativeY, targetKeyword);
    }

    public static DialogHandleRequest handleRememberedChoiceOption(String sourceTask,
                                                                   int relativeX,
                                                                   int relativeY,
                                                                   String actionKey) {
        return handleRememberedChoiceOption(sourceTask, relativeX, relativeY, actionKey, true);
    }

    public static DialogHandleRequest handleRememberedChoiceOption(String sourceTask,
                                                                   int relativeX,
                                                                   int relativeY,
                                                                   String actionKey,
                                                                   boolean verifyDialogType) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLICK_REMEMBERED_OPTION)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_REMEMBERED_POINT)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .targetKeyword(actionKey)
                .rememberedRelativeX(relativeX)
                .rememberedRelativeY(relativeY)
                .allowFallbackOptionClick(false)
                .verifyDialogType(verifyDialogType)
                .build();
    }

    public static DialogHandleRequest handleBusinessOption(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLICK_BUSINESS_OPTION)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_BUSINESS_OPTION)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .allowFallbackOptionClick(false)
                .build();
    }

    public static DialogHandleRequest handleMaintenanceBroadcastOption(String sourceTask) {
        return handleMaintenanceBroadcastOption(sourceTask, true);
    }

    public static DialogHandleRequest handleMaintenanceBroadcastOption(String sourceTask,
                                                                       boolean allowFullMaintenanceBroadcastFallback) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLICK_BUSINESS_OPTION)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_BUSINESS_OPTION)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .allowFallbackOptionClick(false)
                .includeCleanupBusinessOptions(false)
                .allowFullMaintenanceBroadcastFallback(allowFullMaintenanceBroadcastFallback)
                .build();
    }

    public static DialogHandleRequest acceptTask(String sourceTask, Point initialClick) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.ACCEPT_TASK)
                .initialClick(initialClick)
                .storyPolicy(DialogStoryPolicy.CLICK_THROUGH)
                .optionPolicy(DialogOptionPolicy.FALLBACK_FIRST_OPTION)
                .fallbackPolicy(DialogFallbackPolicy.CLICK_FIRST_OPTION)
                .allowFallbackOptionClick(true)
                .build();
    }

    public static DialogHandleRequest fallbackLastOption(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLEANUP)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.FALLBACK_LAST_OPTION)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .allowFallbackOptionClick(false)
                .build();
    }

    public static DialogHandleRequest clickStory(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLEANUP)
                .storyPolicy(DialogStoryPolicy.CLICK_THROUGH)
                .optionPolicy(DialogOptionPolicy.IGNORE)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .allowFallbackOptionClick(false)
                .build();
    }

    public static DialogHandleRequest handleGreenTemplateOption(String sourceTask,
                                                                List<GreenTemplateClickSpec> specs,
                                                                boolean verifyDialogType) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLICK_GREEN_TEMPLATE)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_GREEN_TEMPLATE)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .greenTemplateSpecs(specs)
                .verifyDialogType(verifyDialogType)
                .build();
    }

    public static DialogHandleRequest handleWuhuanShoeShopBuyOption(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.WUHUAN_SHOE_SHOP_BUY_OPTION)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_GREEN_TEMPLATE)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .targetKeyword(WUHUAN_SHOE_SHOP_BUY_OPTION_KEYWORD)
                .greenTemplateSpecs(List.of(new GreenTemplateClickSpec(
                        "wuhuan.shoeShopBuyOption",
                        WUHUAN_SHOE_SHOP_BUY_OPTION_TEMPLATE,
                        -3,
                        3,
                        1)))
                .verifyDialogType(true)
                .allowFallbackOptionClick(false)
                .build();
    }

    public static DialogHandleRequest verifyExpectedOptionDialog(String sourceTask, String expectedGreenTemplatePath) {
        DialogOptionPolicy optionPolicy = expectedGreenTemplatePath == null || expectedGreenTemplatePath.isBlank()
                ? DialogOptionPolicy.VERIFY_OPTION
                : DialogOptionPolicy.VERIFY_GREEN_TEMPLATE;
        List<GreenTemplateClickSpec> specs = expectedGreenTemplatePath == null || expectedGreenTemplatePath.isBlank()
                ? null
                : List.of(new GreenTemplateClickSpec("expectedDialog", expectedGreenTemplatePath, 0, 0, 0));
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.VERIFY_EXPECTED_DIALOG)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(optionPolicy)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .greenTemplateSpecs(specs)
                .verifyDialogType(true)
                .build();
    }

    public static DialogHandleRequest verifyExpectedOptionDialog(String sourceTask,
                                                                 List<String> expectedGreenTemplatePaths) {
        List<GreenTemplateClickSpec> specs = expectedGreenTemplatePaths == null
                ? List.of()
                : expectedGreenTemplatePaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(path -> new GreenTemplateClickSpec("expectedDialog", path, 0, 0, 0))
                .toList();
        DialogOptionPolicy optionPolicy = specs.isEmpty()
                ? DialogOptionPolicy.VERIFY_OPTION
                : DialogOptionPolicy.VERIFY_GREEN_TEMPLATE;
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.VERIFY_EXPECTED_DIALOG)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(optionPolicy)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .greenTemplateSpecs(specs)
                .verifyDialogType(true)
                .build();
    }

    public static DialogHandleRequest readStoryObjective(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.READ_STORY_OBJECTIVE)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.IGNORE)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .build();
    }

    public static DialogHandleRequest verifyWhiteTemplate(String sourceTask,
                                                           String actionKey,
                                                           String templatePath) {
        return verifyWhiteTemplates(sourceTask, List.of(new WhiteTemplateSpec(actionKey, templatePath)));
    }

    public static DialogHandleRequest verifyWhiteTemplates(String sourceTask,
                                                           List<WhiteTemplateSpec> specs) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.VERIFY_WHITE_TEMPLATE)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.IGNORE)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .whiteTemplateSpecs(specs)
                .build();
    }
}
