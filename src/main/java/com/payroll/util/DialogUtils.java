package com.payroll.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import java.util.Optional;

public class DialogUtils {

    public static void showInformation(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            styleAlert(alert);
            alert.showAndWait();
        });
    }

    public static void showSuccess(String header, String content) {
        showInformation("Success", header, content);
    }

    public static void showError(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Encountered");
            alert.setHeaderText(header);
            alert.setContentText(content);
            styleAlert(alert);
            alert.showAndWait();
        });
    }

    public static void showWarning(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(header);
            alert.setContentText(content);
            styleAlert(alert);
            alert.showAndWait();
        });
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void styleAlert(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(DialogUtils.class.getResource("/css/theme.css").toExternalForm());
        pane.getStyleClass().add("custom-alert");
        Stage stage = (Stage) pane.getScene().getWindow();
        stage.setAlwaysOnTop(true);
    }
}
