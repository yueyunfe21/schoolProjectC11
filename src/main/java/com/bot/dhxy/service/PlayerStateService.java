package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStateService {

    private final GameContext context;
    private final ClientIdentityService identityService;
    private final LocationVisionService locationRadar;
    private final GameClientTracker tracker;
    private final InputSequences inputSequences;
    private final CoordinateHelper coordinateHelper;
    private final BagService bagService;

    private long lastIncenseUsedTime = 0;
    private static final long INCENSE_DURATION_MS = 59 * 60 * 1000L;

    private int currentThreshold = 70;
    private int maxChecksBetweenBattles = 1;
    private int checksDoneThisRound = 0;
    private long lastCombatExitTime = 0;

    private static final int HEAL_TIME_INTERVAL = 5000;

    private static final int CHAR_BAR_LEFT_X = 949;
    private static final int CHAR_BAR_RIGHT_X = 1020;
    private static final int PET_BAR_LEFT_X = 823;
    private static final int PET_BAR_RIGHT_X = 876;

    private static final int BAR_HP_Y = 85;
    private static final int BAR_MP_Y = 101;

    private static final int STATUS_PANEL_X = 951;
    private static final int STATUS_PANEL_Y = 119;
    private static final int STATUS_PANEL_W = 79;
    private static final int STATUS_PANEL_H = 39;

    public void syncMyIdentity() {
        log.info("🤖 [状态中枢] 请求读取角色档案...");
        PlayerCharacter me = context.getMe();
        identityService.scanAndSyncIdentity(me);
        log.info("📋 当前上线角色: {}", me.toString());
    }

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

    public void syncAll() {
        syncMyIdentity();
        syncMyPosition();
    }

    public void resetCheckCounter() {
        this.checksDoneThisRound = 0;
        this.lastCombatExitTime = System.currentTimeMillis();
        log.info("🔄 战斗结束，急救检查计数器已重置，准备进行战后体检！");
    }

    public void performFirstAidCheck() {
        if (checksDoneThisRound >= maxChecksBetweenBattles) {
            return;
        }

        if (tracker.getWindowBaseX() == -1) return;

        if (System.currentTimeMillis() - lastCombatExitTime < HEAL_TIME_INTERVAL) {
            return;
        }

        log.info("🩺 开始执行战后体检 (当前设定阈值: {}%)...", currentThreshold);
        healAll();
        checksDoneThisRound++;
        log.info("✅ 本轮体检结束。当前空闲期已查次数: {}/{}", checksDoneThisRound, maxChecksBetweenBattles);
    }

    public void healAll() {
        healPlayer();
        healPet();
    }

    public void healPlayer() {
        int charX = calculateX(CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X, currentThreshold);
        checkAndHeal("人物血量", charX, BAR_HP_Y, true);
        checkAndHeal("人物法力", charX, BAR_MP_Y, false);
    }

    public void healPet() {
        int petX = calculateX(PET_BAR_LEFT_X, PET_BAR_RIGHT_X, currentThreshold);
        checkAndHeal("宝宝血量", petX, BAR_HP_Y, true);
        checkAndHeal("宝宝法力", petX, BAR_MP_Y, false);
    }

    public void ensureSheYaoXiangActive() {
        if (System.currentTimeMillis() - lastIncenseUsedTime < INCENSE_DURATION_MS) {
            log.info("🕯️ 摄妖香怀表未过期，跳过包裹检查。");
            return;
        }

        log.info("🕯️ 摄妖香疑似过期或首次启动，开始执行安全校验...");

        int[] statusRect = coordinateHelper.getScaledRect(STATUS_PANEL_X, STATUS_PANEL_Y, STATUS_PANEL_W, STATUS_PANEL_H);
        java.awt.Point buffIcon = coordinateHelper.findImageInRegion(
                "images/template/status/sheyaoxiang_buff.png", statusRect, 0.85);

        if (buffIcon != null) {
            log.info("✅ 发现摄妖香状态图标还在，更新怀表计时器。point=({}, {})", buffIcon.x, buffIcon.y);
            lastIncenseUsedTime = System.currentTimeMillis();
            return;
        }

        log.warn("⚠️ 未发现摄妖香状态，准备打开包裹补充...");
        boolean used = bagService.findAndUseItem(
                BagService.MAIN_BAG, "item/sheyaoxiang_item.png", null);

        if (used) {
            log.info("✅ 成功使用摄妖香，怀表已重置为 1 小时。等待吃香动画...");
            lastIncenseUsedTime = System.currentTimeMillis();
            sleepQuietly(1000);
        } else {
            log.error("❌ 包裹内未找到摄妖香，请及时购买补充。1 分钟后才会再试。 ");
            lastIncenseUsedTime = System.currentTimeMillis() - INCENSE_DURATION_MS + 60000;
        }
    }

    public boolean checkAndHeal(String name, int relX, int relY, boolean expectRed) {
        int[] absoluteRect = coordinateHelper.getScaledRect(relX, relY, 1, 1);
        int absX = absoluteRect[0];
        int absY = absoluteRect[1];

        BufferedImage pixelImg = tracker.captureToMemory(name, absX, absY, absX + 1, absY + 1);
        if (pixelImg == null) return false;

        int rgb = pixelImg.getRGB(0, 0);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        pixelImg.flush();

        boolean isHealthy;
        if (expectRed) {
            isHealthy = (r > 150) && (r > g + 80) && (r > b + 80);
        } else {
            isHealthy = (b > 150) && (g > 120) && (b > r + 80);
        }

        if (!isHealthy) {
            log.warn("🚨 警报！[{}] 未达 {}% 警戒线，执行原位右键补充！rgb=({}, {}, {})",
                    name, currentThreshold, r, g, b);
            inputSequences.submitAndWait("playerState:heal:" + name, List.of(
                    InputAction.clickRight(absX, absY, 100),
                    InputAction.sleep(800)
            ));
            return true;
        }

        return false;
    }

    private int calculateX(int leftX, int rightX, int threshold) {
        double ratio = threshold / 100.0;
        int targetX = leftX + (int) Math.round((rightX - leftX) * ratio);
        log.debug("🧮 坐标计算：左界{} 右界{} 阈值{}% -> 目标X坐标:{}", leftX, rightX, threshold, targetX);
        return targetX;
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
