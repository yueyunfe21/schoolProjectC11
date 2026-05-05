package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.service.*;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.PlayerCharacter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.bot.dhxy.service.AutoGridCalibrator; // 🌟 新增导入雷达

@Component
@RequiredArgsConstructor
@Slf4j
public class FiveRingTask {

    private final GameContext context;
    private final LocationVisionService locationRadar;
    private final ClientIdentityService identityService;
    private final NavigationService navigationService; // 🌟 注入导航系统
    private final NpcClickService npcClickService;    // 🌟 1. 注入我们的全自动测绘雷达！
    private final AutoGridCalibrator autoGridCalibrator;
    private final DialogService dialogService;

    private static final int DIALOG_RECT_OFFSET_X = 250;
    private static final int DIALOG_RECT_OFFSET_Y = 285;
    private static final int DIALOG_RECT_WIDTH = 532;
    private static final int DIALOG_RECT_HEIGHT = 144;

    private static final int DIALOG_OFFSET_X = 450;
    private static final int DIALOG__OFFSET_Y = 360;


    private final String targetMapName = "长安";
    private final String targetNPCName = "墨意";
    private final int npc_coor_x = 87;
    private final int npc_coor_y = 174;

    private static final int TUNE_X = -10; // 👈 既然你发现第一炮靠右，这里填负数，强行把准星往左拉15像素！(数值可自己微调)
    private static final int TUNE_Y = 0;

    public void syncMyIdentity() {
        System.out.println("🤖 [任务大脑] 请求读取角色档案...");
        PlayerCharacter me = context.getMe();
        identityService.scanAndSyncIdentity(me);
        System.out.println("📋 当前上线角色: " + me.toString());
    }

    public void syncMyPosition() {
        System.out.println("🤖 [任务大脑] 请求雷达扫描当前位置...");
        TextRecognizer.LocationInfo info = locationRadar.scanCurrentLocation();
        if (info != null) {
            PlayerCharacter me = context.getMe();
            me.setCurrentMapName(info.mapName);
            me.setX(info.x);
            me.setY(info.y);
            System.out.println("🔄 全局记忆已更新: " + me.toString());
        }
    }

    public void execute() {
        // 1. 上线先对身份对账
        syncMyIdentity();
        // 2. 看一眼自己在哪
        syncMyPosition();

        //autoGridCalibrator.startAutoCalibration(context.getMe());

        // 🌟 3. 把跑地图的脏活累活全丢给导航系统！
        // 主控逻辑瞬间变得极其干净漂亮
        boolean arrived = navigationService.navigateToNPC(targetMapName, npc_coor_x, npc_coor_y);
//
        if (!arrived) {
            System.out.println("❌ 任务中止：无法到达任务起始点！");
            return;
        }
        System.out.println("⚔️ 到达指定地点，开始执行五环具体任务逻辑...");

        //npcClickService.clickNpcSmart(context.getMe(), targetMapName, npc_coor_x, npc_coor_y,targetNPCName,
                //TUNE_X, TUNE_Y);

        //dialogService.acceptTaskByFixedCoordinates(DIALOG_OFFSET_X, DIALOG__OFFSET_Y);

        // TODO: 接任务、找NPC、打怪等具体业务逻辑...
    }
}