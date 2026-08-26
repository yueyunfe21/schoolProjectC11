package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.local.LocalMovementFactMechanics;
import com.bot.dhxy.cloud.turn.local.NpcArrivalFrameFifoLocalExecutor;
import com.bot.dhxy.cloud.turn.local.TeamReturnPanelLocalOperation;
import com.bot.dhxy.cloud.turn.local.XinshouDragLocalOperationExecutor;
import com.bot.dhxy.service.BagService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2026-08-23 用户契约（停止=彻底清空）：手动停止、队列全部做完、崩溃自动重启后，窗口的
 * "游戏现实记忆"必须回到进程刚启动的状态；只有 暂停→恢复 保留。本类是客户端侧的总复位器，
 * 由 {@link WindowTaskControlService} 在两个收口点调用：
 * ① startOneRemote 且 startupMode==NORMAL（用户手动新开一轮，覆盖停止后/队列做完后再启动）；
 * ② recoverRemoteTerminal 崩溃自动重启提交前。
 *
 * <p>只清"现实记忆"（包裹锚点/页缓存、识别位置、对话准备残留、面板认领、点击点积累、
 * 拖拽会话），不碰秩序保险丝（每窗口世代号、注册绑定、输入设备、传输会话）。
 * 云端侧由 CloudFreshStartReset 按同一规则清。清单来源：2026-08-23 双仓全量清点。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFreshStartReset {

    private final BagService bagService;
    private final NpcArrivalFrameFifoLocalExecutor npcArrivalFrameFifoLocalExecutor;
    private final TeamReturnPanelLocalOperation teamReturnPanelLocalOperation;
    private final XinshouDragLocalOperationExecutor xinshouDragLocalOperationExecutor;
    private final LocalMovementFactMechanics localMovementFactMechanics;

    /** Idempotent per-window reality reset; safe while other windows keep running. */
    public void resetWindowRealityMemory(String windowId, WindowRuntimeContext context, String reason) {
        if (windowId == null || windowId.isBlank()) {
            return;
        }
        // 审查修正：全局页签校准不在这清（会把启动校准刚学到的值抹掉）——
        // 它在 calibrateMainBagTaskTabBeforeRemoteStart 里按批清一次后重新校准。
        bagService.forgetWindowRealityMemory(windowId);
        npcArrivalFrameFifoLocalExecutor.forgetWindowRealityMemory(windowId);
        teamReturnPanelLocalOperation.forgetWindowRealityMemory(windowId);
        xinshouDragLocalOperationExecutor.abortRetainedDragForFreshStart(windowId);
        localMovementFactMechanics.forgetWindowRealityMemory(windowId);
        if (context != null) {
            context.clearCrossRunRealityMemory(reason);
        }
        log.info("Local fresh-start reality reset done: windowId={} reason={}", windowId, reason);
    }
}
