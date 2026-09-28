package com.payroll.controller;

import com.payroll.model.DashboardSummary;
import com.payroll.model.DepartmentHeadcount;
import com.payroll.model.MonthlyTrendItem;
import com.payroll.model.PayrollRun;
import com.payroll.service.DashboardService;
import com.payroll.util.CurrencyUtils;
import com.payroll.util.DateUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Label lblTotalEmployees;
    @FXML private Label lblActiveInactive;
    @FXML private Label lblCurrentMonthPayroll;
    @FXML private Label lblPendingApprovals;
    @FXML private Label lblTotalPaid;
    @FXML private Label lblUnpaidSubtext;

    @FXML private BarChart<String, Number> chartPayrollTrends;
    @FXML private CategoryAxis xAxisTrend;
    @FXML private NumberAxis yAxisTrend;

    @FXML private PieChart chartDeptDistribution;

    @FXML private TableView<PayrollRun> tblRecentRuns;
    @FXML private TableColumn<PayrollRun, String> colRunPeriod;
    @FXML private TableColumn<PayrollRun, String> colRunDate;
    @FXML private TableColumn<PayrollRun, String> colRunEmployees;
    @FXML private TableColumn<PayrollRun, String> colRunNetPay;
    @FXML private TableColumn<PayrollRun, String> colRunStatus;

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadDashboardData();
    }

    private void setupTableColumns() {
        colRunPeriod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPeriodDisplay()));
        colRunDate.setCellValueFactory(cell -> new SimpleStringProperty(DateUtils.formatDate(cell.getValue().getRunDate())));
        colRunEmployees.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getTotalEmployees())));
        colRunNetPay.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyUtils.format(cell.getValue().getTotalNetPay())));

        colRunStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        colRunStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("PAID".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-paid");
                    } else if ("APPROVED".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-approved");
                    } else if ("DRAFT".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-draft");
                    } else {
                        badge.getStyleClass().add("badge-cancelled");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void loadDashboardData() {
        Task<DashboardSummary> task = new Task<>() {
            @Override
            protected DashboardSummary call() {
                return dashboardService.getDashboardSummary();
            }
        };

        task.setOnSucceeded(e -> {
            DashboardSummary summary = task.getValue();
            updateUI(summary);
        });

        task.setOnFailed(e -> logger.error("Failed to load dashboard data", task.getException()));

        new Thread(task).start();
    }

    private void updateUI(DashboardSummary summary) {
        lblTotalEmployees.setText(String.valueOf(summary.getTotalEmployees()));
        lblActiveInactive.setText("Active: " + summary.getActiveEmployees() + "  |  Inactive: " + summary.getInactiveEmployees());

        lblCurrentMonthPayroll.setText(CurrencyUtils.format(summary.getCurrentMonthPayroll()));
        lblPendingApprovals.setText(String.valueOf(summary.getPendingApprovals()));
        lblTotalPaid.setText(CurrencyUtils.format(summary.getTotalPaidPayroll()));
        lblUnpaidSubtext.setText("Pending/Approved: " + CurrencyUtils.format(summary.getTotalUnpaidPayroll()));

        // Update Charts
        updateTrendsChart(summary);
        updateDeptPieChart(summary);

        // Update Table
        tblRecentRuns.setItems(FXCollections.observableArrayList(summary.getRecentPayrollRuns()));
    }

    private void updateTrendsChart(DashboardSummary summary) {
        chartPayrollTrends.getData().clear();

        XYChart.Series<String, Number> grossSeries = new XYChart.Series<>();
        grossSeries.setName("Gross Pay");

        XYChart.Series<String, Number> netSeries = new XYChart.Series<>();
        netSeries.setName("Net Pay");

        for (MonthlyTrendItem item : summary.getMonthlyTrends()) {
            grossSeries.getData().add(new XYChart.Data<>(item.getMonthLabel(), item.getGrossAmount()));
            netSeries.getData().add(new XYChart.Data<>(item.getMonthLabel(), item.getNetAmount()));
        }

        chartPayrollTrends.getData().addAll(grossSeries, netSeries);
    }

    private void updateDeptPieChart(DashboardSummary summary) {
        chartDeptDistribution.getData().clear();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (DepartmentHeadcount dh : summary.getDepartmentDistribution()) {
            if (dh.getHeadcount() > 0) {
                pieData.add(new PieChart.Data(dh.getDepartmentName() + " (" + dh.getHeadcount() + ")", dh.getHeadcount()));
            }
        }
        chartDeptDistribution.setData(pieData);
    }

    @FXML
    private void navigateToAddEmployee() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showEmployees();
        }
    }

    @FXML
    private void navigateToPayroll() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showPayroll();
        }
    }

    @FXML
    private void navigateToReports() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showReports();
        }
    }

    @FXML
    private void navigateToLeaves() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showLeaves();
        }
    }
}
