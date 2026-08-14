package com.bot.dhxy.model.navigation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Audited world-map search targets.
 *
 * <p>The first 71 entries mirror every canonical key in Cloud
 * {@code src/main/resources/config/maps.json}. The final G050 entry is the separately approved
 * world-map search target not yet represented by that coordinate catalog. Tokens are explicit
 * pinyin initials and are never inferred from a machine input method at runtime.</p>
 */
public final class WorldMapSearchTargetCatalog {

    public static final int MAPS_JSON_CANONICAL_TARGET_COUNT = 71;

    private static final List<Target> TARGETS = List.of(
            map("长安", "ca"),
            map("洛阳城", "lyc"),
            map("长安城东", "cacd"),
            map("大唐境内", "dtjn"),
            map("龙宫", "lg"),
            map("大唐边境", "dtbj"),
            map("白骨山", "bgs"),
            map("万寿山", "wss"),
            map("四圣庄", "ssz"),
            map("天宫", "tg"),
            map("蟠桃园", "pty"),
            map("御马监", "ymj"),
            map("灵兽村", "lsc"),
            map("兰若寺", "lrs"),
            map("龙窟五层", "lkwc"),
            map("龙窟七层", "lkqc"),
            map("龙窟六层", "lklc"),
            map("凤巢六层", "fclc"),
            map("凤巢五层", "fcwc"),
            map("凤巢七层", "fcqc"),
            map("大雁塔二层", "dytec"),
            map("大雁塔三层", "dytsc"),
            map("大雁塔四层", "dytsc"),
            map("大雁塔五层", "dytwc"),
            map("大雁塔六层", "dytlc"),
            map("瑶池", "yc"),
            map("宝象国", "bxg"),
            map("火云戈壁", "hygb"),
            map("平顶山", "pds"),
            map("北俱芦洲", "bjlz"),
            map("东海渔村", "dhyc"),
            map("冰窟", "bk"),
            map("波月洞", "byd"),
            map("城隍庙", "chm"),
            map("大雁塔顶", "dytd"),
            map("大雁塔一层", "dytyc"),
            map("地下鬼岛", "dxgd"),
            map("凤巢二层", "fcec"),
            map("凤巢三层", "fcsc"),
            map("凤巢四层", "fcsc"),
            map("凤巢一层", "fcyc"),
            map("古城废墟", "gcfx"),
            map("化生寺", "hss"),
            map("火云洞", "hyd"),
            map("金兜洞", "jdd"),
            map("金銮殿", "jld"),
            map("老君丹房", "ljdf"),
            map("莲花洞", "lhd"),
            map("龙窟二层", "lkec"),
            map("龙窟三层", "lksc"),
            map("龙窟四层", "lksc"),
            map("龙窟一层", "lkyc"),
            map("轮回司", "lhs"),
            map("牛记布店", "njbd"),
            map("女儿村", "nec"),
            map("狮驼岭", "stl"),
            map("孙婆婆家", "sppj"),
            map("铁匠屋", "tjw"),
            map("无忧谷", "wyg"),
            map("五指山", "wzs"),
            map("修罗古城", "xlgc"),
            map("芽馆", "yg"),
            map("隐林涧", "ylj"),
            map("长寿村", "csc"),
            map("广寒宫", "ghg"),
            map("长寿村外", "cscw"),
            map("斧头帮总部", "ftbzb"),
            map("普陀山", "pts"),
            map("珊瑚海岛", "shhd"),
            map("傲来国", "alg"),
            map("地府", "df"),
            new Target("阎王书房", "ywsf", "G050 approved world-map search target")
    );

    static {
        if (TARGETS.size() != MAPS_JSON_CANONICAL_TARGET_COUNT + 1) {
            throw new IllegalStateException("audited world-map target count drifted");
        }
    }

    private WorldMapSearchTargetCatalog() {
    }

    /** @return immutable audited production targets, shared by navigation and the G051 manual tool. */
    public static List<Target> all() {
        return TARGETS;
    }

    /**
     * Resolve a canonical target.
     *
     * @param targetMap exact business canonical map name; whitespace is ignored at the edges.
     * @return catalog target, or empty only when no audited canonical map exists.
     */
    public static Optional<Target> find(String targetMap) {
        if (targetMap == null) {
            return Optional.empty();
        }
        String normalized = targetMap.trim();
        return TARGETS.stream().filter(target -> target.targetMap().equals(normalized)).findFirst();
    }

    private static Target map(String targetMap, String token) {
        return new Target(targetMap, token, "config/maps.json canonical map key");
    }

    /** One canonical target and its explicit, machine-independent ASCII input token. */
    public record Target(String targetMap, String token, String owningSource) {
        public Target {
            if (targetMap == null || targetMap.isBlank()) {
                throw new IllegalArgumentException("targetMap must not be blank");
            }
            if (token == null || !token.matches("[a-z0-9]+")
                    || !token.equals(token.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("token must be lowercase ASCII letters/digits");
            }
            if (owningSource == null || owningSource.isBlank()) {
                throw new IllegalArgumentException("owningSource must not be blank");
            }
        }
    }
}

