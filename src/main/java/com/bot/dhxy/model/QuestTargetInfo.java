package com.bot.dhxy.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 🎯 任务情报数据包
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestTargetInfo {
    private String npcName;   // 目标NPC名字 (例如: "李冰冰")
    private String mapName;   // 目标地图 (例如: "长安城东")
    private int targetX;      // 目标 X 坐标
    private int targetY;      // 目标 Y 坐标
    private String rawText;   // OCR 识别出的原始文本（用于调试或备用逻辑）
}