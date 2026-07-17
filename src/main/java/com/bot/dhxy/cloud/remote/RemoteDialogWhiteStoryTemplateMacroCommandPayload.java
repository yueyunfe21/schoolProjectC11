package com.bot.dhxy.cloud.remote;

import com.bot.dhxy.model.dialog.DialogType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Closed wire command for the {@code LOCAL_MACRO / DIALOG_WHITE_STORY_TEMPLATE} macro, mirroring the Cloud
 * {@code DialogWhiteStoryTemplateMacroCommand} field-for-field. It carries the committed
 * {@code prepareWhiteStoryTemplateOrAbsent} + {@code verifyWhiteStoryTemplate} inputs so the handler can
 * reproduce the one authoritative-frame white story-template observation through the approved local
 * mechanics.
 *
 * <p>{@code specs} is the caller-ordered white-template candidate list (first {@code 0.85} hit wins), each
 * entry a Jackson-safe {@link WhiteTemplateSpecEntry} restating the committed {@code WhiteTemplateSpec}
 * ({@code name} nullable, {@code templatePath} required). {@code absentAllowed} carries the committed
 * absent-target gate; {@code source} is the optional diagnostic label. The optional supplied frame reuses
 * the committed same-frame path (raw PNG bytes + non-blank SHA-256 + full screen-absolute rect + classified
 * type, all present-together or all absent); a fully-absent command drives the mechanics fresh-detection
 * fallback under the exact-window binding gate. The handler re-verifies the supplied SHA-256 before the
 * mechanics. Bytes are defensively copied and excluded from the request digest. No owner/session/queue/retry,
 * no target, no fallback selection.</p>
 */
@Value
@Jacksonized
public class RemoteDialogWhiteStoryTemplateMacroCommandPayload implements RemoteLocalMacroCommandPayload {
    RemoteLocalMacroKind macroKind;
    List<WhiteTemplateSpecEntry> specs;
    boolean absentAllowed;
    String source;
    byte[] suppliedFramePngBytes;
    String suppliedFrameSha256;
    Integer suppliedFrameLeft;
    Integer suppliedFrameTop;
    Integer suppliedFrameRight;
    Integer suppliedFrameBottom;
    DialogType suppliedFrameType;

    @Builder
    public RemoteDialogWhiteStoryTemplateMacroCommandPayload(
            RemoteLocalMacroKind macroKind,
            List<WhiteTemplateSpecEntry> specs,
            boolean absentAllowed,
            String source,
            byte[] suppliedFramePngBytes,
            String suppliedFrameSha256,
            Integer suppliedFrameLeft,
            Integer suppliedFrameTop,
            Integer suppliedFrameRight,
            Integer suppliedFrameBottom,
            DialogType suppliedFrameType) {
        if (macroKind != RemoteLocalMacroKind.DIALOG_WHITE_STORY_TEMPLATE) {
            throw new IllegalArgumentException("macroKind must be DIALOG_WHITE_STORY_TEMPLATE");
        }
        List<WhiteTemplateSpecEntry> specsCopy = specs == null ? List.of() : List.copyOf(specs);
        for (WhiteTemplateSpecEntry spec : specsCopy) {
            if (spec == null) {
                throw new IllegalArgumentException("specs must not contain null");
            }
        }
        byte[] frameCopy = suppliedFramePngBytes == null ? null : suppliedFramePngBytes.clone();
        boolean hasFrame = frameCopy != null;
        boolean hasSha = suppliedFrameSha256 != null && !suppliedFrameSha256.isBlank();
        boolean anySha = suppliedFrameSha256 != null;
        boolean hasRect = suppliedFrameLeft != null && suppliedFrameTop != null
                && suppliedFrameRight != null && suppliedFrameBottom != null;
        boolean anyRect = suppliedFrameLeft != null || suppliedFrameTop != null
                || suppliedFrameRight != null || suppliedFrameBottom != null;
        boolean hasType = suppliedFrameType != null;
        if (hasFrame != (anySha && hasSha)) {
            throw new IllegalArgumentException(
                    "a supplied frame and its non-blank SHA-256 must both be present or both absent");
        }
        if (anyRect != hasRect) {
            throw new IllegalArgumentException("supplied frame rect must be a full quad or fully absent");
        }
        if (hasFrame != hasRect) {
            throw new IllegalArgumentException("a supplied frame requires its screen-absolute rect and vice versa");
        }
        if (hasFrame != hasType) {
            throw new IllegalArgumentException("a supplied frame requires its classified type and vice versa");
        }
        if (hasRect && (suppliedFrameRight <= suppliedFrameLeft || suppliedFrameBottom <= suppliedFrameTop)) {
            throw new IllegalArgumentException("supplied frame rect must be a positive-area rect");
        }
        this.macroKind = macroKind;
        this.specs = specsCopy;
        this.absentAllowed = absentAllowed;
        this.source = source;
        this.suppliedFramePngBytes = frameCopy;
        this.suppliedFrameSha256 = suppliedFrameSha256;
        this.suppliedFrameLeft = suppliedFrameLeft;
        this.suppliedFrameTop = suppliedFrameTop;
        this.suppliedFrameRight = suppliedFrameRight;
        this.suppliedFrameBottom = suppliedFrameBottom;
        this.suppliedFrameType = suppliedFrameType;
    }

    public byte[] getSuppliedFramePngBytes() {
        return suppliedFramePngBytes == null ? null : suppliedFramePngBytes.clone();
    }

    /**
     * Jackson-safe wire restatement of a committed {@code WhiteTemplateSpec}: {@code name} nullable,
     * {@code templatePath} required and non-blank. Mirrors the Cloud
     * {@code DialogWhiteStoryTemplateMacroCommand.WhiteTemplateSpecEntry} field-for-field.
     */
    @Value
    @Jacksonized
    @Builder
    public static class WhiteTemplateSpecEntry {
        String name;
        String templatePath;

        public WhiteTemplateSpecEntry(String name, String templatePath) {
            if (templatePath == null || templatePath.isBlank()) {
                throw new IllegalArgumentException("white template spec entry requires a non-blank templatePath");
            }
            this.name = name;
            this.templatePath = templatePath;
        }
    }
}
