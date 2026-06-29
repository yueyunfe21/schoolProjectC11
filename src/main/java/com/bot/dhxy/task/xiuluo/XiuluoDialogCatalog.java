package com.bot.dhxy.task.xiuluo;

import com.bot.dhxy.model.dialog.GreenTemplateClickSpec;

import java.util.List;

/**
 * 修罗-owned dialog templates shared by the foreground task and watcher preparation provider.
 */
public final class XiuluoDialogCatalog {
    public static final String TASK_CODE = "xiuluo_v2";
    public static final String OPTION_ENTER_BATTLE = "xiuluo.enterBattle";
    public static final String ENTER_BATTLE_TEMPLATE = "images/template/dialog/xiuluo/xiuluo_enter_battle_kanda.png";

    private XiuluoDialogCatalog() {
    }

    public static List<GreenTemplateClickSpec> enterBattleSpecs() {
        return List.of(new GreenTemplateClickSpec(OPTION_ENTER_BATTLE, ENTER_BATTLE_TEMPLATE, -6, 6, 4));
    }
}
