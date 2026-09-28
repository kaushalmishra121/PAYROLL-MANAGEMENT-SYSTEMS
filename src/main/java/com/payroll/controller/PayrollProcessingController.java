package com.payroll.controller;

import com.payroll.model.PayrollAdjustment;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import com.payroll.service.PayrollService;
import com.payroll.service.ReportService;
import com.payroll.util.CurrencyUtils;
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
import java.util.List;

public class PayrollProcessingController {
    private static final Logger logger = LoggerFactory.getLogger(PayrollProcessingController.class);

    @FXML private TableView<PayrollRun> tblPayrollRuns;
    @FXML private TableColumn<PayrollRun, String> colRunPeriod;
    @FXML private TableColumn<PayrollRun, String> colRunDate;
    @FXML private TableColumn<PayrollRun, String> colRunHeadcount;
    @FXML private TableColumn<PayrollRun, String> colRunGross;
    @FXML private TableColumn<PayrollRun, String> colRunDeductions;
    @FXML private TableColumn<PayrollRun, String> colRunNet;
    @FXML private TableColumn<PayrollRun, String> colRunStatus;
    @FXML private TableColumn<PayrollRun, Void> colRunActions;

    @FXML private Label lblRegisterTitle;
    @FXML private Label lblRegisterSubtitle;
    @FXML private Button btnExportRegisterCsv;

    @FXML private TableView<PayrollRecord> tblPayrollRecords;
    @FXML private TableColumn<PayrollRecord, String> colRecCode;
    @FXML private TableColumn<PayrollRecord, String> colRecName;
    @FXML private TableColumn<PayrollRecord, String> colRecDept;
    @FXML private TableColumn<PayrollRecord, String> colRecDays;
    @FXML private TableColumn<PayrollRecord, String> colRecGross;
    @FXML private TableColumn<PayrollRecord, String> colRecPf;
    @FXML private TableColumn<PayrollRecord, String> colRecTax;
    @FXML private TableColumn<PayrollRecord, String> colRecLoss;
    @FXML private TableColumn<PayrollRecord, String> colRecDeductions;
    @FXML private TableColumn<PayrollRecord, String> colRecNet;
    @FXML private TableColumn<PayrollRecord, Void> colRecActions;

    private final PayrollService payrollService = new PayrollService();
    private final ReportService reportService = new ReportService();

    private final ObservableList<PayrollRun> runList = FXCollections.observableArrayList();
    private final ObservableList<PayrollRecord> recordList = FXCollections.observableArrayList();
    private PayrollRun currentSelectedRun;

