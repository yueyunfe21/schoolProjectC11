package com.bot.dhxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * G151：采集后端选择。
 *
 * <p>{@link Backend#LEGACY}（默认）＝BitBlt 链；{@link Backend#WGC_SIDECAR}＝
 * Windows.Graphics.Capture sidecar（scripts/wgc_capture_sidecar.py）优先、失败按次回退 BitBlt。
 * 两路都不向游戏窗口投递任何消息。PrintWindow——全系统唯一会让游戏窗口过程收到重绘请求的采集
 * 路径（2026-07 实证整窗重绘；落盘 910 帧实证重启前它是主路）——已按用户拍板（2026-09-04
 * "兜底我都不想要"）整体删除：两路皆失败=本拍快失败，宁可瞎一帧不让游戏有感。
 * 2026-09-04 对 3519 实测：WGC 帧与窗口像素零偏移、帧契约同构（1036x783 含标题栏），模板零改动。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.capture")
public class CaptureBackendProperties {

    private Backend backend = Backend.LEGACY;
    private int wgcSidecarPort = 47831;
    /** 帧最大年龄：sidecar 缓存帧比这个新就直接用，否则等下一帧。 */
    private int wgcFreshMs = 600;
    /** 单次采集请求的整体超时。 */
    private int wgcTimeoutMs = 1_500;

    public enum Backend {
        LEGACY,
        WGC_SIDECAR
    }
}
