package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.PlayerCharacter;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户端身份服务
 * 专门负责从底层提取账号、服务器等元数据
 */
@Service
public class ClientIdentityService {

    private final GameClientTracker tracker;

    public ClientIdentityService(GameClientTracker tracker) {
        this.tracker = tracker;
    }

    /**
     * 🪪 从窗口标题中白嫖角色基本信息
     */
    public void scanAndSyncIdentity(PlayerCharacter me) {
        String title = tracker.getFullWindowTitle();
        if (title == null || title.isEmpty()) {
            return;
        }

        // 正则魔法升级：同时兼容中英文括号和冒号，并且对空格更加宽容！
        String regex = "-\\s+(.+?)\\s+-\\s+(.+?)\\s*[\\(（]ID[:：]\\s*(\\d+)[\\)）]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(title);

        if (matcher.find()) {
            me.setGameServerName(matcher.group(1)); // 服务器
            me.setName(matcher.group(2));           // 名字
            me.setId(matcher.group(3));             // ID
            System.out.println("🪪 [身份识别] 成功从底层提取角色档案！" + me.getName());
        } else {
            System.out.println("⚠️ [身份识别] 窗口标题格式不匹配，无法提取身份信息。标题: " + title);
        }
    }
}