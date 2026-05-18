package com.bot.dhxy.service;

import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.tools.CoordinateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

/**
 * 🤖 角色状态同步中枢
 * 负责调用各个视觉/底层雷达，并将结果写入全局记忆 (GameContext)，
 * 以及负责非战斗状态下的自动急救（加血加蓝）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStateService {

    // ==========================================
    // 💉 原有依赖注入
    // ==========================================
    private final GameContext context;
    private final ClientIdentityService identityService;
    private final LocationVisionService locationRadar;

    // ==========================================
    // 💉 加血模块新增依赖注入
    // ==========================================
    private final GameClientTracker tracker;
    private final InputProvider inputProvider;
    private final CoordinateHelper coordinateHelper;
    private final BagService bagService;

    // ==========================================
    // ⏱️ 摄妖香时间锁配置
    // ==========================================
    private long lastIncenseUsedTime = 0; // 记录上一次吃香的时间戳 (默认0，保证第一次必查)
    // 设定香的有效期：60分钟 = 3600000 毫秒。为了保险，我们设为 59 分钟 (3540000 毫秒) 提前补香
    private static final long INCENSE_DURATION_MS = 59 * 60 * 1000L;

    // ==========================================
    // ⚙️ 自动急救配置区 (未来可从 UI 界面读取)
    // ==========================================
    private int currentThreshold = 70;         // 用户设置的警戒线：默认 70 (%)。可选 30, 50, 70
    private int maxChecksBetweenBattles = 1;   // 两次战斗之间，最多允许查几次血？默认 1 次
    private int checksDoneThisRound = 0;       // 🔒 当前空闲轮次中，已经查过的次数
    // 🌟 新增：记录上一次脱离战斗的精确时间
    private long lastCombatExitTime = 0;

    private static final int HEAL_TIME_INTERVAL = 5000;  //战斗过后多少秒加血

    // ==========================================
    // 📍 坐标量尺 (只需量取状态槽的最左侧和最右侧的 X 坐标)
    // ==========================================
    // 人物状态槽的 X 边界 (示例值，请根据实际截图填写)
    private static final int CHAR_BAR_LEFT_X = 949;  // 人物血条/蓝条【最左端】的 X 坐标
    private static final int CHAR_BAR_RIGHT_X = 1020; // 人物血条/蓝条【最右端】的 X 坐标

    // 宝宝状态槽的 X 边界 (示例值，请根据实际截图填写)
    private static final int PET_BAR_LEFT_X = 823;   // 宝宝血条/蓝条【最左端】的 X 坐标
    private static final int PET_BAR_RIGHT_X = 876;  // 宝宝血条/蓝条【最右端】的 X 坐标

    // Y 坐标：人物和宝宝的红蓝槽高度 Y 坐标 (无需改变，还是量一条线的 Y)
    private static final int BAR_HP_Y = 85; // 第二条：红槽的 Y 坐标
    private static final int BAR_MP_Y = 101; // 第三条：蓝槽的 Y 坐标

    // 状态面板区域 (用于检测是否开启了状态页)
    private static final int STATUS_PANEL_X = 951;
    private static final int STATUS_PANEL_Y = 119;
    private static final int STATUS_PANEL_W = 79;
    private static final int STATUS_PANEL_H = 39;

    // =========================================================================
    // 🤖 原有核心业务：状态与坐标同步
    // =========================================================================

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


    // =========================================================================
    // 🩺 新增核心业务：极简顺滑急救系统 (加血加蓝)
    // =========================================================================

    /**
     * 🔓 解锁方法：每次战斗结束时，由战斗管家调用，重置检查次数
     */
    public void resetCheckCounter() {
        this.checksDoneThisRound = 0;
        this.lastCombatExitTime = System.currentTimeMillis();
        log.info("🔄 战斗结束，急救检查计数器已重置，准备进行战后体检！");
    }

    /**
     * 🩺 终极一键体检：按顺序查 人血 -> 人蓝 -> 兽血 -> 兽蓝
     */
    public void performFirstAidCheck() {
        // 1. 检查有没有超过设定的检查次数
        if (checksDoneThisRound >= maxChecksBetweenBattles) {
            return; // 已经查过了，直接无视，专心跑路
        }

        if (tracker.getWindowBaseX() == -1) return;

        // 🌟🌟🌟 核心防发呆机制：非阻塞式校验！
        // 如果距离脱离战斗还不到 5 秒（黑屏期），直接 return 放行！
        // 这样主线程瞬间通过，机器人马上就可以去寻路、跑动！
        if (System.currentTimeMillis() - lastCombatExitTime < HEAL_TIME_INTERVAL) {
            return;
        }

        log.info("🩺 开始执行战后体检 (当前设定阈值: {}%)...", currentThreshold);

        // 2. 执行全量加血
        healAll();

        // 3. 查完一次，计数器加 1，锁死直到下一次战斗结束
        checksDoneThisRound++;
        log.info("✅ 本轮体检结束。当前空闲期已查次数: {}/{}", checksDoneThisRound, maxChecksBetweenBattles);
    }

    /**
     * 🩸 为人物和宝宝执行全量检查与治疗
     */
    public void healAll() {
        healPlayer();
        healPet();
    }

    /**
     * 👤 仅检查并治疗人物 HP/MP
     */
    public void healPlayer() {
        int charX = calculateX(CHAR_BAR_LEFT_X, CHAR_BAR_RIGHT_X, currentThreshold);
        checkAndHeal("人物血量", charX, BAR_HP_Y, true);
        checkAndHeal("人物法力", charX, BAR_MP_Y, false);
    }

    /**
     * 🐾 仅检查并治疗宝宝 HP/MP
     */
    public void healPet() {
        int petX = calculateX(PET_BAR_LEFT_X, PET_BAR_RIGHT_X, currentThreshold);
        checkAndHeal("宝宝血量", petX, BAR_HP_Y, true);
        checkAndHeal("宝宝法力", petX, BAR_MP_Y, false);
    }


    /**
     * 🕯️ 摄妖香智能管控 (自带 1 小时防抖怀表)
     */
    public void ensureSheYaoXiangActive() {
        // 1. 查怀表：如果距离上次吃香还没到 59 分钟，直接瞬间放行，不消耗任何 CPU！
        if (System.currentTimeMillis() - lastIncenseUsedTime < INCENSE_DURATION_MS) {
            return;
        }

        log.info("🕯️ 摄妖香疑似过期 (或脚本首次启动)，开始执行安全校验...");

        // 2. 双重保险：扫一眼左上角状态栏，看看图标在不在？(需截取摄妖香Buff图标)
        int[] statusRect = coordinateHelper.getScaledRect(STATUS_PANEL_X, STATUS_PANEL_Y, STATUS_PANEL_W, STATUS_PANEL_H);
        // ⚠️ 请自己截一张左上角摄妖香状态的小图标，放到 images/template/status/ 下
        java.awt.Point buffIcon = coordinateHelper.findImageInRegion(
                "images/template/status/sheyaoxiang_buff.png", statusRect, 0.85);

        if (buffIcon != null) {
            log.info("✅ 发现摄妖香状态图标还在！更新怀表计时器！");
            lastIncenseUsedTime = System.currentTimeMillis();
            return;
        }

        // 3. 香确实没了，呼叫包裹引擎去吃香！
        log.warn("⚠️ 未发现摄妖香状态，准备打开包裹补充...");
        // ⚠️ 请截一张包裹里摄妖香物品的图标，放到 images/template/item/ 下
        boolean used = bagService.findAndUseItem(
                BagService.MAIN_BAG, "item/sheyaoxiang_item.png", null);

        if (used) {
            log.info("✅ 成功使用摄妖香！怀表已重置为 1 小时！");
            lastIncenseUsedTime = System.currentTimeMillis();
            // 强行休眠 1 秒，等待人物头顶冒出吃香动画
            try { Thread.sleep(1000); } catch (Exception ignored) {}
        } else {
            log.error("❌ 包裹内未找到摄妖香！请及时购买补充！");
            // 惩罚机制：没找到香，就把怀表拨回到 58 分钟前。这样它 1 分钟后才会再试，防止疯狂开包死循环！
            lastIncenseUsedTime = System.currentTimeMillis() - INCENSE_DURATION_MS + 60000;
        }
    }

    /**
     * 🔫 核心动作：看一眼，没满就原位右键并等待
     */
    public boolean checkAndHeal(String name, int relX, int relY, boolean expectRed) {
        // 1. 🌟 获取绝对坐标：直接调用您的 getScaledRect
        // 您的底层已经自动加上了 WindowBaseX/Y，所以返回的就是完美的屏幕逻辑绝对坐标！
        int[] absoluteRect = coordinateHelper.getScaledRect(relX, relY, 1, 1);
        int absX = absoluteRect[0];
        int absY = absoluteRect[1];

        // 2. 内存截图：拿绝对坐标截取这个像素点
        BufferedImage pixelImg = tracker.captureToMemory(name, absX, absY, absX + 1, absY + 1);
        if (pixelImg == null) return false;

        int rgb = pixelImg.getRGB(0, 0);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // 3. 颜色判定
        boolean isHealthy;
        if (expectRed) {
            // 血条是偏橙红色，红色通道明显最高
            isHealthy = (r > 150) && (r > g + 80) && (r > b + 80);
        } else {
            // 蓝条实际是青蓝色，不是纯蓝色
            // 所以不能要求 b 必须远大于 g，只要蓝/绿都亮，且红色明显低即可
            isHealthy = (b > 150) && (g > 120) && (b > r + 80);
        }

        if (!isHealthy) {
            log.warn("🚨 警报！[{}] 未达 {}% 警戒线，执行原位右键补充！", name, currentThreshold);
            // 4. 🌟 执行点击：
            // 直接传逻辑绝对坐标！您的 WinApiMouseController 底层会自己去乘 scale 转成物理坐标！
            inputProvider.clickRight(absX, absY, 100);

            try { Thread.sleep(800); } catch (Exception ignored) {}
            return true;
        }

        return false;
    }

    /**
     * 🧮 动态坐标计算引擎
     * 根据状态槽的最左侧、最右侧和百分比，计算出精确的像素点 X 坐标
     */
    private int calculateX(int leftX, int rightX, int threshold) {
        // 公式：起点 + 槽位总长度 * (百分比 / 100)
        double ratio = threshold / 100.0;
        int targetX = leftX + (int) Math.round((rightX - leftX) * ratio);
        log.debug("🧮 坐标计算：左界{} 右界{} 阈值{}% -> 目标X坐标:{}", leftX, rightX, threshold, targetX);
        return targetX;
    }
}
