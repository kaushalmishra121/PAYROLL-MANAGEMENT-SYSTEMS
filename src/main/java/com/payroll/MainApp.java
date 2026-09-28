package com.payroll;

import com.payroll.config.DatabaseConfig;
import com.payroll.config.DatabaseInitializer;
import com.payroll.dao.ModernDashboardDao;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        logger.info("Starting Enterprise Payroll Management System...");

        // Auto-check and initialize database tables if needed
        DatabaseInitializer.initializeDatabaseIfRequired();
        try {
            new ModernDashboardDao().initSchemaAndSeed();
        } catch (Exception e) {
            logger.warn("Modern dashboard table init: {}", e.getMessage());
        }

        stage.setTitle("PAYROLL - Enterprise Management System (Champions School & College)");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);

        // Show login screen first — the original startup flow
        loadLoginView();

        stage.setOnCloseRequest(e -> {
            logger.info("Shutting down application and connection pools.");
            DatabaseConfig.closeDataSource();
            Platform.exit();
            System.exit(0);
        });
    }

    public static void loadLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 680);
            scene.getStylesheets().add(MainApp.class.getResource("/css/theme.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            logger.error("Failed to load login screen", e);
        }
    }

    public static void loadMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main-layout.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1360, 860);
            scene.getStylesheets().add(MainApp.class.getResource("/css/sidebar-theme.css").toExternalForm());
            scene.getStylesheets().add(MainApp.class.getResource("/css/theme.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Enterprise Payroll Management System");
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            logger.error("Failed to load main workspace", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
