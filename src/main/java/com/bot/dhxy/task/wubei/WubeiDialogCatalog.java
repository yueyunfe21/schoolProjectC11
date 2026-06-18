package com.bot.dhxy.task.wubei;

import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;
import com.bot.dhxy.model.dialog.WhiteTemplateSpec;

import java.util.List;

/**
 * Shared 五倍 dialog templates and action keys used by both the watcher and the task.
 *
 * <p>The watcher prepares these dialogs in the background, while {@link WubeiTask} only consumes
 * the prepared action. Keeping the catalog in one place prevents the Runner and task from drifting
 * on template paths or click-offset policy.</p>
 */
public final class WubeiDialogCatalog {

    public static final String TASK_CODE = "wubei";
    public static final String ACCEPT_NPC_NAME = "降魔侍卫";

    public static final String OPTION_ACCEPT_TASK = "wubei.acceptTask";
    public static final String OPTION_ENTER_BATTLE = "wubei.enterBattle";
    public static final String OPTION_ENTER_BATTLE_PROVE = "wubei.enterBattle.prove";
    public static final String OPTION_ENTER_BATTLE_KUIXING = "wubei.enterBattle.kuixing";
    public static final String STORY_PROBE_TARGET_READY = "wubei.probeTargetReady";
    public static final String STORY_PROBE_WRONG_POSITION = "wubei.probeWrongPosition";
    public static final String STORY_PROBE_NO_TARGET = "wubei.probeNoTarget";

    public static final String ACCEPT_OPTION_TEMPLATE =
            "images/template/dialog/wubei/wubei_accept_chumoweiguo.png";
    public static final String ENTER_BATTLE_TEMPLATE =
            "images/template/dialog/wubei/wubei_enter_battle_xiaomie.png";
    public static final String ENTER_BATTLE_PROVE_TEMPLATE =
            "images/template/dialog/wubei/wubei_enter_battle_zhengming.png";
    public static final String ENTER_BATTLE_KUIXING_TEMPLATE =
            "images/template/dialog/wubei/wubei_enter_battle_kuixing.png";
    public static final String PROBE_STORY_TEMPLATE =
            "images/template/dialog/wubei/wubei_probe_story_koukou.png";
    public static final String PROBE_WRONG_POSITION_TEMPLATE =
            "images/template/dialog/wubei/wubei_probe_story_wrong_position.png";

    private WubeiDialogCatalog() {
    }

    public static List<GreenTemplateClickSpec> acceptTaskSpecs() {
        return List.of(new GreenTemplateClickSpec(
                OPTION_ACCEPT_TASK,
                ACCEPT_OPTION_TEMPLATE,
                32,
                78,
                3));
    }

    public static List<GreenTemplateClickSpec> enterBattleSpecs() {
        return List.of(
                new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 18, 4),
                new GreenTemplateClickSpec(OPTION_ENTER_BATTLE_PROVE, ENTER_BATTLE_PROVE_TEMPLATE, -6, 18, 4),
                new GreenTemplateClickSpec(OPTION_ENTER_BATTLE_KUIXING, ENTER_BATTLE_KUIXING_TEMPLATE, -6, 18, 4));
    }

    public static List<WhiteTemplateSpec> probeStorySpecs() {
        return List.of(
                new WhiteTemplateSpec(STORY_PROBE_TARGET_READY, PROBE_STORY_TEMPLATE),
                new WhiteTemplateSpec(STORY_PROBE_WRONG_POSITION, PROBE_WRONG_POSITION_TEMPLATE));
    }
}
