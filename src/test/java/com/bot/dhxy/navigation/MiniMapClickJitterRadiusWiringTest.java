package com.bot.dhxy.navigation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for configurable mini-map click jitter radius.
 */
public final class MiniMapClickJitterRadiusWiringTest {

    private MiniMapClickJitterRadiusWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String request = read(root, "src/main/java/com/bot/dhxy/model/navigation/NavigationRequest.java");
        String helper = read(root, "src/main/java/com/bot/dhxy/tools/CoordinateHelper.java");
        String navigation = read(root, "src/main/java/com/bot/dhxy/service/NavigationService.java");
        String wubei = read(root, "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java");
        String xiuluo = read(root, "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java");

        require(request.contains("miniMapClickRandomRadiusPx"),
                "NavigationRequest must expose optional mini-map click jitter radius");
        require(helper.contains("randomizeMiniMapClickPoint(Point basePixelPoint, int randomRadiusPx)"),
                "CoordinateHelper must randomize mini-map click using caller-provided radius");
        require(navigation.contains("request.getMiniMapClickRandomRadiusPx()"),
                "NavigationService must pass request jitter radius into CoordinateHelper");
        require(wubei.contains("PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX")
                        && wubei.contains(".miniMapClickRandomRadiusPx(PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX)"),
                "Wubei post-accept prepath must use the larger jitter radius");
        require(xiuluo.contains("PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX")
                        && xiuluo.contains(".miniMapClickRandomRadiusPx(PREPATH_MINI_MAP_CLICK_RANDOM_RADIUS_PX)"),
                "Xiuluo start-exit prepath must use the same larger jitter radius");

        System.out.println("MiniMapClickJitterRadiusWiringTest passed");
    }

    private static String read(Path root, String path) throws Exception {
        return Files.readString(root.resolve(path), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
