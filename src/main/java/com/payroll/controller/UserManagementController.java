package com.payroll.controller;

import com.payroll.model.AuditLog;
import com.payroll.model.User;
import com.payroll.service.AuditService;
import com.payroll.service.AuthService;
import com.payroll.util.DateUtils;
import com.payroll.util.DialogUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserManagementController {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);

    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colUserFullName;
    @FXML private TableColumn<User, String> colUserEmail;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserStatus;
    @FXML private TableColumn<User, Void> colUserActions;

    @FXML private TableView<AuditLog> tblAuditLogs;
    @FXML private TableColumn<AuditLog, String> colAuditTime;
    @FXML private TableColumn<AuditLog, String> colAuditUser;
    @FXML private TableColumn<AuditLog, String> colAuditAction;
    @FXML private TableColumn<AuditLog, String> colAuditEntity;
    @FXML private TableColumn<AuditLog, String> colAuditDetails;
    @FXML private TableColumn<AuditLog, String> colAuditIp;

    private final AuthService authService = new AuthService();
    private final AuditService auditService = new AuditService();

    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final ObservableList<AuditLog> logList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupUserColumns();
        setupAuditColumns();
        loadUsers();
        loadAuditLogs();
    }

    private void setupUserColumns() {
        colUserId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colUserFullName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colUserEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colUserRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().name()));

        colUserStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colUserStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().addAll("badge", "ACTIVE".equalsIgnoreCase(status) ? "badge-active" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colUserActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnStatus = new Button("Toggle");
            private final Button btnPass = new Button("Password");
            private final HBox box = new HBox(6, btnStatus, btnPass);
            {
                btnStatus.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnPass.getStyleClass().addAll("btn-secondary", "btn-sm");
                box.setAlignment(Pos.CENTER);

                btnStatus.setOnAction(e -> handleToggleStatus(getTableView().getItems().get(getIndex())));
                btnPass.setOnAction(e -> showResetPasswordDialog(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblUsers.setItems(userList);
    }

    private void setupAuditColumns() {
        colAuditTime.setCellValueFactory(c -> new SimpleStringProperty(DateUtils.formatDateTime(c.getValue().getCreatedAt())));
        colAuditUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername() != null ? c.getValue().getUsername() : "SYSTEM"));
        colAuditAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        colAuditEntity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntityType()));
        colAuditDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));
        colAuditIp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIpAddress()));

        tblAuditLogs.setItems(logList);
    }

    public void loadUsers() {
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                return authService.getAllUsers();
            }
        };
        task.setOnSucceeded(e -> userList.setAll(task.getValue()));
        new Thread(task).start();
    }

    @FXML
    public void loadAuditLogs() {
        Task<List<AuditLog>> task = new Task<>() {
            @Override
            protected List<AuditLog> call() {
                return auditService.getRecentLogs(100);
            }
        };
        task.setOnSucceeded(e -> logList.setAll(task.getValue()));
        new Thread(task).start();
    }

    @FXML
    public void showCreateUserDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Create System User");
        dialog.setHeaderText("Create a new authenticated administrator or HR account.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Create Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        TextField txtUsername = new TextField();
        TextField txtEmail = new TextField();
        TextField txtName = new TextField();
        ComboBox<User.Role> cmbRole = new ComboBox<>(FXCollections.observableArrayList(User.Role.values()));
        cmbRole.setValue(User.Role.HR);
        PasswordField txtPass = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Username:*"), txtUsername);
        grid.addRow(1, new Label("Full Name:*"), txtName);
        grid.addRow(2, new Label("Corporate Email:*"), txtEmail);
        grid.addRow(3, new Label("System Role:*"), cmbRole);
        grid.addRow(4, new Label("Password (min 6 chars):*"), txtPass);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    User u = new User();
                    u.setUsername(txtUsername.getText());
                    u.setEmail(txtEmail.getText());
                    u.setFullName(txtName.getText());
                    u.setRole(cmbRole.getValue());
                    u.setStatus(User.UserStatus.ACTIVE);

                    authService.createUser(u, txtPass.getText());
                    DialogUtils.showSuccess("User Created", "User account '" + u.getUsername() + "' created successfully.");
                    loadUsers();
                    loadAuditLogs();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Creation Failed", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showResetPasswordDialog(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Reset Password - " + user.getUsername());
        dialog.setHeaderText("Set a new password for " + user.getFullName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Update Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        PasswordField txtNewPass = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("New Password (min 6 chars):*"), txtNewPass);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    authService.changePassword(user.getId(), "", txtNewPass.getText());
                    DialogUtils.showSuccess("Password Updated", "Password updated successfully for " + user.getUsername());
                    loadAuditLogs();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Reset Failed", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleToggleStatus(User user) {
        User.UserStatus newStatus = (user.getStatus() == User.UserStatus.ACTIVE) ? User.UserStatus.INACTIVE : User.UserStatus.ACTIVE;
        if (DialogUtils.showConfirmation("Change Account Status", "Change status for '" + user.getUsername() + "'?", "New Status: " + newStatus)) {
            try {
                authService.updateUserStatus(user.getId(), newStatus);
                DialogUtils.showSuccess("Status Updated", "User status changed to " + newStatus);
                loadUsers();
                loadAuditLogs();
            } catch (Exception ex) {
                DialogUtils.showError("Status Update Failed", ex.getMessage());
            }
        }
    }
}
