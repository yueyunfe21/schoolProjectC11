package com.bot.dhxy.window.observation;

import com.bot.dhxy.input.action.DialogMouseNoParkZone;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.input.action.InputActionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G137 合同：对话选项点完后的移出悬停滑步（2026-09-01 17:21 封妖卡死）。
 *
 * <p>事故：多谢点在 (309,430)，光标停在原地压住紧接着弹出的"使用封妖符"选项行，悬停变色让
 * 绿字模板 9 秒配不上，直到清障腿碰巧把鼠标点到 (758,511) 挪开。合同钉死：点击点落在 no-park
 * 带内必产出三步滑出且终点在带外；带外点击零动作。</p>
 */
class G137DialogOptionUnhoverGlideContractTest {

    /** 事故原坐标：选项行点击必须滑出，且终点离开 no-park 带。 */
    @Test
    void incidentOptionClickGlidesOutOfZone() {
        List<InputAction> glide = WindowObservationSampler.dialogOptionUnhoverGlide(309, 430, 0, 0);
        assertEquals(6, glide.size(), "三步滑出=3 移动+3 停顿");
        for (int i = 0; i < 6; i += 2) {
            assertEquals(InputActionType.MOVE_MOUSE, glide.get(i).getType(), "偶数位=移动 idx=" + i);
        }
        InputAction last = glide.get(4);
        assertFalse(DialogMouseNoParkZone.containsScreenPoint(last.getX(), last.getY(), 0, 0),
                "滑步终点必须在 no-park 带外: (" + last.getX() + "," + last.getY() + ")");
    }

    /** 事故里的清障点击点 (758,511) 在带外：不产生任何多余输入。 */
    @Test
    void clickOutsideZoneYieldsNoActions() {
        assertTrue(WindowObservationSampler.dialogOptionUnhoverGlide(758, 511, 0, 0).isEmpty());
    }

    /** 窗口不在屏幕原点时按窗口相对坐标判带（队长窗基点 254,23 实测样）。 */
    @Test
    void offsetWindowStillGlidesOut() {
        List<InputAction> glide = WindowObservationSampler.dialogOptionUnhoverGlide(
                309 + 254, 430 + 23, 254, 23);
        assertEquals(6, glide.size());
        InputAction last = glide.get(4);
        assertFalse(DialogMouseNoParkZone.containsScreenPoint(last.getX(), last.getY(), 254, 23));
    }
}
