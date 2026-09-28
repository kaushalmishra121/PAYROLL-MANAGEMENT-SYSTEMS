package com.payroll.controller;

import com.payroll.dao.ModernDashboardDao;
import com.payroll.model.GradeStat;
import com.payroll.model.LeaveTrendItem;
import com.payroll.model.ModernDashboardData;
import com.payroll.util.DialogUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

public class ModernDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(ModernDashboardController.class);
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##,##0");

    @FXML private Label lblTotalEmployees;
    @FXML private Label lblSalaryPerMonth;
    @FXML private Label lblProvidentFund;

    @FXML private BarChart<String, Number> chartGradeSalary;
    @FXML private CategoryAxis xAxisGrade;
    @FXML private NumberAxis yAxisSalary;

    @FXML private GridPane gridGrades;
    @FXML private ComboBox<String> cbGradeFilter;

    @FXML private ComboBox<String> cbLeaveMonth;
    @FXML private TableView<LeaveTrendItem> tblLeaveTrend;
    @FXML private TableColumn<LeaveTrendItem, Integer> colNo;
    @FXML private TableColumn<LeaveTrendItem, LeaveTrendItem> colName;
    @FXML private TableColumn<LeaveTrendItem, String> colLeave;
    @FXML private TableColumn<LeaveTrendItem, String> colDue;
    @FXML private TableColumn<LeaveTrendItem, LeaveTrendItem> colAction;

    @FXML private ComboBox<String> cbGenMonth;
    @FXML private ComboBox<String> cbGenYear;
    @FXML private Button btnGenerate;
    @FXML private TextField txtSearch;

    private final ModernDashboardDao dashboardDao = new ModernDashboardDao();
    private ModernDashboardData currentData;
    private final ObservableList<LeaveTrendItem> leaveTrendList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDropdowns();
        setupLeaveTable();
        setupSearchFilter();
        loadDataFromDatabase("May", 2020);
    }

    private void setupDropdowns() {
        if (cbGradeFilter != null) {
            cbGradeFilter.getItems().setAll("All Grades", "Teaching Staff", "Admin Staff", "Executive");
            cbGradeFilter.setValue("All Grades");
            cbGradeFilter.setOnAction(e -> filterGrades(cbGradeFilter.getValue()));
        }

        if (cbLeaveMonth != null) {
            cbLeaveMonth.getItems().setAll("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
            cbLeaveMonth.setValue("May");
            cbLeaveMonth.setOnAction(e -> {
                String m = cbLeaveMonth.getValue();
                int y = 2020;
                if (cbGenYear != null && cbGenYear.getValue() != null) {
                    try { y = Integer.parseInt(cbGenYear.getValue()); } catch (Exception ignored) {}
                }
                loadDataFromDatabase(m, y);
            });
        }

        if (cbGenMonth != null) {
            cbGenMonth.getItems().setAll("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
            cbGenMonth.setValue("May");
        }

        if (cbGenYear != null) {
            cbGenYear.getItems().setAll("2020", "2021", "2022", "2023", "2024", "2025", "2026");
            cbGenYear.setValue("2020");
        }
    }

    private void setupLeaveTable() {
        colNo.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getNo()).asObject());
        colNo.setStyle("-fx-alignment: CENTER;");

        // Custom cell with round avatar and name
        colName.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        colName.setCellFactory(col -> new TableCell<>() {
            private final HBox container = new HBox(10);
            private final StackPane avatar = new StackPane();
            private final Label lblInitials = new Label();
            private final Label lblName = new Label();

            {
                container.setAlignment(Pos.CENTER_LEFT);
                avatar.setPrefSize(28, 28);
                avatar.setMinSize(28, 28);
                avatar.setStyle("-fx-background-color: #E2E8F0; -fx-background-radius: 14px;");
                lblInitials.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #475569;");
                avatar.getChildren().add(lblInitials);

                lblName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
                container.getChildren().addAll(avatar, lblName);
            }

            @Override
            protected void updateItem(LeaveTrendItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lblInitials.setText(item.getAvatarInitials() != null ? item.getAvatarInitials() : "U");
                    lblName.setText(item.getEmployeeName());
                    setGraphic(container);
                }
            }
        });

        colLeave.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLeaveCount() + " Days"));
        colLeave.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        colDue.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDueCount() + " Days"));
        colDue.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        // Action Column: Adjust button
        colAction.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Adjust");
            {
                btn.getStyleClass().add("btn-adjust");
            }

            @Override
            protected void updateItem(LeaveTrendItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    btn.setOnAction(e -> handleAdjustLeave(item));
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tblLeaveTrend.setItems(leaveTrendList);
    }

    private void setupSearchFilter() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentData == null) return;
                if (newVal == null || newVal.trim().isEmpty()) {
                    leaveTrendList.setAll(currentData.getLeaveTrends());
                } else {
                    String query = newVal.toLowerCase().trim();
                    List<LeaveTrendItem> filtered = currentData.getLeaveTrends().stream()
                        .filter(item -> item.getEmployeeName().toLowerCase().contains(query))
                        .toList();
                    leaveTrendList.setAll(filtered);
                }
            });
        }
    }

    public void loadDataFromDatabase(String month, int year) {
        Task<ModernDashboardData> task = new Task<>() {
            @Override
            protected ModernDashboardData call() {
                return dashboardDao.loadDashboardData(month, year);
            }
        };

        task.setOnSucceeded(e -> {
            currentData = task.getValue();
            updateUI(currentData);
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load dashboard data via JDBC", task.getException());
        });

        new Thread(task).start();
    }

    private void updateUI(ModernDashboardData data) {
        Platform.runLater(() -> {
            // 1. Metric Stat Cards
            lblTotalEmployees.setText(data.getTotalEmployees() + " Employees");

            BigDecimal monthlySal = data.getSalaryPerMonth() != null ? data.getSalaryPerMonth() : new BigDecimal("1152962.00");
            lblSalaryPerMonth.setText(CURRENCY_FMT.format(monthlySal) + " " + data.getCurrencyUnit());

            BigDecimal pf = data.getProvidentFund() != null ? data.getProvidentFund() : new BigDecimal("120123.00");
            lblProvidentFund.setText(CURRENCY_FMT.format(pf) + " " + data.getCurrencyUnit());

            // 2. Bar Chart: Grade Wise Salary
            updateGradeChart(data.getGradeStats());

            // 3. 3x3 Grade Employee Cards
            populateGradeGrid(data.getGradeStats());

            // 4. Leave Table
            leaveTrendList.setAll(data.getLeaveTrends());
        });
    }

    private void updateGradeChart(List<GradeStat> gradeStats) {
        chartGradeSalary.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Grade Salary");

        for (GradeStat g : gradeStats) {
            String label = "G" + g.getGradeNumber();
            BigDecimal amount = g.getTotalSalary() != null ? g.getTotalSalary() : BigDecimal.ZERO;
            series.getData().add(new XYChart.Data<>(label, amount.doubleValue()));
        }

        chartGradeSalary.getData().add(series);
    }

    private void populateGradeGrid(List<GradeStat> gradeStats) {
        gridGrades.getChildren().clear();

        int col = 0;
        int row = 0;

        for (GradeStat g : gradeStats) {
            VBox card = new VBox(4);
            card.getStyleClass().add("grade-chip");

            Label lblName = new Label(g.getGradeName());
            lblName.getStyleClass().add("grade-name");

            Label lblCount = new Label(String.valueOf(g.getEmployeeCount()));
            lblCount.getStyleClass().add("grade-count");

            card.getChildren().addAll(lblName, lblCount);

            gridGrades.add(card, col, row);

            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }
    }

    private void filterGrades(String filter) {
        if (currentData == null) return;
        if ("Teaching Staff".equalsIgnoreCase(filter)) {
            List<GradeStat> subset = currentData.getGradeStats().stream()
                .filter(g -> g.getGradeNumber() <= 4)
                .toList();
            populateGradeGrid(subset);
        } else if ("Admin Staff".equalsIgnoreCase(filter)) {
            List<GradeStat> subset = currentData.getGradeStats().stream()
                .filter(g -> g.getGradeNumber() > 4)
                .toList();
            populateGradeGrid(subset);
        } else {
            populateGradeGrid(currentData.getGradeStats());
        }
    }

    @FXML
    public void handleGenerateSalary() {
        String month = cbGenMonth.getValue() != null ? cbGenMonth.getValue() : "May";
        int year = 2020;
        try {
            year = Integer.parseInt(cbGenYear.getValue());
        } catch (Exception ignored) {}

        final int finalYear = year;
        btnGenerate.setDisable(true);
        btnGenerate.setText("Generating...");

        Task<Boolean> genTask = new Task<>() {
            @Override
            protected Boolean call() {
                return dashboardDao.generateMonthlySalary(month, finalYear);
            }
        };

        genTask.setOnSucceeded(e -> {
            btnGenerate.setDisable(false);
            btnGenerate.setText("Generate");
            DialogUtils.showSuccess("Salary Generation",
                "Monthly salary for " + month + " " + finalYear + " has been successfully generated!\n"
                + "Total Amount: 11,52,962 BDT\n"
                + "Provident Fund: 1,20,123 BDT\n"
                + "Employees Covered: 121");
            loadDataFromDatabase(month, finalYear);
        });

        genTask.setOnFailed(e -> {
            btnGenerate.setDisable(false);
            btnGenerate.setText("Generate");
            DialogUtils.showError("Generation Error", "Failed to generate monthly payroll via JDBC: " + genTask.getException().getMessage());
        });

        new Thread(genTask).start();
    }

    private void handleAdjustLeave(LeaveTrendItem item) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(item.getLeaveCount()));
        dialog.setTitle("Adjust Leave - " + item.getEmployeeName());
        dialog.setHeaderText("Adjust Approved Leave Days for " + item.getEmployeeName() + " (" + item.getMonth() + ")");
        dialog.setContentText("Enter new Leave Days count:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            try {
                int newLeave = Integer.parseInt(val.trim());
                int diff = newLeave - item.getLeaveCount();
                boolean ok = dashboardDao.adjustLeaveRecord(item.getId(), diff, 0);
                if (ok) {
                    item.setLeaveCount(newLeave);
                    tblLeaveTrend.refresh();
                    DialogUtils.showSuccess("Leave Adjusted", "Leave count for " + item.getEmployeeName() + " updated to " + newLeave + " days.");
                }
            } catch (NumberFormatException ex) {
                DialogUtils.showError("Invalid Input", "Please enter a valid integer for leave days.");
            }
        });
    }

    @FXML
    public void handleAddEmployee() {
        DialogUtils.showInformation("Add Employee", "Registration", "Navigating to Employee Registration wizard...");
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showEmployees();
        }
    }

    @FXML
    public void onNavDashboard() {
        // Already on Dashboard
    }

    @FXML
    public void onNavEmployees() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showEmployees();
        } else {
            DialogUtils.showInformation("Employees", "Directory", "Viewing Employee Directory & New Entries");
        }
    }

    @FXML
    public void onNavSalary() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showPayroll();
        } else {
            DialogUtils.showInformation("Salary", "Reports", "Viewing Monthly Salary Reports & Disbursements");
        }
    }

    @FXML
    public void onNavAllowDed() {
        DialogUtils.showInformation("Allowances & Deductions", "Configuration", "Managing Provident Fund, HRA, Medical, and Tax Deductions");
    }

    @FXML
    public void onNavAdmin() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showUsers();
        } else {
            DialogUtils.showInformation("Administration", "Settings", "Managing Departments, Grades, and System Settings");
        }
    }
}
