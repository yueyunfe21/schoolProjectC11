package com.bot.dhxy.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor(force = true) // force=true 是为了兼容下面的 @NonNull
@RequiredArgsConstructor         // 生成包含所有 @NonNull 字段的构造函数
public class PlayerCharacter {

    // ============ 核心身份信息 (初始化必填) ============
    @NonNull
    private String name;
    @NonNull
    private String id;
    @NonNull
    private String gameServerName;

    // ============ 动态状态信息 (后续再 set) ============
    private String currentMapName;
    private int x;
    private int y;

    // 你的 toString 依然保留
    @Override
    public String toString() {
        return String.format("👤 [%s] | ID: %s | 服务器: %s | 位置: %s (%d, %d)",
                name, id, gameServerName, currentMapName, x, y);
    }
}