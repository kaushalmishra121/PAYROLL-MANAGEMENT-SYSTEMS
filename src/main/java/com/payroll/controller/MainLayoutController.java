package com.payroll.controller;

import com.payroll.MainApp;
import com.payroll.model.User;
import com.payroll.service.AuthService;
import com.payroll.util.DateUtils;
import com.payroll.util.DialogUtils;
import com.payroll.util.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class MainLayoutController {
    private static final Logger logger = LoggerFactory.getLogger(MainLayoutController.class);

    @FXML private StackPane contentArea;
    @FXML private Label lblHeaderTitle;
    @FXML private Label lblHeaderSubtitle;
    @FXML private Label lblClock;

    @FXML private Label lblUserInitial;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    @FXML private VBox adminNavSection;
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavEmployees;
    @FXML private Button btnNavDepartments;
    @FXML private Button btnNavAttendance;
    @FXML private Button btnNavLeaves;
    @FXML private Button btnNavPayroll;
    @FXML private Button btnNavReports;
    @FXML private Button btnNavUsers;
    @FXML private Button btnThemeToggle;

    private final AuthService authService = new AuthService();
    private boolean isDarkMode = false;

    private static MainLayoutController instance;

    public static MainLayoutController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        setupUserProfile();
        lblClock.setText(DateUtils.formatDate(LocalDate.now()));
        showDashboard();
    }

    private void setupUserProfile() {
        User user = UserSession.getCurrentUser();
        if (user != null) {
            if (lblUserName != null) lblUserName.setText(user.getFullName());
            if (lblUserRole != null) lblUserRole.setText(user.getRole().name());
            String initial = user.getFullName() != null && !user.getFullName().isEmpty() 
                    ? user.getFullName().substring(0, 1).toUpperCase() : "U";
            if (lblUserInitial != null) lblUserInitial.setText(initial);

            // Hide Admin options if not ADMIN
            if (!user.isAdmin() && adminNavSection != null) {
                adminNavSection.setVisible(false);
                adminNavSection.setManaged(false);
            }
        } else {
            if (lblUserName != null) lblUserName.setText("School Admin");
            if (lblUserRole != null) lblUserRole.setText("Champions School & College");
            if (lblUserInitial != null) lblUserInitial.setText("SA");
        }
    }

    @FXML
    public void showDashboard() {
        loadView("/fxml/dashboard-view.fxml", "Executive Dashboard", "Real-time employee metrics and payroll analytics", btnNavDashboard);
    }

    @FXML
    public void showModernDashboard() {
        loadView("/fxml/modern-dashboard-view.fxml", "School Dashboard", "Champions School & College Payroll Analytics", btnNavDashboard);
    }

    @FXML
    public void showEmployees() {
        loadView("/fxml/employee-management-view.fxml", "Employee Management", "Directory, onboarding, profiles, and salary structures", btnNavEmployees);
    }

    @FXML
    public void showDepartments() {
        loadView("/fxml/department-management-view.fxml", "Departments & Designations", "Organizational hierarchy, job titles, and pay scales", btnNavDepartments);
    }

    @FXML
    public void showAttendance() {
        loadView("/fxml/attendance-view.fxml", "Attendance Tracking", "Daily check-ins, working hours, and monthly summaries", btnNavAttendance);
    }

    @FXML
    public void showLeaves() {
        loadView("/fxml/leave-management-view.fxml", "Leave Management", "Leave applications, balance reviews, and approvals", btnNavLeaves);
    }

    @FXML
    public void showPayroll() {
        loadView("/fxml/payroll-processing-view.fxml", "Payroll Processing", "Monthly calculation runs, approvals, and disbursements", btnNavPayroll);
    }

    @FXML
    public void showReports() {
        loadView("/fxml/reports-view.fxml", "Reports & Payslips", "Compliance registers, itemized payslips, and exports", btnNavReports);
    }

    @FXML
    public void showUsers() {
        UserSession.requireAdmin();
        loadView("/fxml/user-management-view.fxml", "User Management & Audit Trail", "System security, user accounts, and activity logs", btnNavUsers);
    }

    public void loadView(String fxmlPath, String title, String subtitle, Button activeButton) {
        try {
            URL fxmlUrl = MainApp.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                logger.error("FXML resource not found on classpath: {}", fxmlPath);
                DialogUtils.showError("View Navigation Error",
                        "FXML resource not found: " + fxmlPath
                        + "\nEnsure the file exists under src/main/resources" + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node node = loader.load();

            contentArea.getChildren().setAll(node);
            if (lblHeaderTitle != null) lblHeaderTitle.setText(title);
            if (lblHeaderSubtitle != null) lblHeaderSubtitle.setText(subtitle);

            highlightNavButton(activeButton);
        } catch (IOException e) {
            logger.error("Failed to load view: {}", fxmlPath, e);
            DialogUtils.showError("View Navigation Error", "Could not load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    private void highlightNavButton(Button activeButton) {
        List<Button> allButtons = Arrays.asList(
                btnNavDashboard, btnNavEmployees, btnNavDepartments, btnNavAttendance,
                btnNavLeaves, btnNavPayroll, btnNavReports, btnNavUsers
        );
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-item-active");
                if (!btn.getStyleClass().contains("nav-item")) {
                    btn.getStyleClass().add("nav-item");
                }
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-item");
            if (!activeButton.getStyleClass().contains("nav-item-active")) {
                activeButton.getStyleClass().add("nav-item-active");
            }
        }
    }

    @FXML
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (contentArea.getScene() != null) {
            if (isDarkMode) {
                contentArea.getScene().getRoot().getStyleClass().add("dark-theme");
                btnThemeToggle.setText("☀️ Light");
            } else {
                contentArea.getScene().getRoot().getStyleClass().remove("dark-theme");
                btnThemeToggle.setText("🌓 Theme");
            }
        }
    }

    @FXML
    private void handleLogout() {
        if (DialogUtils.showConfirmation("Logout Confirmation", "Are you sure you want to log out?", "You will be returned to the login screen.")) {
            authService.logout();
            MainApp.loadLoginView();
        }
    }
}