    @FXML
    public void initialize() {
        setupRunColumns();
        setupRecordColumns();
        loadPayrollRuns();

        tblPayrollRuns.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadRecordsForRun(newVal);
            }
        });
    }

    private void setupRunColumns() {
        colRunPeriod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPeriodDisplay()));
        colRunDate.setCellValueFactory(c -> new SimpleStringProperty(DateUtils.formatDate(c.getValue().getRunDate())));
        colRunHeadcount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTotalEmployees())));
        colRunGross.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getTotalGrossPay())));
        colRunDeductions.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getTotalDeductions())));
        colRunNet.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getTotalNetPay())));

        colRunStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colRunStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("badge");
                    if ("PAID".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-paid");
                    } else if ("APPROVED".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-approved");
                    } else if ("DRAFT".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-draft");
                    } else {
                        badge.getStyleClass().add("badge-cancelled");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colRunActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("Approve");
            private final Button btnPay = new Button("Pay");
            private final Button btnCancel = new Button("Cancel");
            private final HBox box = new HBox(6, btnApprove, btnPay, btnCancel);
            {
                btnApprove.getStyleClass().addAll("btn-success", "btn-sm");
                btnPay.getStyleClass().addAll("btn-primary", "btn-sm");
                btnCancel.getStyleClass().addAll("btn-danger", "btn-sm");
                box.setAlignment(Pos.CENTER);

                btnApprove.setOnAction(e -> handleApproveRun(getTableView().getItems().get(getIndex())));
                btnPay.setOnAction(e -> handleMarkPaid(getTableView().getItems().get(getIndex())));
                btnCancel.setOnAction(e -> handleCancelRun(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PayrollRun r = getTableView().getItems().get(getIndex());
                    btnApprove.setVisible(r.getStatus() == PayrollRun.PayrollStatus.DRAFT);
                    btnPay.setVisible(r.getStatus() == PayrollRun.PayrollStatus.APPROVED);
                    btnCancel.setVisible(r.getStatus() != PayrollRun.PayrollStatus.PAID && r.getStatus() != PayrollRun.PayrollStatus.CANCELLED);
                    setGraphic(box);
                }
            }
        });

        tblPayrollRuns.setItems(runList);
    }

    private void setupRecordColumns() {
        colRecCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeCode()));
        colRecName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeName()));
        colRecDept.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartmentName()));
        colRecDays.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPayableDays() + " / " + c.getValue().getTotalWorkingDays()));
        colRecGross.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getGrossEarnings())));
        colRecPf.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getPfDeduction())));
        colRecTax.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getTdsDeduction().add(c.getValue().getProfessionalTax()))));
        colRecLoss.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getUnpaidLeaveDeduction())));
        colRecDeductions.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getTotalDeductions())));
        colRecNet.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getNetSalary())));

        colRecActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPdf = new Button("📄 PDF Payslip");
            private final Button btnAdj = new Button("± Adjust");
            private final HBox box = new HBox(6, btnPdf, btnAdj);
            {
                btnPdf.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnAdj.getStyleClass().addAll("btn-secondary", "btn-sm");
                box.setAlignment(Pos.CENTER);

                btnPdf.setOnAction(e -> generatePdfPayslip(getTableView().getItems().get(getIndex())));
                btnAdj.setOnAction(e -> showAdjustmentDialog(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblPayrollRecords.setItems(recordList);
    }

    public void loadPayrollRuns() {
        Task<List<PayrollRun>> task = new Task<>() {
            @Override
            protected List<PayrollRun> call() {
                return payrollService.getAllPayrollRuns();
            }
        };

        task.setOnSucceeded(e -> {
            runList.setAll(task.getValue());
            if (!runList.isEmpty()) {
                tblPayrollRuns.getSelectionModel().selectFirst();
            }
        });

        new Thread(task).start();
    }

    public void loadRecordsForRun(PayrollRun run) {
        this.currentSelectedRun = run;
        lblRegisterTitle.setText("Payroll Register - " + run.getPeriodDisplay() + " (" + run.getStatus() + ")");
        lblRegisterSubtitle.setText("Total Gross: " + CurrencyUtils.format(run.getTotalGrossPay()) + " | Total Deductions: " + CurrencyUtils.format(run.getTotalDeductions()) + " | Total Net: " + CurrencyUtils.format(run.getTotalNetPay()));

        Task<List<PayrollRecord>> task = new Task<>() {
            @Override
            protected List<PayrollRecord> call() {
                return payrollService.getPayrollRecords(run.getId());
            }
        };

        task.setOnSucceeded(e -> recordList.setAll(task.getValue()));
        new Thread(task).start();
    }

    @FXML
    public void showProcessPayrollDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Process Monthly Payroll");
        dialog.setHeaderText("Execute batch financial calculation for all active employees.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnRunType = new ButtonType("Compute Payroll", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnRunType, ButtonType.CANCEL);

        ObservableList<String> months = FXCollections.observableArrayList(
                "1 - January", "2 - February", "3 - March", "4 - April",
                "5 - May", "6 - June", "7 - July", "8 - August",
                "9 - September", "10 - October", "11 - November", "12 - December"
        );
        ComboBox<String> cmbMonth = new ComboBox<>(months);
        LocalDate now = LocalDate.now();
        cmbMonth.getSelectionModel().select(now.getMonthValue() - 1);

        ObservableList<Integer> years = FXCollections.observableArrayList(2023, 2024, 2025, 2026, 2027);
        ComboBox<Integer> cmbYear = new ComboBox<>(years);
        cmbYear.setValue(now.getYear());

        TextField txtNotes = new TextField("Monthly automated batch run.");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Payroll Month:*"), cmbMonth);
        grid.addRow(1, new Label("Payroll Year:*"), cmbYear);
        grid.addRow(2, new Label("Notes / Batch Memo:"), txtNotes);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnRunType) {
                int m = cmbMonth.getSelectionModel().getSelectedIndex() + 1;
                int y = cmbYear.getValue();
                try {
                    PayrollRun run = payrollService.processMonthlyPayroll(m, y, txtNotes.getText());
                    DialogUtils.showSuccess("Payroll Processed", "Successfully computed draft payroll for " + run.getPeriodDisplay() + " for " + run.getTotalEmployees() + " employees.");
                    loadPayrollRuns();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Calculation Failed", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleApproveRun(PayrollRun run) {
        if (DialogUtils.showConfirmation("Approve Payroll Run", "Approve payroll run for " + run.getPeriodDisplay() + "?", "This will transition status to APPROVED.")) {
            try {
                payrollService.approvePayroll(run.getId());
                DialogUtils.showSuccess("Payroll Approved", "Payroll run approved successfully.");
                loadPayrollRuns();
            } catch (Exception ex) {
                DialogUtils.showError("Approval Failed", ex.getMessage());
            }
        }
    }

    private void handleMarkPaid(PayrollRun run) {
        if (DialogUtils.showConfirmation("Disburse Payroll", "Mark payroll for " + run.getPeriodDisplay() + " as PAID?", "Total disbursement: " + CurrencyUtils.format(run.getTotalNetPay()))) {
            try {
                payrollService.markPayrollPaid(run.getId(), LocalDate.now());
                DialogUtils.showSuccess("Disbursement Complete", "Payroll marked as PAID and disbursement recorded.");
                loadPayrollRuns();
            } catch (Exception ex) {
                DialogUtils.showError("Payment Failed", ex.getMessage());
            }
        }
    }

    private void handleCancelRun(PayrollRun run) {
        if (DialogUtils.showConfirmation("Cancel Payroll Run", "Cancel payroll for " + run.getPeriodDisplay() + "?", "Status will be set to CANCELLED.")) {
            try {
                payrollService.cancelPayroll(run.getId());
                DialogUtils.showSuccess("Cancelled", "Payroll run cancelled.");
                loadPayrollRuns();
            } catch (Exception ex) {
                DialogUtils.showError("Cancellation Failed", ex.getMessage());
            }
        }
    }

    private void showAdjustmentDialog(PayrollRecord record) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Payroll Adjustment - " + record.getEmployeeName());
        dialog.setHeaderText("Add an auditable addition or deduction to this employee's payslip.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Apply Adjustment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        ComboBox<PayrollAdjustment.AdjustmentType> cmbType = new ComboBox<>(FXCollections.observableArrayList(PayrollAdjustment.AdjustmentType.values()));
        cmbType.setValue(PayrollAdjustment.AdjustmentType.ADDITION);

        TextField txtAmount = new TextField("500.00");
        TextField txtReason = new TextField("Performance bonus or authorized adjustment");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Adjustment Type:*"), cmbType);
        grid.addRow(1, new Label("Amount ($):*"), txtAmount);
        grid.addRow(2, new Label("Reason / Memo:*"), txtReason);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    BigDecimal amt = CurrencyUtils.parse(txtAmount.getText());
                    payrollService.addAdjustment(record.getId(), cmbType.getValue(), amt, txtReason.getText());
                    DialogUtils.showSuccess("Adjustment Applied", "Salary adjusted successfully.");
                    if (currentSelectedRun != null) loadRecordsForRun(currentSelectedRun);
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Adjustment Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void generatePdfPayslip(PayrollRecord record) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Payslip");
        fileChooser.setInitialFileName("Payslip_" + record.getEmployeeCode() + "_" + record.getPayrollMonth() + "_" + record.getPayrollYear() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        File file = fileChooser.showSaveDialog(tblPayrollRecords.getScene().getWindow());

        if (file != null) {
            try {
                reportService.generatePayslipPdf(record.getId(), file);
                DialogUtils.showSuccess("PDF Payslip Generated", "Saved successfully to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Payslip Generation Error", e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportRegisterCsv() {
        if (currentSelectedRun == null) {
            DialogUtils.showWarning("No Run Selected", "Please select a payroll run from the table above first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Payroll Register CSV");
        fileChooser.setInitialFileName("Payroll_Register_" + currentSelectedRun.getPayrollYear() + "_" + currentSelectedRun.getPayrollMonth() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(tblPayrollRecords.getScene().getWindow());

        if (file != null) {
            try {
                reportService.exportPayrollRegisterCsv(currentSelectedRun.getId(), file);
                DialogUtils.showSuccess("Export Successful", "Payroll register exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Export Failed", e.getMessage());
            }
        }
    }
}
