package com.bot.dhxy.task.wubei;

import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;

import java.util.List;

/** The 696a12b0 enter-battle portion of the shared 五倍 dialog catalog. */
public final class WubeiDialogCatalog {
    public static final String TASK_CODE = "wubei";
    public static final String OPTION_ENTER_BATTLE = "wubei.enterBattle";
    public static final String OPTION_ENTER_BATTLE_PROVE = "wubei.enterBattle.prove";
    public static final String OPTION_ENTER_BATTLE_KUIXING = "wubei.enterBattle.kuixing";
    public static final String ENTER_BATTLE_TEMPLATE =
            "images/template/dialog/wubei/wubei_enter_battle_xiaomie.png";
    public static final String ENTER_BATTLE_PROVE_TEMPLATE =
            "images/template/dialog/wubei/wubei_enter_battle_zhengming.png";
    public static final String ENTER_BATTLE_KUIXING_TEMPLATE =
            "images/template/dialog/wubei/wubei_enter_battle_kuixing.png";

    private WubeiDialogCatalog() {
    }

    public static List<GreenTemplateClickSpec> enterBattleSpecs() {
        return List.of(
                new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 18, 4),
                new GreenTemplateClickSpec(OPTION_ENTER_BATTLE_PROVE, ENTER_BATTLE_PROVE_TEMPLATE, -6, 18, 4),
                new GreenTemplateClickSpec(OPTION_ENTER_BATTLE_KUIXING, ENTER_BATTLE_KUIXING_TEMPLATE, -6, 18, 4));
    }
}
