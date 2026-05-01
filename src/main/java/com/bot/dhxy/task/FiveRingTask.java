package com.bot.dhxy.task;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.service.ClientIdentityService;
import com.bot.dhxy.service.LocationVisionService;
import com.bot.dhxy.service.NavigationService; // 🌟 引入导航系统
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.PlayerCharacter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FiveRingTask {

    private final GameContext context;
    private final LocationVisionService locationRadar;
    private final ClientIdentityService identityService;
    private final NavigationService navigationService; // 🌟 注入导航系统

    private final String targetMapName = "长安";
    private final int npc_coor_x = 87;
    private final int npc_coor_y = 174;

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

        // 🌟 3. 把跑地图的脏活累活全丢给导航系统！
        // 主控逻辑瞬间变得极其干净漂亮
        boolean arrived = navigationService.navigateToMap(targetMapName);

        if (!arrived) {
            System.out.println("❌ 任务中止：无法到达任务起始点！");
            return;
        }

        System.out.println("⚔️ 到达指定地点，开始执行五环具体任务逻辑...");
        // TODO: 接任务、找NPC、打怪等具体业务逻辑...
    }
}