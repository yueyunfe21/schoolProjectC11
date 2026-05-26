package com.bot.dhxy.service.dialog;

import lombok.Builder;
import lombok.Value;

import java.awt.Point;

@Value
@Builder
public class DialogHandleRequest {
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

    @Builder.Default
    boolean allowFallbackOptionClick = false;

    @Builder.Default
    boolean includeCleanupBusinessOptions = true;

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

    public static DialogHandleRequest clickKeyword(String sourceTask, String targetKeyword, boolean allowFallbackOptionClick) {
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

    public static DialogHandleRequest clickBusinessOption(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLICK_BUSINESS_OPTION)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_BUSINESS_OPTION)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .allowFallbackOptionClick(false)
                .build();
    }

    public static DialogHandleRequest clickMaintenanceBroadcastOption(String sourceTask) {
        return DialogHandleRequest.builder()
                .sourceTask(sourceTask)
                .operation(DialogOperation.CLICK_BUSINESS_OPTION)
                .storyPolicy(DialogStoryPolicy.IGNORE)
                .optionPolicy(DialogOptionPolicy.CLICK_BUSINESS_OPTION)
                .fallbackPolicy(DialogFallbackPolicy.RETURN_UNRESOLVED)
                .allowFallbackOptionClick(false)
                .includeCleanupBusinessOptions(false)
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
}
