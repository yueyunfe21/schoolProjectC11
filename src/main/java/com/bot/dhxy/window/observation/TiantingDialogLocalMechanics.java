package com.bot.dhxy.window.observation;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.tools.ImagePreprocessor;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client-local 天庭 dialog option matching.
 *
 * <p>The business flow puts these eight templates on the client on purpose: when one of them is on
 * screen the answer is already known, so shipping the frame to the cloud only to be told "click the
 * option you can already see" costs a round trip per dialog. Only a dialog that matches none of them
 * goes up, and the cloud answers with the first green line.</p>
 *
 * <p>Matching is raw ROI against raw template, the same way the 修罗 看打 probe works — these
 * templates are crops of the real dialog, so washing them first would only throw information away.</p>
 */
final class TiantingDialogLocalMechanics {

    /** The dialog area on screen; window-relative, unscaled. */
    static final int DIALOG_ROI_LEFT = 200;
    static final int DIALOG_ROI_TOP = 250;
    static final int DIALOG_ROI_WIDTH = 640;
    static final int DIALOG_ROI_HEIGHT = 300;

    static final double MATCH_RATE = 0.85d;

    static final String KAIDA = "images/template/dialog/tianting/kaida.png";
    static final String DUOXIE = "images/template/dialog/tianting/duoxie.png";
    static final String ZHUOYUE = "images/template/dialog/tianting/zhuoyue.png";
    static final String YAOWANG = "images/template/dialog/tianting/yaowang.png";
    /**
     * 使用封妖符. Deliberately not polled here: the flow only offers it in the short window that opens
     * after 多谢 is clicked, so a hit outside that window would take a branch the flow never chose.
     * Its probe lands with the 封妖符 branch that owns the window (WP6).
     */
    static final String FENGYAO = "images/template/dialog/tianting/fengyao.png";

    /** 为民除害 on 李靖's dialog; matched only during the accept leg. */
    static final String ACCEPT = "images/template/dialog/tianting/accept.png";
    /** 取消任务 on 李靖's dialog; matched only under an explicit Cloud cancel operation. */
    static final String CANCEL = "images/template/dialog/tianting/quxiao.png";
    /** 在下愿为三 on 地藏王's dialog; it deliberately reuses this mature 640x300 matcher. */
    static final String GHOST_KING_ACCEPT = "images/template/dialog/guiwang/accept.png";

    /** 使用引妖香; checked post-combat and again in 天庭's no-movement business fallback. */
    static final String YINYAO = "images/template/dialog/tianting/yinyao.png";

    static final String ACTION_ACCEPT_TASK = "tianting.acceptTask";
    static final String ACTION_CANCEL_TASK = "tianting.cancelTask";
    static final String ACTION_GHOST_KING_ACCEPT_TASK = "ghostKing.acceptTask";
    static final String ACTION_ENTER_BATTLE_KAIDA = "tianting.enterBattle.kaida";
    static final String ACTION_DUOXIE = "tianting.duoxie";
    static final String ACTION_FENGYAO = "tianting.fengyao";
    static final String ACTION_YINYAO = "tianting.yinyao";
    static final String ACTION_ENTER_BATTLE_ZHUOYUE = "tianting.enterBattle.zhuoyue";
    static final String ACTION_ENTER_BATTLE_YAOWANG = "tianting.enterBattle.yaowang";

    /** Stable wire/business identity for every locally known dialog option. */
    private static final Map<String, String> ACTION_KEYS_BY_TEMPLATE = Map.ofEntries(
            Map.entry(ACCEPT, ACTION_ACCEPT_TASK),
            Map.entry(CANCEL, ACTION_CANCEL_TASK),
            Map.entry(GHOST_KING_ACCEPT, ACTION_GHOST_KING_ACCEPT_TASK),
            Map.entry(KAIDA, ACTION_ENTER_BATTLE_KAIDA),
            Map.entry(DUOXIE, ACTION_DUOXIE),
            Map.entry(FENGYAO, ACTION_FENGYAO),
            Map.entry(YINYAO, ACTION_YINYAO),
            Map.entry(ZHUOYUE, ACTION_ENTER_BATTLE_ZHUOYUE),
            Map.entry(YAOWANG, ACTION_ENTER_BATTLE_YAOWANG));

