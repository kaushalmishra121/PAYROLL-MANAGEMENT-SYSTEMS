package com.payroll.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class FxmlLoadingTest {

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Already initialized
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testDashboardViewFxmlLoadsSuccessfully() {
        assertDoesNotThrow(() -> {
            URL fxmlUrl = getClass().getResource("/fxml/dashboard-view.fxml");
            assertNotNull(fxmlUrl, "FXML resource /fxml/dashboard-view.fxml must exist");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Object root = loader.load();
            assertNotNull(root, "Root node must not be null");
        });
    }

    @Test
    public void testMainLayoutFxmlLoadsSuccessfully() {
        assertDoesNotThrow(() -> {
            URL fxmlUrl = getClass().getResource("/fxml/main-layout.fxml");
            assertNotNull(fxmlUrl, "FXML resource /fxml/main-layout.fxml must exist");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Object root = loader.load();
            assertNotNull(root, "Root node must not be null");
        });
    }

    @Test
    public void testModernDashboardViewFxmlLoadsSuccessfully() {
        assertDoesNotThrow(() -> {
            URL fxmlUrl = getClass().getResource("/fxml/modern-dashboard-view.fxml");
            assertNotNull(fxmlUrl, "FXML resource /fxml/modern-dashboard-view.fxml must exist");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Object root = loader.load();
            assertNotNull(root, "Root node must not be null");
        });
    }
}
