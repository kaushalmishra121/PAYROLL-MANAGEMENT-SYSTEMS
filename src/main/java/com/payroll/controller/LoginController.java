package com.payroll.controller;

import com.payroll.MainApp;
import com.payroll.service.AuthService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Submit on enter key
        txtPassword.setOnAction(this::handleLogin);
        txtUsername.setOnAction(e -> txtPassword.requestFocus());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        hideError();
        btnLogin.setDisable(true);
        btnLogin.setText("Verifying credentials...");

        // Perform authentication asynchronously so UI does not freeze
        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                authService.login(username, password);
                return null;
            }
        };

        loginTask.setOnSucceeded(e -> {
            btnLogin.setDisable(false);
            btnLogin.setText("Sign In to Workspace");
            MainApp.loadMainView();
        });

        loginTask.setOnFailed(e -> {
            btnLogin.setDisable(false);
            btnLogin.setText("Sign In to Workspace");
            Throwable ex = loginTask.getException();
            showError(ex != null ? ex.getMessage() : "Authentication failed. Please try again.");
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void fillAdminDemo() {
        txtUsername.setText("admin");
        txtPassword.setText("Admin@123");
        hideError();
    }

    @FXML
    private void fillHrDemo() {
        txtUsername.setText("hrmanager");
        txtPassword.setText("Hr@12345");
        hideError();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
