package com.bot.dhxy.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaFX 主窗口服务。
 *
 * 负责启动 JavaFX 平台并显示主窗口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MainWindowService {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);

    private final MainWindowController mainWindowController;

    public void showMainWindow() {
        if (FX_STARTED.compareAndSet(false, true)) {
            startFxAndShowWindow();
        } else {
            Platform.runLater(this::createStage);
        }
    }

    private void startFxAndShowWindow() {
        CountDownLatch latch = new CountDownLatch(1);
        Thread fxThread = new Thread(() -> {
            try {
                Platform.startup(() -> {
                    createStage();
                    latch.countDown();
                });
            } catch (IllegalStateException e) {
                Platform.runLater(() -> {
                    createStage();
                    latch.countDown();
                });
            }
        }, "javafx-main-window-thread");
        fxThread.setDaemon(false);
        fxThread.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("JavaFX 主窗口启动等待被中断", e);
        }
    }

    private void createStage() {
        long createStartedAtNanos = System.nanoTime();
        Stage stage = new Stage();
        stage.setTitle("DHXY Robot 控制台");
        Scene scene = new Scene(mainWindowController.buildView(), 1280, 820);
        applyStylesheet(scene);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            event.consume();
            log.info("主窗口关闭，停止 UI 刷新并请求停止任务队列。");
            mainWindowController.shutdownUi();
            stage.hide();
            Platform.exit();
        });
        stage.setOnShown(event -> {
            log.info("[UI_RESPONSIVENESS] JavaFX stage shown createElapsedMs={}",
                    (System.nanoTime() - createStartedAtNanos) / 1_000_000L);
            mainWindowController.onStageShown();
        });
        stage.show();
    }

    private void applyStylesheet(Scene scene) {
        URL stylesheet = getClass().getResource("/styles/dhxy-fluent.css");
        if (stylesheet == null) {
            log.warn("未找到 JavaFX 样式文件：/styles/dhxy-fluent.css");
            return;
        }
        scene.getStylesheets().add(stylesheet.toExternalForm());
    }
}