    /**
     * The options polled on every frame, in match order.
     *
     * <p>看打 must lead — it is by far the most common way into a fight, and taking 卓越 when both are
     * offered would silently switch branches. The remaining three carry no business ordering (design
     * decision Q3); they are not free to be reordered ahead of 看打.</p>
     */
    private static final List<String> RESIDENT_OPTIONS = List.of(KAIDA, DUOXIE, ZHUOYUE, YAOWANG);

    /**
     * 天庭 business options checked after a green tracker click starts no movement.
     *
     * <p>This is deliberately wider than {@link #RESIDENT_OPTIONS}. It is not a global resident set:
     * the sampler uses it only inside 天庭's bounded no-movement arbitration. 引妖香 stays here as a
     * fixed candidate because the dialog itself can block the green click; it must not depend on the
     * asynchronous Tracker OCR having classified that option first.</p>
     */
    private static final List<String> RECOVERY_OPTIONS =
            List.of(YINYAO, KAIDA, DUOXIE, ZHUOYUE, YAOWANG, FENGYAO, ACCEPT);
    /** Same recovery priority after the accepted-cycle 引妖香 latch has been consumed. */
    private static final List<String> RECOVERY_OPTIONS_NO_YINYAO =
            List.of(KAIDA, DUOXIE, ZHUOYUE, YAOWANG, FENGYAO, ACCEPT);

    private TiantingDialogLocalMechanics() {
    }

    /**
     * One matched option and where to click it.
     *
     * @param templatePath local image used for the match; never used as the business identity.
     * @param actionKey stable business identity shared with Cloud {@code TiantingDialogCatalog}.
     * @param roiOffsetX horizontal click point relative to the dialog ROI origin.
     * @param roiOffsetY vertical click point relative to the dialog ROI origin.
     * @param score raw match score, carried for threshold calibration from real runs.
     */
    record OptionHit(String templatePath, String actionKey, int roiOffsetX, int roiOffsetY, double score) {
    }

    /**
     * Resolve a local template to the stable business action carried across the observation boundary.
     *
     * @param templatePath exact repo-local template path; null and unknown paths are rejected.
     * @return stable action key, or empty when the template is outside the approved local options.
     */
    static Optional<String> actionKeyForTemplate(String templatePath) {
        return Optional.ofNullable(ACTION_KEYS_BY_TEMPLATE.get(templatePath));
    }

    /**
     * Match the resident options against one dialog frame.
     *
     * @param roi the dialog ROI crop; null yields no hit.
     * @return the first option that matches in priority order, or empty when none does — which is the
     *         signal to hand the frame to the cloud for the first-green-line fallback.
     */
    static Optional<OptionHit> matchResidentOption(BufferedImage roi) {
        return matchFirstOf(roi, RESIDENT_OPTIONS);
    }

    /**
     * Match all known 天庭 options after a tracker click started no movement.
     *
     * @param roi the dialog ROI crop; null yields no hit.
     * @return the first known option in stable business priority, or empty for the Cloud fallback.
     */
    static Optional<OptionHit> matchRecoveryOption(BufferedImage roi) {
        return matchFirstOf(roi, RECOVERY_OPTIONS);
    }

    /** Match normal 天庭 recovery options without rechecking 引妖香 in the same accepted cycle. */
    static Optional<OptionHit> matchRecoveryOptionWithoutYinyao(BufferedImage roi) {
        return matchFirstOf(roi, RECOVERY_OPTIONS_NO_YINYAO);
    }

    /**
     * Match 为民除害 on 李靖's dialog.
     *
     * <p>Its own entry rather than a resident option: the accept dialog only exists for the moment the
     * quest is taken, and answering it during a fight leg would mean answering a dialog the flow is
     * not in.</p>
     *
     * @param roi the dialog ROI crop; null yields no hit.
     * @return the accept option when present.
     */
    static Optional<OptionHit> matchAcceptOption(BufferedImage roi) {
        return matchFirstOf(roi, List.of(ACCEPT));
    }

