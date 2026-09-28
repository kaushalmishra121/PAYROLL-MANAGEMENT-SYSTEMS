package com.payroll.controller;

import com.payroll.model.Attendance;
import com.payroll.model.Employee;
import com.payroll.service.AttendanceService;
import com.payroll.service.EmployeeService;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AttendanceController {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    @FXML private ComboBox<String> cmbMonth;
    @FXML private ComboBox<Integer> cmbYear;
    @FXML private Label lblTableSummary;

    @FXML private TableView<Attendance> tblAttendance;
    @FXML private TableColumn<Attendance, String> colAttDate;
    @FXML private TableColumn<Attendance, String> colAttCode;
    @FXML private TableColumn<Attendance, String> colAttName;
    @FXML private TableColumn<Attendance, String> colAttDept;
    @FXML private TableColumn<Attendance, String> colAttStatus;
    @FXML private TableColumn<Attendance, String> colAttInTime;
    @FXML private TableColumn<Attendance, String> colAttOutTime;
    @FXML private TableColumn<Attendance, String> colAttNotes;
    @FXML private TableColumn<Attendance, Void> colAttActions;

    private final AttendanceService attendanceService = new AttendanceService();
    private final EmployeeService employeeService = new EmployeeService();
    private final ReportService reportService = new ReportService();

    private final ObservableList<Attendance> attendanceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDateFilters();
        setupTableColumns();
        loadAttendance();
    }

    private void setupDateFilters() {
        ObservableList<String> months = FXCollections.observableArrayList(
                "1 - January", "2 - February", "3 - March", "4 - April",
                "5 - May", "6 - June", "7 - July", "8 - August",
                "9 - September", "10 - October", "11 - November", "12 - December"
        );
        cmbMonth.setItems(months);
        LocalDate now = LocalDate.now();
        cmbMonth.getSelectionModel().select(now.getMonthValue() - 1);

        ObservableList<Integer> years = FXCollections.observableArrayList(2023, 2024, 2025, 2026, 2027);
        cmbYear.setItems(years);
        cmbYear.setValue(now.getYear());
    }

    private void setupTableColumns() {
        colAttDate.setCellValueFactory(c -> new SimpleStringProperty(DateUtils.formatDate(c.getValue().getAttendanceDate())));
        colAttCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeCode()));
        colAttName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeName()));
        colAttDept.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartmentName()));
        colAttInTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckInTime() != null ? c.getValue().getCheckInTime().toString() : "-"));
        colAttOutTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCheckOutTime() != null ? c.getValue().getCheckOutTime().toString() : "-"));
        colAttNotes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes() != null ? c.getValue().getNotes() : ""));

        colAttStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colAttStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("badge");
                    if ("PRESENT".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-active");
                    } else if ("HALF_DAY".equalsIgnoreCase(status) || "ON_LEAVE".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-draft");
                    } else {
                        badge.getStyleClass().add("badge-inactive");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colAttActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDel = new Button("Delete");
            private final HBox box = new HBox(6, btnEdit, btnDel);
            {
                btnEdit.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnDel.getStyleClass().addAll("btn-danger", "btn-sm");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> showEditAttendanceDialog(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> handleDeleteAttendance(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblAttendance.setItems(attendanceList);
    }

    @FXML
    public void loadAttendance() {
        int month = cmbMonth.getSelectionModel().getSelectedIndex() + 1;
        int year = cmbYear.getValue() != null ? cmbYear.getValue() : LocalDate.now().getYear();

        Task<List<Attendance>> task = new Task<>() {
            @Override
            protected List<Attendance> call() {
                return attendanceService.getAttendanceForMonth(month, year);
            }
        };

        task.setOnSucceeded(e -> {
            attendanceList.setAll(task.getValue());
            lblTableSummary.setText("Attendance Log - " + DateUtils.getMonthName(month) + " " + year + " (" + attendanceList.size() + " records)");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleExportCsv() {
        int month = cmbMonth.getSelectionModel().getSelectedIndex() + 1;
        int year = cmbYear.getValue() != null ? cmbYear.getValue() : LocalDate.now().getYear();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Attendance to CSV");
        fileChooser.setInitialFileName("Attendance_" + year + "_" + month + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(tblAttendance.getScene().getWindow());

        if (file != null) {
            try {
                reportService.exportAttendanceCsv(month, year, file);
                DialogUtils.showSuccess("Export Successful", "Attendance records exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    public void showRecordAttendanceDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Record Daily Attendance");
        dialog.setHeaderText("Log daily attendance entry for an employee.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Save Record", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        ComboBox<Employee> cmbEmp = new ComboBox<>(FXCollections.observableArrayList(employeeService.getActiveEmployees()));
        DatePicker dpDate = new DatePicker(LocalDate.now());
        ComboBox<Attendance.AttendanceStatus> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(Attendance.AttendanceStatus.values()));
        cmbStatus.setValue(Attendance.AttendanceStatus.PRESENT);

        TextField txtInTime = new TextField("09:00:00");
        TextField txtOutTime = new TextField("17:30:00");
        TextField txtNotes = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Employee:*"), cmbEmp);
        grid.addRow(1, new Label("Attendance Date:*"), dpDate);
        grid.addRow(2, new Label("Status:*"), cmbStatus);
        grid.addRow(3, new Label("Check-In Time:"), txtInTime);
        grid.addRow(4, new Label("Check-Out Time:"), txtOutTime);
        grid.addRow(5, new Label("Notes:"), txtNotes);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    if (cmbEmp.getValue() == null) {
                        throw new IllegalArgumentException("Please select an employee.");
                    }
                    Attendance a = new Attendance();
                    a.setEmployeeId(cmbEmp.getValue().getId());
                    a.setAttendanceDate(dpDate.getValue());
                    a.setStatus(cmbStatus.getValue());
                    if (!txtInTime.getText().isBlank()) a.setCheckInTime(LocalTime.parse(txtInTime.getText()));
                    if (!txtOutTime.getText().isBlank()) a.setCheckOutTime(LocalTime.parse(txtOutTime.getText()));
                    a.setNotes(txtNotes.getText());

                    attendanceService.recordAttendance(a);
                    DialogUtils.showSuccess("Attendance Recorded", "Attendance logged successfully.");
                    loadAttendance();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Save Failed", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    public void showEditAttendanceDialog(Attendance att) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Attendance - " + att.getEmployeeName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        ComboBox<Attendance.AttendanceStatus> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(Attendance.AttendanceStatus.values()));
        cmbStatus.setValue(att.getStatus());

        TextField txtInTime = new TextField(att.getCheckInTime() != null ? att.getCheckInTime().toString() : "");
        TextField txtOutTime = new TextField(att.getCheckOutTime() != null ? att.getCheckOutTime().toString() : "");
        TextField txtNotes = new TextField(att.getNotes() != null ? att.getNotes() : "");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Employee:"), new Label(att.getEmployeeName() + " (" + att.getEmployeeCode() + ")"));
        grid.addRow(1, new Label("Date:"), new Label(DateUtils.formatDate(att.getAttendanceDate())));
        grid.addRow(2, new Label("Status:*"), cmbStatus);
        grid.addRow(3, new Label("Check-In Time:"), txtInTime);
        grid.addRow(4, new Label("Check-Out Time:"), txtOutTime);
        grid.addRow(5, new Label("Notes:"), txtNotes);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    att.setStatus(cmbStatus.getValue());
                    if (!txtInTime.getText().isBlank()) att.setCheckInTime(LocalTime.parse(txtInTime.getText()));
                    else att.setCheckInTime(null);
                    if (!txtOutTime.getText().isBlank()) att.setCheckOutTime(LocalTime.parse(txtOutTime.getText()));
                    else att.setCheckOutTime(null);
                    att.setNotes(txtNotes.getText());

                    attendanceService.updateAttendance(att);
                    DialogUtils.showSuccess("Updated", "Attendance updated successfully.");
                    loadAttendance();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Update Failed", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void handleDeleteAttendance(Attendance att) {
        if (DialogUtils.showConfirmation("Delete Attendance", "Delete attendance for " + att.getEmployeeName() + " on " + att.getAttendanceDate() + "?", "This action will remove the record.")) {
            attendanceService.deleteAttendance(att.getId());
            loadAttendance();
        }
    }
}
