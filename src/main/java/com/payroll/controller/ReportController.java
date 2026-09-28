package com.payroll.controller;

import com.payroll.model.Employee;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import com.payroll.service.EmployeeService;
import com.payroll.service.PayrollService;
import com.payroll.service.ReportService;
import com.payroll.util.DialogUtils;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @FXML private ComboBox<Employee> cmbPayslipEmployee;
    @FXML private ComboBox<PayrollRecord> cmbPayslipPeriod;

    @FXML private ComboBox<PayrollRun> cmbRegisterRun;

    @FXML private ComboBox<String> cmbAttMonth;
    @FXML private ComboBox<Integer> cmbAttYear;

    private final EmployeeService employeeService = new EmployeeService();
    private final PayrollService payrollService = new PayrollService();
    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        loadEmployees();
        loadPayrollRuns();
        setupAttendanceSelectors();

        cmbPayslipEmployee.setOnAction(e -> {
            Employee emp = cmbPayslipEmployee.getValue();
            if (emp != null) {
                loadPayslipRecordsForEmployee(emp);
            }
        });
    }

    private void loadEmployees() {
        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() {
                return employeeService.getAllEmployees();
            }
        };
        task.setOnSucceeded(e -> {
            cmbPayslipEmployee.setItems(FXCollections.observableArrayList(task.getValue()));
            if (!cmbPayslipEmployee.getItems().isEmpty()) {
                cmbPayslipEmployee.getSelectionModel().selectFirst();
                loadPayslipRecordsForEmployee(cmbPayslipEmployee.getItems().get(0));
            }
        });
        new Thread(task).start();
    }

    private void loadPayslipRecordsForEmployee(Employee emp) {
        Task<List<PayrollRecord>> task = new Task<>() {
            @Override
            protected List<PayrollRecord> call() {
                return payrollService.getPayrollRecordsForEmployee(emp.getId());
            }
        };
        task.setOnSucceeded(e -> {
            cmbPayslipPeriod.setItems(FXCollections.observableArrayList(task.getValue()));
            if (!cmbPayslipPeriod.getItems().isEmpty()) {
                cmbPayslipPeriod.getSelectionModel().selectFirst();
            }
        });
        new Thread(task).start();
    }

    private void loadPayrollRuns() {
        Task<List<PayrollRun>> task = new Task<>() {
            @Override
            protected List<PayrollRun> call() {
                return payrollService.getAllPayrollRuns();
            }
        };
        task.setOnSucceeded(e -> {
            cmbRegisterRun.setItems(FXCollections.observableArrayList(task.getValue()));
            if (!cmbRegisterRun.getItems().isEmpty()) {
                cmbRegisterRun.getSelectionModel().selectFirst();
            }
        });
        new Thread(task).start();
    }

    private void setupAttendanceSelectors() {
        cmbAttMonth.setItems(FXCollections.observableArrayList(
                "1 - January", "2 - February", "3 - March", "4 - April",
                "5 - May", "6 - June", "7 - July", "8 - August",
                "9 - September", "10 - October", "11 - November", "12 - December"
        ));
        LocalDate now = LocalDate.now();
        cmbAttMonth.getSelectionModel().select(now.getMonthValue() - 1);
        cmbAttYear.setItems(FXCollections.observableArrayList(2023, 2024, 2025, 2026, 2027));
        cmbAttYear.setValue(now.getYear());
    }

    @FXML
    public void handleGeneratePdfPayslip() {
        PayrollRecord record = cmbPayslipPeriod.getValue();
        if (record == null) {
            DialogUtils.showWarning("Selection Required", "No payslip record selected or no payroll runs generated for this employee yet.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Payslip");
        fileChooser.setInitialFileName("Payslip_" + record.getEmployeeCode() + "_" + record.getPayrollMonth() + "_" + record.getPayrollYear() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        File file = fileChooser.showSaveDialog(cmbPayslipEmployee.getScene().getWindow());

        if (file != null) {
            try {
                reportService.generatePayslipPdf(record.getId(), file);
                DialogUtils.showSuccess("Payslip Generated", "PDF payslip generated successfully at:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Generation Error", e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportPayrollRegister() {
        PayrollRun run = cmbRegisterRun.getValue();
        if (run == null) {
            DialogUtils.showWarning("Selection Required", "Please choose a payroll run first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Payroll Register");
        fileChooser.setInitialFileName("Payroll_Register_" + run.getPayrollYear() + "_" + run.getPayrollMonth() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(cmbRegisterRun.getScene().getWindow());

        if (file != null) {
            try {
                reportService.exportPayrollRegisterCsv(run.getId(), file);
                DialogUtils.showSuccess("Export Complete", "Register exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportEmployeeDirectory() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Employee Directory");
        fileChooser.setInitialFileName("Employee_Directory_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(cmbRegisterRun.getScene().getWindow());

        if (file != null) {
            try {
                reportService.exportEmployeesCsv(file, null, null, null, null);
                DialogUtils.showSuccess("Export Complete", "Employee directory exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportAttendance() {
        int m = cmbAttMonth.getSelectionModel().getSelectedIndex() + 1;
        int y = cmbAttYear.getValue() != null ? cmbAttYear.getValue() : LocalDate.now().getYear();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Attendance Log");
        fileChooser.setInitialFileName("Attendance_Log_" + y + "_" + m + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(cmbRegisterRun.getScene().getWindow());

        if (file != null) {
            try {
                reportService.exportAttendanceCsv(m, y, file);
                DialogUtils.showSuccess("Export Complete", "Attendance register exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Export Failed", e.getMessage());
            }
        }
    }
}
