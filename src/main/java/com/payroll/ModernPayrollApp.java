package com.payroll;

import com.payroll.config.DatabaseConfig;
import com.payroll.dao.ModernDashboardDao;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone launcher for the Modern Payroll Management System Dashboard.
 * Matches reference UI specifications with pure JDBC integration (MySQL).
 */
public class ModernPayrollApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ModernPayrollApp.class);

    @Override
    public void start(Stage stage) {
        logger.info("Launching Modern Payroll Management System Dashboard...");

        // 1. Initialize modern tables and seed data via pure JDBC
        try {
            ModernDashboardDao dao = new ModernDashboardDao();
            dao.initSchemaAndSeed();
        } catch (Exception e) {
            logger.warn("Initial DB setup warning: {}", e.getMessage());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/modern-dashboard-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1360, 880);
            scene.getStylesheets().add(getClass().getResource("/css/modern-dashboard.css").toExternalForm());

            stage.setTitle("PAYROLL - Modern Management System (Champions School & College)");
            stage.setMinWidth(1180);
            stage.setMinHeight(750);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

            stage.setOnCloseRequest(e -> {
                DatabaseConfig.closeDataSource();
                Platform.exit();
                System.exit(0);
            });

        } catch (Exception e) {
            logger.error("Failed to start Modern Payroll Dashboard", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
