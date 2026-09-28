package com.payroll.controller;

import com.payroll.model.Employee;
import com.payroll.model.LeaveRequest;
import com.payroll.service.EmployeeService;
import com.payroll.service.LeaveService;
import com.payroll.service.ReportService;
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
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LeaveManagementController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveManagementController.class);

    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Label lblLeaveSummary;

    @FXML private TableView<LeaveRequest> tblLeaves;
    @FXML private TableColumn<LeaveRequest, String> colLeaveCode;
    @FXML private TableColumn<LeaveRequest, String> colLeaveName;
    @FXML private TableColumn<LeaveRequest, String> colLeaveDept;
    @FXML private TableColumn<LeaveRequest, String> colLeaveType;
    @FXML private TableColumn<LeaveRequest, String> colLeaveStart;
    @FXML private TableColumn<LeaveRequest, String> colLeaveEnd;
    @FXML private TableColumn<LeaveRequest, String> colLeaveDays;
    @FXML private TableColumn<LeaveRequest, String> colLeaveReason;
    @FXML private TableColumn<LeaveRequest, String> colLeaveStatus;
    @FXML private TableColumn<LeaveRequest, Void> colLeaveActions;

    private final LeaveService leaveService = new LeaveService();
    private final EmployeeService employeeService = new EmployeeService();
    private final ReportService reportService = new ReportService();

    private final ObservableList<LeaveRequest> leaveList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilter();
        setupTableColumns();
        loadLeaves();
    }

    private void setupFilter() {
        cmbStatusFilter.setItems(FXCollections.observableArrayList(
                "All Requests", "PENDING", "APPROVED", "REJECTED", "CANCELLED"
        ));
        cmbStatusFilter.getSelectionModel().selectFirst();
        cmbStatusFilter.setOnAction(e -> loadLeaves());
    }

    private void setupTableColumns() {
        colLeaveCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeCode()));
        colLeaveName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeName()));
        colLeaveDept.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartmentName()));
        colLeaveType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLeaveType().name()));
        colLeaveStart.setCellValueFactory(c -> new SimpleStringProperty(DateUtils.formatDate(c.getValue().getStartDate())));
        colLeaveEnd.setCellValueFactory(c -> new SimpleStringProperty(DateUtils.formatDate(c.getValue().getEndDate())));
        colLeaveDays.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTotalDays().toString()));
        colLeaveReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReason()));

        colLeaveStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colLeaveStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("badge");
                    if ("APPROVED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-approved");
                    } else if ("PENDING".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-pending");
                    } else {
                        badge.getStyleClass().add("badge-rejected");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colLeaveActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("Approve");
            private final Button btnReject = new Button("Reject");
            private final HBox box = new HBox(6, btnApprove, btnReject);
            {
                btnApprove.getStyleClass().addAll("btn-success", "btn-sm");
                btnReject.getStyleClass().addAll("btn-danger", "btn-sm");
                box.setAlignment(Pos.CENTER);

                btnApprove.setOnAction(e -> handleReview(getTableView().getItems().get(getIndex()), LeaveRequest.LeaveStatus.APPROVED));
                btnReject.setOnAction(e -> handleReview(getTableView().getItems().get(getIndex()), LeaveRequest.LeaveStatus.REJECTED));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    LeaveRequest req = getTableView().getItems().get(getIndex());
                    if (req.getStatus() == LeaveRequest.LeaveStatus.PENDING) {
                        setGraphic(box);
                    } else {
                        Label doneLabel = new Label("Completed");
                        doneLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");
                        setGraphic(doneLabel);
                    }
                }
            }
        });

        tblLeaves.setItems(leaveList);
    }

    @FXML
    public void loadLeaves() {
        String filter = cmbStatusFilter.getValue();
        Task<List<LeaveRequest>> task = new Task<>() {
            @Override
            protected List<LeaveRequest> call() {
                if (filter != null && !filter.startsWith("All")) {
                    return leaveService.getLeavesByStatus(LeaveRequest.LeaveStatus.valueOf(filter));
                }
                return leaveService.getAllLeaves();
            }
        };

        task.setOnSucceeded(e -> {
            leaveList.setAll(task.getValue());
            lblLeaveSummary.setText("Leave Applications (" + leaveList.size() + " total, " + leaveService.countPendingLeaves() + " pending review)");
        });

        new Thread(task).start();
    }

    private void handleReview(LeaveRequest req, LeaveRequest.LeaveStatus status) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Review Leave Application");
        dialog.setHeaderText("Action: " + status + " for " + req.getEmployeeName() + " (" + req.getLeaveType() + ")");
        dialog.setContentText("Enter review comments/reason:");

        dialog.showAndWait().ifPresent(comments -> {
            leaveService.reviewLeave(req.getId(), status, comments);
            DialogUtils.showSuccess("Application Reviewed", "Leave marked as " + status);
            loadLeaves();
        });
    }

    @FXML
    public void showApplyLeaveDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Apply for Employee Leave");
        dialog.setHeaderText("Submit a leave application for approval.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Submit Application", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        ComboBox<Employee> cmbEmp = new ComboBox<>(FXCollections.observableArrayList(employeeService.getActiveEmployees()));
        ComboBox<LeaveRequest.LeaveType> cmbType = new ComboBox<>(FXCollections.observableArrayList(LeaveRequest.LeaveType.values()));
        cmbType.setValue(LeaveRequest.LeaveType.CASUAL);

        DatePicker dpStart = new DatePicker(LocalDate.now());
        DatePicker dpEnd = new DatePicker(LocalDate.now());
        TextField txtDays = new TextField("1");
        TextArea txtReason = new TextArea();
        txtReason.setPrefRowCount(3);

        dpEnd.setOnAction(e -> {
            if (dpStart.getValue() != null && dpEnd.getValue() != null) {
                long days = ChronoUnit.DAYS.between(dpStart.getValue(), dpEnd.getValue()) + 1;
                if (days > 0) txtDays.setText(String.valueOf(days));
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Employee:*"), cmbEmp);
        grid.addRow(1, new Label("Leave Type:*"), cmbType);
        grid.addRow(2, new Label("Start Date:*"), dpStart);
        grid.addRow(3, new Label("End Date:*"), dpEnd);
        grid.addRow(4, new Label("Total Days:*"), txtDays);
        grid.addRow(5, new Label("Reason:*"), txtReason);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    if (cmbEmp.getValue() == null) throw new IllegalArgumentException("Please select an employee.");
                    LeaveRequest req = new LeaveRequest();
                    req.setEmployeeId(cmbEmp.getValue().getId());
                    req.setLeaveType(cmbType.getValue());
                    req.setStartDate(dpStart.getValue());
                    req.setEndDate(dpEnd.getValue());
                    req.setTotalDays(new BigDecimal(txtDays.getText()));
                    req.setReason(txtReason.getText());

                    leaveService.applyLeave(req);
                    DialogUtils.showSuccess("Submitted", "Leave application submitted for review.");
                    loadLeaves();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Submission Failed", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    public void handleExportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Leave Records to CSV");
        fileChooser.setInitialFileName("Leave_Records_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(tblLeaves.getScene().getWindow());

        if (file != null) {
            try {
                reportService.exportLeavesCsv(file);
                DialogUtils.showSuccess("Export Successful", "Leave records exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Export Failed", e.getMessage());
            }
        }
    }
}
