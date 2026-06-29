package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for 五倍 task-kind recognition.
 *
 * <p>五倍 task kind must come from raw yellow title templates only. OCR/yellow-text may still be
 * kept as diagnostic or target-name evidence, but it must not decide 暗雷/白龙马/黄袍 business
 * branches.</p>
 */
public final class WubeiTrackerTaskKindTemplateWiringTest {

    private WubeiTrackerTaskKindTemplateWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String service = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java"), StandardCharsets.UTF_8);
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        serviceUsesRawYellowTitleTemplates(service);
        serviceGreenLinkPolicyUsesTemplateTaskKind(service);
        wubeiTaskBranchesUseTemplateTaskKind(wubei);

        System.out.println("WubeiTrackerTaskKindTemplateWiringTest passed");
    }

    private static void serviceUsesRawYellowTitleTemplates(String service) {
        require(service.contains("public static final String WUBEI_TASK_KEY_SANCANG_FENGMO"),
                "五倍 task key constants must be public so task code can use canonical template keys");
        require(service.contains("wubei_title_sancang_fengmo_yellow.png"),
                "三藏封魔 must use the raw yellow title template");
        require(service.contains("wubei_title_baoxiang_miqing_yellow.png"),
                "宝象谜情 must use the raw yellow title template");
        require(service.contains("wubei_title_dianqian_xianyi_yellow.png"),
                "殿前献艺 must use the raw yellow title template");
        require(service.contains("wubei_title_zhidou_huangpao_yellow.png"),
                "智斗黄袍 must use the raw yellow title template");
        require(service.contains("wubei_title_kuixing_guiwei_yellow.png"),
                "魁星归位 must use the raw yellow title template");

        String liveRead = methodBody(service, "public TaskTrackerPanelReadResult readWubeiTrackerPanel(");
        require(liveRead.contains("cropTaskDetailInTrackerPanel(source, WUBEI_TRACKER_TITLE_TEMPLATES, false)"),
                "live 五倍 title matching must use the raw panel image, not yellow washing");

        String snapshotRead = methodBody(service, "public TaskTrackerPanelReadResult readWubeiTrackerPanelFromSnapshot(");
        require(!snapshotRead.contains("washYellowText"),
                "accept-time 五倍 snapshot title matching must not wash yellow text");
        require(snapshotRead.contains("findTitlePointInPanelImage(source, windowSnapshotPath.toString(),")
                        && snapshotRead.contains("windowSnapshotPath.toString(), absoluteLeft, absoluteTop"),
                "accept-time 五倍 snapshot title matching must use the raw snapshot image as match source");
    }

    private static void serviceGreenLinkPolicyUsesTemplateTaskKind(String service) {
        String detail = methodBody(service, "private TaskTrackerPanelReadResult readWubeiTrackerDetail(");
        require(detail.contains("scanWubeiTrackerGreenLinks(\n                image, absoluteLeft, absoluteTop, safeSource, titleTemplate)"),
                "五倍 green-link policy must receive the matched title template, not OCR yellow text");
        require(!detail.contains("scanWubeiTrackerGreenLinks(\n                image, absoluteLeft, absoluteTop, safeSource, yellowText)"),
                "五倍 green-link policy must not branch on yellow OCR text");

        String scan = methodBody(service, "private WubeiGreenLinkScan scanWubeiTrackerGreenLinks(");
        require(scan.contains("isWubeiDarkThunderTaskKey("),
                "暗雷 green-link policy must use template task key");
        require(scan.contains("isWubeiMirrorProbeTaskKey("),
                "白龙马/显形镜 green-link policy must use template task key");
        require(!service.contains("isWubeiDarkThunderYellowText("),
                "暗雷 classification helper must not use OCR yellow text");
        require(!service.contains("isWubeiMirrorProbeYellowText("),
                "白龙马/显形镜 classification helper must not use OCR yellow text");
    }

    private static void wubeiTaskBranchesUseTemplateTaskKind(String wubei) {
        String readTracker = methodBody(wubei, "private WubeiStepOutcome runReadTrackerPhase(");
        require(readTracker.contains("isTrackerDarkThunderTask(currentTrackerPanel)"),
                "READ_TRACKER must detect 暗雷怪 from template task key");
        require(readTracker.contains("currentRoundChainedCombatExpected = isTrackerChainedCombatTask(currentTrackerPanel)"),
                "READ_TRACKER must detect 黄袍连战 from template task key");
        require(readTracker.contains("if (isTrackerProbeTask(currentTrackerPanel))"),
                "READ_TRACKER must detect 白龙马/显形镜 from template task key");
        require(!readTracker.contains("containsDarkThunder(currentTrackerPanel.getYellowText())"),
                "READ_TRACKER must not detect 暗雷怪 from OCR yellow text");
        require(!readTracker.contains("containsChainedCombatTarget(currentTrackerPanel.getYellowText())"),
                "READ_TRACKER must not detect 黄袍 from OCR yellow text");
        require(!readTracker.contains("containsProbeTask(currentTrackerPanel.getYellowText())"),
                "READ_TRACKER must not detect 白龙马/显形镜 from OCR yellow text");
        require(!readTracker.contains("currentTrackerPanel.isProbeObjective()"),
                "READ_TRACKER must not use green-link shape as task-kind fallback");

        String pathing = methodBody(wubei, "private WubeiStepOutcome runTrackerPathingPhase(");
        require(pathing.contains("if (isTrackerProbeTask(currentTrackerPanel))"),
                "TRACKER_PATHING must route 白龙马/显形镜 by template task key");
        require(!pathing.contains("containsProbeTask(currentTrackerPanel.getYellowText())"),
                "TRACKER_PATHING must not route 白龙马/显形镜 by OCR yellow text");
        require(!pathing.contains("currentTrackerPanel.isProbeObjective()"),
                "TRACKER_PATHING must not route 白龙马/显形镜 by shape fallback");

        require(wubei.contains("boolean stillChained = isTrackerChainedCombatTask(postCombatPanel)"),
                "post-combat 黄袍 continuation must use template task key");
        require(!wubei.contains("containsChainedCombatTarget(postCombatPanel.getYellowText())"),
                "post-combat 黄袍 continuation must not use OCR yellow text");
    }

    private static String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0) {
            throw new AssertionError("Missing method signature: " + signature);
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        if (bodyStart < 0) {
            throw new AssertionError("Missing method body for: " + signature);
        }
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body for: " + signature);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
