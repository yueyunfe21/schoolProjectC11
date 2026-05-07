package com.bot.dhxy.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TeleportConfig {
    // 专门存地图别名映射，DialogService 只需要来这里查一下就行
    public static final Map<String, List<String>> MAP_ALIASES = Map.of(
            "长安", List.of("长安", "长安城", "皇宫门口", "化生寺", "去长安", "回长安"),
            "洛阳", List.of("洛阳", "洛阳集市", "城门"),
            "大唐边境", List.of("边境", "斧头帮", "渔村口")
    );
}
