package com.bot.dhxy.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        Stage stage = new Stage();
        stage.setTitle("DHXY Robot 控制台");
        stage.setScene(new Scene(mainWindowController.buildView(), 980, 640));
        stage.show();
    }
}
