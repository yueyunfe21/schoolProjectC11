package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户端身份服务。
 * 专门负责从窗口标题提取账号、服务器、角色名等元数据。
 */
@Slf4j
@Service
public class ClientIdentityService {

    private final GameClientTracker tracker;
    private final WindowTaskContextHolder windowTaskContextHolder;

    public ClientIdentityService(GameClientTracker tracker,
                                 WindowTaskContextHolder windowTaskContextHolder) {
        this.tracker = tracker;
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

    /**
     * 从当前 WindowTaskRunner 绑定的原生窗口标题中提取角色基本信息。
     * 多窗口模式下优先读取 WindowRuntimeContext.nativeBinding.title，避免 tracker 尚未刷新时拿到空标题。
     */
    public void scanAndSyncIdentity(PlayerCharacter me) {
        String title = resolveCurrentWindowTitle();
        if (title == null || title.isBlank()) {
            log.warn("⚠️ [身份识别] 当前窗口标题为空，无法提取身份信息。");
            return;
        }

        log.info("🪪 [身份识别] 使用窗口标题解析角色档案：{}", title);

        String regex = "-\\s+(.+?)\\s+-\\s+(.+?)\\s*[\\(（]ID[:：]\\s*(\\d+)[\\)）]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(title);

        if (matcher.find()) {
            me.setGameServerName(matcher.group(1));
            me.setName(matcher.group(2));
            me.setId(matcher.group(3));
            log.info("🪪 [身份识别] 成功提取：server={} name={} id={}",
                    me.getGameServerName(), me.getName(), me.getId());
        } else {
            log.warn("⚠️ [身份识别] 窗口标题格式不匹配，无法提取身份信息。标题: {}", title);
        }
    }

    private String resolveCurrentWindowTitle() {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isPresent()) {
            WindowNativeBinding binding = current.get().getNativeBinding();
            if (binding != null && binding.getTitle() != null && !binding.getTitle().isBlank()) {
                return binding.getTitle();
            }
        }

        String trackerTitle = tracker.getFullWindowTitle();
        if (trackerTitle != null && !trackerTitle.isBlank()) {
            return trackerTitle;
        }

        if (tracker.locateWindow()) {
            trackerTitle = tracker.getFullWindowTitle();
            if (trackerTitle != null && !trackerTitle.isBlank()) {
                return trackerTitle;
            }
        }

        return null;
    }
}