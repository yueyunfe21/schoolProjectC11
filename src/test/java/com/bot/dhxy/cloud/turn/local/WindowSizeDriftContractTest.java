package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.window.model.WindowNativeBinding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 窗口尺寸漂移诊断的合同。
 *
 * <p>事故：2026-08-30 01:55 游戏客户端自行把五个窗口从 1036x783 改成 1117x840（位置未动、进程未重启、
 * Windows 事件日志无任何显示/GPU/驱动/电源事件），两小时后又自行改了回来。全仓定尺寸模板一起失配，最先撞上
 * 的是队伍面板的队长按钮探测，五窗全失败、整批不启动。当时给用户的唯一信息是"本地队伍菜单在 5 秒内未命中
 * 队长按钮"——指向队伍面板，与真因无关，最后靠翻落盘证据图再从 PNG 尺寸反推窗口大小才定案。</p>
 *
 * <p>这些合同钉住的是：尺寸这个免费就能量到的量必须被量、必须报出来、而且不能误报。</p>
 */
class WindowSizeDriftContractTest {

    private static WindowNativeBinding window(String handle, int width, int height) {
        return new WindowNativeBinding(handle, "大话西游2", "D", 10296L, 0, 0, width, height);
    }

    private static List<WindowNativeBinding> calibratedFive() {
        return List.of(window("0x1", 1036, 783), window("0x2", 1036, 783), window("0x3", 1036, 783),
                window("0x4", 1036, 783), window("0x5", 1036, 783));
    }

    /** 平时绝不能吵：标定尺寸下必须一声不吭，否则告警会被当噪音忽略掉。 */
    @Test
    void calibratedWindowsMustNotWarn() {
        assertNull(LocalTeamRolePreflightService.describeWindowSizeDrift(calibratedFive()));
    }

    /** 事故复现：五窗 1117x840 必须报，且必须把实测值、标定值、窗口数都说清楚。 */
    @Test
    void theIncidentSizeMustBeReportedWithBothSizesAndCount() {
        String notice = LocalTeamRolePreflightService.describeWindowSizeDrift(
                List.of(window("0x1", 1117, 840), window("0x2", 1117, 840), window("0x3", 1117, 840),
                        window("0x4", 1117, 840), window("0x5", 1117, 840)));
        assertNotNull(notice, "五窗全部偏离标定尺寸时必须报警");
        assertTrue(notice.contains("1117x840"), "必须给出实测尺寸：" + notice);
        assertTrue(notice.contains("1036x783"), "必须给出标定基线，用户才知道调回哪个尺寸：" + notice);
        assertTrue(notice.contains("5"), "必须给出受影响窗口数：" + notice);
    }

    /**
     * UI 靠 {@code indexOf(WINDOW_SIZE_DRIFT_MARKER)} 从失败文案里截取告警正文并弹窗。
     * 文案一旦不以 marker 开头，弹窗内容就会缺头或整个识别不到。
     */
    @Test
    void noticeMustStartWithTheMarkerTheUiMatchesOn() {
        String notice = LocalTeamRolePreflightService.describeWindowSizeDrift(
                List.of(window("0x1", 1117, 840)));
        assertNotNull(notice);
        assertTrue(notice.startsWith(LocalTeamRolePreflightService.WINDOW_SIZE_DRIFT_MARKER),
                "UI 按 marker 截取正文，文案必须以 marker 开头：" + notice);
    }

    /**
     * 模板匹配不做任何缩放，所以偏 1px 也可能杀掉 0.85 阈值下的小模板（队长按钮只有 65x14）。
     * 这里不允许有"容差"——上一次给追踪器面板设任意容差就被判为无据（G115）。
     */
    @Test
    void aSinglePixelOfDriftMustStillBeReported() {
        assertNotNull(LocalTeamRolePreflightService.describeWindowSizeDrift(
                List.of(window("0x1", 1037, 783))), "宽 +1px 必须报");
        assertNotNull(LocalTeamRolePreflightService.describeWindowSizeDrift(
                List.of(window("0x1", 1036, 782))), "高 -1px 必须报");
    }

    /** 只有部分窗口漂移时，必须逐尺寸列出，否则用户不知道该调哪几个。 */
    @Test
    void mixedSizesMustAllBeListed() {
        String notice = LocalTeamRolePreflightService.describeWindowSizeDrift(
                List.of(window("0x1", 1036, 783), window("0x2", 1117, 840), window("0x3", 1280, 960)));
        assertNotNull(notice);
        assertTrue(notice.contains("1117x840") && notice.contains("1280x960"), "两种漂移尺寸都要列出：" + notice);
        assertTrue(!notice.contains("1036x783(") , "标定尺寸的窗口不该被列进漂移清单：" + notice);
    }

    /**
     * 绑定还没拿到几何信息（hasGeometry()==false）时必须跳过，不能报成尺寸漂移。
     * 绑定失败在这条链上另有专门的报错文案，混为一谈会把用户引到错误的方向。
     */
    @Test
    void bindingsWithoutGeometryMustNotBeMistakenForDrift() {
        assertNull(LocalTeamRolePreflightService.describeWindowSizeDrift(
                List.of(WindowNativeBinding.empty(), window("0x1", 1036, 783))));
    }

    /** 没有窗口就没有结论——空集合不得报警。 */
    @Test
    void emptyInputMustNotWarn() {
        assertNull(LocalTeamRolePreflightService.describeWindowSizeDrift(List.of()));
    }
}