    /**
     * Match 取消任务 only after Cloud has explicitly decided that the carried task must be cancelled.
     *
     * @param roi the dialog ROI crop; null yields no hit.
     * @return the cancel option when present; accept and every combat option remain ineligible.
     */
    static Optional<OptionHit> matchCancelOption(BufferedImage roi) {
        return matchFirstOf(roi, List.of(CANCEL));
    }

    /**
     * Match the only legal 鬼王 accept option using the same raw 640x300 dialog ROI as 天庭.
     *
     * @param roi exact-window dialog ROI crop; null yields no hit.
     * @return the 鬼王 accept option when present.
     */
    static Optional<OptionHit> matchGhostKingAcceptOption(BufferedImage roi) {
        return matchFirstOf(roi, List.of(GHOST_KING_ACCEPT));
    }

    /**
     * Match 使用引妖香, the post-combat option that has to be cleared before anything else runs.
     *
     * @param roi the dialog ROI crop; null yields no hit.
     * @return the 引妖 option when present.
     */
    static Optional<OptionHit> matchYinyaoOption(BufferedImage roi) {
        return matchFirstOf(roi, List.of(YINYAO));
    }

    /**
     * Match 使用封妖符, the option offered only in the short window that follows 多谢.
     *
     * <p>Kept off the resident set on purpose: matching it during an ordinary leg would open the
     * coordinate dialog in the middle of a flow that has nothing to do with it.</p>
     *
     * @param roi the dialog ROI crop; null yields no hit.
     * @return the 封妖 option when present.
     */
    static Optional<OptionHit> matchFengyaoOption(BufferedImage roi) {
        return matchFirstOf(roi, List.of(FENGYAO));
    }

    private static Optional<OptionHit> matchFirstOf(BufferedImage roi, List<String> templatePaths) {
        if (roi == null) {
            return Optional.empty();
        }
        String bestTemplate = null;
        double bestScore = 0.0d;
        for (String templatePath : templatePaths) {
            BufferedImage template = ImagePreprocessor.pathToBufferedImage(templatePath);
            if (template == null) {
                // A missing template must not silently promote the next option to this one's priority;
                // it simply cannot match, and the miss path ends at the cloud fallback.
                continue;
            }
            try {
                String evidenceSite = "tianting-dialog-"
                        + templatePath.substring(templatePath.lastIndexOf('/') + 1).replace(".png", "");
                double[] result = ImageFinder.find(roi, template, MATCH_RATE);
                // 用户铁律（2026-08-18 全量清扫）：模板匹配点落盘判定原图。
                MatchEvidenceStore.saveOnChange(evidenceSite, null, roi, template, result);
                if (result != null && result.length >= 3) {
                    String actionKey = ACTION_KEYS_BY_TEMPLATE.get(templatePath);
                    if (actionKey == null) {
                        continue;
                    }
                    return Optional.of(new OptionHit(
                            templatePath,
                            actionKey,
                            (int) Math.round(result[0]),
                            (int) Math.round(result[1]),
                            result[2]));
                }
                // Threshold 0 so a miss is a number rather than a null; only the best one is kept.
                double[] best = ImageFinder.find(roi, template, 0.0d);
                MatchEvidenceStore.saveOnChange(evidenceSite + "-score", null, roi, template, best);
                double score = best != null && best.length >= 3 ? best[2] : 0.0d;
                if (score > bestScore) {
                    bestScore = score;
                    bestTemplate = templatePath;
                }
            } finally {
                template.flush();
            }
        }
        /*
         * Nothing matched, and the ROI is about to be flushed. Keep it: a miss reported as an empty
         * Optional cannot be told apart from a wrong template, a wrong ROI, or a frame sampled before the
         * dialog had drawn — and those three need completely different fixes. The image answers it at a
         * glance. Bounded, so a long run cannot fill the disk.
         */
        DialogMatchMissDump.write(roi, bestTemplate, bestScore);
        return Optional.empty();
    }
}
