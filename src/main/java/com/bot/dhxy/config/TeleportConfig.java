package com.bot.dhxy.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TeleportConfig {
    // 专门存地图别名映射，DialogService 只需要来这里查一下就行
    public static final Map<String, List<String>> MAP_ALIASES = Map.of(
            "长安", List.of("长安桥", "长安城", "皇宫门口", "去长安", "回长安","大雁塔", "长安武馆","长安"),
            "洛阳城", List.of("洛阳", "洛阳集市", "城门"),
            "大唐边境", List.of("边境", "斧头帮", "渔村口"),
            "灵兽村", List.of("灵兽村", "去灵兽村", "灵兽"),
            "大唐境内", List.of("江洲", "洪洲","丰都"),
            "宝象国", List.of("宝象国商会", "宝象国光禄寺", "宝象国")
    );
}
