package com.bot.dhxy.service;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.PlayerCharacter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 🤖 角色状态同步中枢
 * 负责调用各个视觉/底层雷达，并将结果写入全局记忆 (GameContext)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStateService {

    private final GameContext context;
    private final ClientIdentityService identityService;
    private final LocationVisionService locationRadar;

    /**
     * 同步我是谁：读取底层窗口标题获取身份
     */
    public void syncMyIdentity() {
        log.info("🤖 [状态中枢] 请求读取角色档案...");
        PlayerCharacter me = context.getMe();
        identityService.scanAndSyncIdentity(me);
        log.info("📋 当前上线角色: {}", me.toString());
    }

    /**
     * 同步我在哪：通过 OCR 雷达扫描左上角坐标
     */
    public void syncMyPosition() {
        log.info("🤖 [状态中枢] 请求雷达扫描当前位置...");
        TextRecognizer.LocationInfo info = locationRadar.scanCurrentLocation();

        if (info != null) {
            PlayerCharacter me = context.getMe();
            me.setCurrentMapName(info.mapName);
            me.setX(info.x);
            me.setY(info.y);
            log.info("🔄 全局记忆已更新: {}", me.toString());
        } else {
            log.warn("⚠️ [状态中枢] 雷达未能看清当前位置，记忆未更新。");
        }
    }

    /**
     * 一键全量同步（方便在脚本刚启动时调用）
     */
    public void syncAll() {
        syncMyIdentity();
        syncMyPosition();
    }
}