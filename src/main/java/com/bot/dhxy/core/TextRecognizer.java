package com.bot.dhxy.core;

import com.baidu.aip.ocr.AipOcr;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.awt.Point;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextRecognizer {

    // ⚠️ 请在这里填入你刚刚在百度云控制台获取的三个 Key！
    public static final String APP_ID = "7663260";
    public static final String API_KEY = "sPCs5AFdc13mfgtqHnovGP5b";
    public static final String SECRET_KEY = "yoDcd7FEqh4fkC5qLaE3igfQ0wEbPudx";

    private final AipOcr client;

    public TextRecognizer() {
        System.out.println("☁️ [文字中枢] 正在连接百度云 OCR 引擎...");
        // 初始化百度 OCR 客户端
        client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
        // 设置网络超时时间
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
    }

    /**
     * 核心接口：调用云端识别图片中的文字
     */
    public String readText(String imagePath) {
        try {
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("language_type", "CHN_ENG");
            options.put("detect_direction", "true");

            JSONObject res = client.basicGeneral(imagePath, options);

            StringBuilder fullText = new StringBuilder();
            if (res.has("words_result")) {
                JSONArray wordsResult = res.getJSONArray("words_result");
                for (int i = 0; i < wordsResult.length(); i++) {
                    fullText.append(wordsResult.getJSONObject(i).getString("words"));
                }
            } else if (res.has("error_msg")) {
                System.err.println("❌ 百度云报错: " + res.getString("error_msg"));
            }

            return fullText.toString();

        } catch (Exception e) {
            System.err.println("❌ 云端网络请求异常: " + e.getMessage());
            return "";
        }
    }

    // ==========================================
    // 🚀 性能核弹专属组件：一次读取，全量返回
    // ==========================================

    /**
     * 一次性调用百度 OCR，获取图片中所有文字块及其中心坐标
     * @param imagePath 截图路径
     * @return 包含文字和坐标的列表
     */
    public java.util.List<OcrWordResult> getAllTextResults(String imagePath) {
        java.util.List<OcrWordResult> results = new java.util.ArrayList<>();
        try {
            java.util.HashMap<String, String> options = new java.util.HashMap<>();
            options.put("language_type", "CHN_ENG");

            // ⚠️ 必须使用 general 接口（带位置的高精度版接口）
            JSONObject res = client.general(imagePath, options);

            if (!res.has("words_result")) {
                System.err.println("❌ getAllTextResults: 未识别到文字或图片为空");
                return results;
            }

            JSONArray wordsResult = res.getJSONArray("words_result");

            for (int i = 0; i < wordsResult.length(); i++) {
                JSONObject item = wordsResult.getJSONObject(i);
                String words = item.getString("words");
                JSONObject loc = item.getJSONObject("location");

                int top = loc.getInt("top");
                int left = loc.getInt("left");
                int width = loc.getInt("width");
                int height = loc.getInt("height");

                // 算出该行文字框的正中心点
                int centerX = left + (width / 2);
                int centerY = top + (height / 2);

                // 塞进列表里
                results.add(new OcrWordResult(words, centerX, centerY));
            }

            System.out.println("⚡ OCR 极速全量扫描完成，共提取 " + results.size() + " 个文字块。");

        } catch (Exception e) {
            System.err.println("❌ OCR 全量请求异常: " + e.getMessage());
        }
        return results;
    }

    /**
     * 内部数据类：用来同时保存文字和它对应的中心坐标
     */
    public static class OcrWordResult {
        private final String text;
        private final int x;
        private final int y;

        public OcrWordResult(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }

        public String getText() { return text; }
        public int getX() { return x; }
        public int getY() { return y; }
    }

    /**
     * 业务接口：专门解析游戏坐标 (如 "长安 [14, 229]")
     */
    public LocationInfo parseLocation(String imagePath) {
        String rawText = readText(imagePath);
        if (rawText.isEmpty()) return null;

        System.out.println("🔍 云端识别结果: " + rawText);

        Pattern pattern = Pattern.compile("([^0-9\\[]+).*?\\[(\\d+)\\s*,\\s*(\\d+)\\]");
        Matcher matcher = pattern.matcher(rawText);

        if (matcher.find()) {
            String mapName = matcher.group(1).trim();
            int x = Integer.parseInt(matcher.group(2));
            int y = Integer.parseInt(matcher.group(3));
            return new LocationInfo(mapName, x, y);
        }

        System.out.println("⚠️ 未能在文本中匹配到标准坐标格式。");
        return null;
    }

    // ==========================================
    // 🌟 核心新增：盲视野终极导航雷达！
    // ==========================================
    /**
     * 不认地名，只认画面里最后一个【坐标链接】
     * @param imagePath 截图路径
     * @return 相对截图左上角的中心坐标 Point
     */
    public Point findLastCoordinateLink(String imagePath) {
        try {
            HashMap<String, String> options = new HashMap<String, String>();
            options.put("language_type", "CHN_ENG");

            // ⚠️ 注意这里必须用 general (带位置信息的接口)
            JSONObject res = client.general(imagePath, options);

            if (!res.has("words_result")) {
                System.err.println("❌ 未识别到文字或调用失败");
                return null;
            }

            JSONArray wordsResult = res.getJSONArray("words_result");

            int lastX = -1;
            int lastY = -1;

            // 正则匹配："(数字, 数字)" 或 "（数字，数字）"
            String regex = "[\\(（]\\s*\\d+\\s*[,，]\\s*\\d+\\s*[\\)）]";
            Pattern pattern = Pattern.compile(regex);

            for (int i = 0; i < wordsResult.length(); i++) {
                JSONObject item = wordsResult.getJSONObject(i);
                String words = item.getString("words");

                if (pattern.matcher(words).find()) {
                    JSONObject loc = item.getJSONObject("location");

                    int top = loc.getInt("top");
                    int left = loc.getInt("left");
                    int width = loc.getInt("width");
                    int height = loc.getInt("height");

                    lastX = left + (width / 2);
                    lastY = top + (height / 2);

                    System.out.println("🔍 发现传送节点 [" + words + "]，记录坐标: " + lastX + ", " + lastY);
                }
            }

            if (lastX != -1) {
                System.out.println("🎯 锁定最后一个绿色传送链接！最终选择相对坐标: " + lastX + ", " + lastY);
                return new Point(lastX, lastY);
            }

        } catch (Exception e) {
            System.err.println("❌ 带位置 OCR 请求异常: " + e.getMessage());
        }
        return null;
    }

    public static class LocationInfo {
        public String mapName;
        public int x;
        public int y;

        public LocationInfo(String mapName, int x, int y) {
            this.mapName = mapName;
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("【当前位置】地图: %s | X坐标: %d | Y坐标: %d", mapName, x, y);
        }
    }
}