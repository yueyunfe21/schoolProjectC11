package com.bot.dhxy.service;

import com.bot.dhxy.config.InputProvider;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.awt.Point;

/**
 * 🎁 物品给予业务流程引擎
 * 只负责业务流转，把找物品这种体力活全部丢给强悍的 BagService！
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GiveItemService {

    private final InputProvider inputProvider;
    private final CoordinateHelper coordinateHelper;
    private final BagService bagService; // 🌟 注入万能包裹引擎

    private static final String BTN_GIVE_TEMPLATE = "images/template/300huan/btn_give.png";

    public boolean executeGive(String targetItemTemplate, Integer knownBagIndex) {
        log.info("🎁 [给予流程] 界面已就绪，呼叫底层包裹引擎搜索: [{}]", targetItemTemplate);
        sleep(800);

        // 🌟 1. 甩锅给 BagService，使用 GIVE_BAG 图纸去寻找并点击物品！
        boolean itemSelected = bagService.findAndSelectItem(BagService.GIVE_BAG, targetItemTemplate, knownBagIndex);

        if (!itemSelected) {
            log.error("❌ [给予流程] 包裹引擎未能找到指定物品，交易中止！");
            return false;
        }

        // 🌟 2. 物品选中后，由本服务执行最后的“给予”按钮点击
        return clickGiveButton();
    }

    private boolean clickGiveButton() {
        Point btnGivePoint = coordinateHelper.findImageAbsoluteCoordinate(BTN_GIVE_TEMPLATE, 0.85);
        if (btnGivePoint == null) {
            log.error("❌ [给予流程] 没找到右下角的【给予】按钮图！");
            return false;
        }

        Point safeBtnClick = coordinateHelper.getRandomizedPoint(btnGivePoint, 20, 8);
        log.info("🖱️ [给予流程] 锁定给予按钮，执行最终交易！");
        inputProvider.clickLeft(safeBtnClick.x, safeBtnClick.y, 100);
        sleep(1000);
        log.info("✅ [给予流程] 交易完成，界面已关闭！");
        return true;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception ignored) {} }
}