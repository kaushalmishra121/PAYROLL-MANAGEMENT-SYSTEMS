package com.payroll.controller;

import com.payroll.model.Department;
import com.payroll.model.Designation;
import com.payroll.model.Employee;
import com.payroll.model.EmployeeSalaryStructure;
import com.payroll.service.DepartmentService;
import com.payroll.service.EmployeeService;
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
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EmployeeManagementController {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeManagementController.class);

    @FXML private TextField txtSearch;
    @FXML private ComboBox<Department> cmbDepartmentFilter;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Label lblTableCount;

    @FXML private TableView<Employee> tblEmployees;
    @FXML private TableColumn<Employee, String> colEmpCode;
    @FXML private TableColumn<Employee, String> colEmpName;
    @FXML private TableColumn<Employee, String> colEmpEmail;
    @FXML private TableColumn<Employee, String> colEmpDept;
    @FXML private TableColumn<Employee, String> colEmpDesig;
    @FXML private TableColumn<Employee, String> colEmpJoinDate;
    @FXML private TableColumn<Employee, String> colEmpGross;
    @FXML private TableColumn<Employee, String> colEmpStatus;
    @FXML private TableColumn<Employee, Void> colEmpActions;

    private final EmployeeService employeeService = new EmployeeService();
    private final DepartmentService departmentService = new DepartmentService();
    private final ReportService reportService = new ReportService();

    private final ObservableList<Employee> employeeList = FXCollections.observableArrayList();
    private final ObservableList<Department> departmentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadDropdownData();
        loadEmployees();

        // Live search on typing
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        cmbDepartmentFilter.setOnAction(e -> handleSearch());
        cmbStatusFilter.setOnAction(e -> handleSearch());
    }

    private void setupTableColumns() {
        colEmpCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeCode()));
        colEmpName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colEmpEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEmpDept.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartmentName()));
        colEmpDesig.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDesignationTitle()));
        colEmpJoinDate.setCellValueFactory(c -> new SimpleStringProperty(DateUtils.formatDate(c.getValue().getJoiningDate())));
        colEmpGross.setCellValueFactory(c -> new SimpleStringProperty(CurrencyUtils.format(c.getValue().getGrossSalary())));

        // Status Badge
        colEmpStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmploymentStatus().name()));
        colEmpStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("badge");
                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-active");
                    } else if ("PROBATION".equalsIgnoreCase(status)) {
                        badge.getStyleClass().add("badge-probation");
                    } else {
                        badge.getStyleClass().add("badge-inactive");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Actions Column
        colEmpActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("View");
            private final Button btnEdit = new Button("Edit");
            private final Button btnStatus = new Button("Toggle");
            private final HBox container = new HBox(6, btnView, btnEdit, btnStatus);

            {
                btnView.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnEdit.getStyleClass().addAll("btn-primary", "btn-sm");
                btnStatus.getStyleClass().addAll("btn-secondary", "btn-sm");
                container.setAlignment(Pos.CENTER);

                btnView.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    showEmployeeDetailsDialog(emp);
                });
                btnEdit.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    showEditEmployeeDialog(emp);
                });
                btnStatus.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    toggleEmployeeStatus(emp);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });

        tblEmployees.setItems(employeeList);
    }

    private void loadDropdownData() {
        Task<List<Department>> deptTask = new Task<>() {
            @Override
            protected List<Department> call() {
                return departmentService.getActiveDepartments();
            }
        };
        deptTask.setOnSucceeded(e -> {
            departmentList.setAll(deptTask.getValue());
            Department allDept = new Department(0L, "All Departments", "ALL", "", Department.Status.ACTIVE);
            departmentList.add(0, allDept);
            cmbDepartmentFilter.setItems(departmentList);
            cmbDepartmentFilter.getSelectionModel().selectFirst();
        });
        new Thread(deptTask).start();

        ObservableList<String> statusList = FXCollections.observableArrayList(
                "All Statuses", "ACTIVE", "PROBATION", "SUSPENDED", "RESIGNED", "TERMINATED"
        );
        cmbStatusFilter.setItems(statusList);
        cmbStatusFilter.getSelectionModel().selectFirst();
    }

    public void loadEmployees() {
        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() {
                return employeeService.getAllEmployees();
            }
        };
        task.setOnSucceeded(e -> {
            employeeList.setAll(task.getValue());
            lblTableCount.setText("Showing " + employeeList.size() + " Employees");
        });
        new Thread(task).start();
    }

    @FXML
    public void handleSearch() {
        String query = txtSearch.getText();
        Department selectedDept = cmbDepartmentFilter.getValue();
        Long deptId = (selectedDept != null && selectedDept.getId() > 0) ? selectedDept.getId() : null;

        String selectedStatusStr = cmbStatusFilter.getValue();
        Employee.EmploymentStatus status = null;
        if (selectedStatusStr != null && !selectedStatusStr.startsWith("All")) {
            try {
                status = Employee.EmploymentStatus.valueOf(selectedStatusStr);
            } catch (Exception ignored) {}
        }

        Employee.EmploymentStatus finalStatus = status;
        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() {
                return employeeService.searchAndFilter(query, deptId, null, finalStatus);
            }
        };
        task.setOnSucceeded(e -> {
            employeeList.setAll(task.getValue());
            lblTableCount.setText("Showing " + employeeList.size() + " Employees");
        });
        new Thread(task).start();
    }

    @FXML
    public void handleResetFilter() {
        txtSearch.clear();
        cmbDepartmentFilter.getSelectionModel().selectFirst();
        cmbStatusFilter.getSelectionModel().selectFirst();
        loadEmployees();
    }

    @FXML
    public void handleExportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Employee Register to CSV");
        fileChooser.setInitialFileName("Employees_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(tblEmployees.getScene().getWindow());

        if (file != null) {
            try {
                reportService.exportEmployeesCsv(file, txtSearch.getText(), null, null, null);
                DialogUtils.showSuccess("Export Successful", "Employee register successfully exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtils.showError("Export Failed", "Error exporting to CSV: " + e.getMessage());
            }
        }
    }

    @FXML
    public void showAddEmployeeDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Register New Employee");
        dialog.setHeaderText("Enter employee details, organizational placement, and salary structure.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Save Employee", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        // Form Fields
        TextField txtCode = new TextField("EMP" + (1000 + employeeList.size() + 1));
        TextField txtFirst = new TextField();
        TextField txtLast = new TextField();
        TextField txtEmail = new TextField();
        TextField txtPhone = new TextField();
        TextField txtAddress = new TextField();
        DatePicker dpJoinDate = new DatePicker(LocalDate.now());

        ComboBox<Department> cmbDept = new ComboBox<>();
        ComboBox<Designation> cmbDesig = new ComboBox<>();
        cmbDept.setItems(FXCollections.observableArrayList(departmentService.getActiveDepartments()));
        cmbDept.setOnAction(e -> {
            Department sel = cmbDept.getValue();
            if (sel != null) {
                cmbDesig.setItems(FXCollections.observableArrayList(departmentService.getDesignationsByDepartment(sel.getId())));
            }
        });

        ComboBox<Employee.EmploymentStatus> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(Employee.EmploymentStatus.values()));
        cmbStatus.getSelectionModel().select(Employee.EmploymentStatus.ACTIVE);

        // Bank fields
        TextField txtBank = new TextField("Chase Bank");
        TextField txtAccount = new TextField();
        TextField txtRouting = new TextField();
        TextField txtTaxId = new TextField();

        // Salary structure fields
        TextField txtBasic = new TextField("60000.00");
        TextField txtHra = new TextField("24000.00");
        TextField txtSpecial = new TextField("12000.00");
        TextField txtConv = new TextField("1600.00");
        TextField txtMed = new TextField("1250.00");
        TextField txtPfRate = new TextField("12.00");
        TextField txtProfTax = new TextField("200.00");
        TextField txtTds = new TextField("5000.00");

        TabPane tabPane = new TabPane();
        tabPane.setPrefWidth(600);

        // Tab 1: Profile & Organization
        GridPane grid1 = createGrid();
        grid1.addRow(0, new Label("Employee ID:*"), txtCode, new Label("Join Date:*"), dpJoinDate);
        grid1.addRow(1, new Label("First Name:*"), txtFirst, new Label("Last Name:*"), txtLast);
        grid1.addRow(2, new Label("Corporate Email:*"), txtEmail, new Label("Phone:*"), txtPhone);
        grid1.addRow(3, new Label("Department:*"), cmbDept, new Label("Designation:*"), cmbDesig);
        grid1.addRow(4, new Label("Status:*"), cmbStatus, new Label("Address:"), txtAddress);
        Tab tab1 = new Tab("1. Profile & Organization", grid1);
        tab1.setClosable(false);

        // Tab 2: Bank & Tax Details
        GridPane grid2 = createGrid();
        grid2.addRow(0, new Label("Bank Name:"), txtBank, new Label("Account Number:"), txtAccount);
        grid2.addRow(1, new Label("IFSC/Routing:"), txtRouting, new Label("PAN / Tax ID:"), txtTaxId);
        Tab tab2 = new Tab("2. Banking & Tax", grid2);
        tab2.setClosable(false);

        // Tab 3: Salary Structure
        GridPane grid3 = createGrid();
        grid3.addRow(0, new Label("Basic Salary ($):*"), txtBasic, new Label("HRA ($):"), txtHra);
        grid3.addRow(1, new Label("Special Allowance ($):"), txtSpecial, new Label("Conveyance ($):"), txtConv);
        grid3.addRow(2, new Label("Medical Allowance ($):"), txtMed, new Label("PF Rate (%):"), txtPfRate);
        grid3.addRow(3, new Label("Professional Tax ($):"), txtProfTax, new Label("Monthly TDS ($):"), txtTds);
        Tab tab3 = new Tab("3. Salary Structure", grid3);
        tab3.setClosable(false);

        tabPane.getTabs().addAll(tab1, tab2, tab3);
        dialog.getDialogPane().setContent(tabPane);

        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    Employee emp = new Employee();
                    emp.setEmployeeCode(txtCode.getText());
                    emp.setFirstName(txtFirst.getText());
                    emp.setLastName(txtLast.getText());
                    emp.setEmail(txtEmail.getText());
                    emp.setPhone(txtPhone.getText());
                    emp.setAddress(txtAddress.getText());
                    if (cmbDept.getValue() != null) emp.setDepartmentId(cmbDept.getValue().getId());
                    if (cmbDesig.getValue() != null) emp.setDesignationId(cmbDesig.getValue().getId());
                    emp.setJoiningDate(dpJoinDate.getValue());
                    emp.setEmploymentStatus(cmbStatus.getValue());
                    emp.setBankName(txtBank.getText());
                    emp.setAccountNumber(txtAccount.getText());
                    emp.setIfscOrRouting(txtRouting.getText());
                    emp.setPanOrTaxId(txtTaxId.getText());

                    EmployeeSalaryStructure struct = new EmployeeSalaryStructure();
                    struct.setBasicSalary(CurrencyUtils.parse(txtBasic.getText()));
                    struct.setHra(CurrencyUtils.parse(txtHra.getText()));
                    struct.setSpecialAllowance(CurrencyUtils.parse(txtSpecial.getText()));
                    struct.setConveyanceAllowance(CurrencyUtils.parse(txtConv.getText()));
                    struct.setMedicalAllowance(CurrencyUtils.parse(txtMed.getText()));
                    struct.setPfRatePct(CurrencyUtils.parse(txtPfRate.getText()));
                    struct.setProfessionalTax(CurrencyUtils.parse(txtProfTax.getText()));
                    struct.setTdsMonthly(CurrencyUtils.parse(txtTds.getText()));

                    employeeService.createEmployee(emp, struct);
                    DialogUtils.showSuccess("Employee Registered", "Employee " + emp.getFullName() + " registered successfully.");
                    loadEmployees();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Registration Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    public void showEditEmployeeDialog(Employee employee) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Employee: " + employee.getFullName());
        dialog.setHeaderText("Update profile, organizational alignment, or salary structure.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        TextField txtFirst = new TextField(employee.getFirstName());
        TextField txtLast = new TextField(employee.getLastName());
        TextField txtEmail = new TextField(employee.getEmail());
        TextField txtPhone = new TextField(employee.getPhone());
        TextField txtAddress = new TextField(employee.getAddress());
        DatePicker dpJoinDate = new DatePicker(employee.getJoiningDate());

        ComboBox<Department> cmbDept = new ComboBox<>();
        ComboBox<Designation> cmbDesig = new ComboBox<>();
        cmbDept.setItems(FXCollections.observableArrayList(departmentService.getActiveDepartments()));
        for (Department d : cmbDept.getItems()) {
            if (d.getId().equals(employee.getDepartmentId())) {
                cmbDept.setValue(d);
                break;
            }
        }
        cmbDesig.setItems(FXCollections.observableArrayList(departmentService.getDesignationsByDepartment(employee.getDepartmentId())));
        for (Designation ds : cmbDesig.getItems()) {
            if (ds.getId().equals(employee.getDesignationId())) {
                cmbDesig.setValue(ds);
                break;
            }
        }
        cmbDept.setOnAction(e -> {
            if (cmbDept.getValue() != null) {
                cmbDesig.setItems(FXCollections.observableArrayList(departmentService.getDesignationsByDepartment(cmbDept.getValue().getId())));
            }
        });

        ComboBox<Employee.EmploymentStatus> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(Employee.EmploymentStatus.values()));
        cmbStatus.setValue(employee.getEmploymentStatus());

        TextField txtBank = new TextField(employee.getBankName());
        TextField txtAccount = new TextField(employee.getAccountNumber());
        TextField txtRouting = new TextField(employee.getIfscOrRouting());
        TextField txtTaxId = new TextField(employee.getPanOrTaxId());

        // Load existing salary structure
        EmployeeSalaryStructure struct = employeeService.getSalaryStructure(employee.getId())
                .orElse(new EmployeeSalaryStructure(employee.getId(), employee.getBasicSalary(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("12.00"), new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now()));

        TextField txtBasic = new TextField(struct.getBasicSalary().toString());
        TextField txtHra = new TextField(struct.getHra().toString());
        TextField txtSpecial = new TextField(struct.getSpecialAllowance().toString());
        TextField txtConv = new TextField(struct.getConveyanceAllowance().toString());
        TextField txtMed = new TextField(struct.getMedicalAllowance().toString());
        TextField txtPfRate = new TextField(struct.getPfRatePct().toString());
        TextField txtProfTax = new TextField(struct.getProfessionalTax().toString());
        TextField txtTds = new TextField(struct.getTdsMonthly().toString());

        TabPane tabPane = new TabPane();
        tabPane.setPrefWidth(600);

        GridPane grid1 = createGrid();
        grid1.addRow(0, new Label("First Name:*"), txtFirst, new Label("Last Name:*"), txtLast);
        grid1.addRow(1, new Label("Corporate Email:*"), txtEmail, new Label("Phone:*"), txtPhone);
        grid1.addRow(2, new Label("Department:*"), cmbDept, new Label("Designation:*"), cmbDesig);
        grid1.addRow(3, new Label("Status:*"), cmbStatus, new Label("Address:"), txtAddress);
        Tab tab1 = new Tab("Profile & Org", grid1);
        tab1.setClosable(false);

        GridPane grid2 = createGrid();
        grid2.addRow(0, new Label("Bank Name:"), txtBank, new Label("Account Number:"), txtAccount);
        grid2.addRow(1, new Label("IFSC/Routing:"), txtRouting, new Label("PAN / Tax ID:"), txtTaxId);
        Tab tab2 = new Tab("Banking", grid2);
        tab2.setClosable(false);

        GridPane grid3 = createGrid();
        grid3.addRow(0, new Label("Basic Salary ($):*"), txtBasic, new Label("HRA ($):"), txtHra);
        grid3.addRow(1, new Label("Special Allowance ($):"), txtSpecial, new Label("Conveyance ($):"), txtConv);
        grid3.addRow(2, new Label("Medical Allowance ($):"), txtMed, new Label("PF Rate (%):"), txtPfRate);
        grid3.addRow(3, new Label("Professional Tax ($):"), txtProfTax, new Label("Monthly TDS ($):"), txtTds);
        Tab tab3 = new Tab("Salary Structure", grid3);
        tab3.setClosable(false);

        tabPane.getTabs().addAll(tab1, tab2, tab3);
        dialog.getDialogPane().setContent(tabPane);

        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    employee.setFirstName(txtFirst.getText());
                    employee.setLastName(txtLast.getText());
                    employee.setEmail(txtEmail.getText());
                    employee.setPhone(txtPhone.getText());
                    employee.setAddress(txtAddress.getText());
                    if (cmbDept.getValue() != null) employee.setDepartmentId(cmbDept.getValue().getId());
                    if (cmbDesig.getValue() != null) employee.setDesignationId(cmbDesig.getValue().getId());
                    employee.setEmploymentStatus(cmbStatus.getValue());
                    employee.setBankName(txtBank.getText());
                    employee.setAccountNumber(txtAccount.getText());
                    employee.setIfscOrRouting(txtRouting.getText());
                    employee.setPanOrTaxId(txtTaxId.getText());

                    struct.setBasicSalary(CurrencyUtils.parse(txtBasic.getText()));
                    struct.setHra(CurrencyUtils.parse(txtHra.getText()));
                    struct.setSpecialAllowance(CurrencyUtils.parse(txtSpecial.getText()));
                    struct.setConveyanceAllowance(CurrencyUtils.parse(txtConv.getText()));
                    struct.setMedicalAllowance(CurrencyUtils.parse(txtMed.getText()));
                    struct.setPfRatePct(CurrencyUtils.parse(txtPfRate.getText()));
                    struct.setProfessionalTax(CurrencyUtils.parse(txtProfTax.getText()));
                    struct.setTdsMonthly(CurrencyUtils.parse(txtTds.getText()));

                    employeeService.updateEmployee(employee);
                    employeeService.updateSalaryStructure(struct);

                    DialogUtils.showSuccess("Employee Updated", "Profile and salary structure updated successfully.");
                    loadEmployees();
                    return true;
                } catch (Exception ex) {
                    DialogUtils.showError("Update Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    public void showEmployeeDetailsDialog(Employee employee) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Employee Profile - " + employee.getFullName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        EmployeeSalaryStructure struct = employeeService.getSalaryStructure(employee.getId())
                .orElse(new EmployeeSalaryStructure());

        VBox content = new VBox(16);
        content.setPrefWidth(520);
        content.setPadding(new Insets(10));

        // Header info card
        HBox header = new HBox(16);
        header.getStyleClass().add("card");
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(50, 50);
        avatar.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 25;");
        Label avText = new Label(employee.getFirstName().substring(0, 1));
        avText.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold;");
        avatar.getChildren().add(avText);

        VBox nameBox = new VBox(4);
        Label name = new Label(employee.getFullName() + " (" + employee.getEmployeeCode() + ")");
        name.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        Label role = new Label(employee.getDesignationTitle() + " • " + employee.getDepartmentName());
        role.setStyle("-fx-text-fill: #64748b;");
        nameBox.getChildren().addAll(name, role);

        header.getChildren().addAll(avatar, nameBox);

        // Details grid
        GridPane details = createGrid();
        details.getStyleClass().add("card");
        details.addRow(0, new Label("Email:"), new Label(employee.getEmail()), new Label("Phone:"), new Label(employee.getPhone()));
        details.addRow(1, new Label("Join Date:"), new Label(DateUtils.formatDate(employee.getJoiningDate())), new Label("Status:"), new Label(employee.getEmploymentStatus().name()));
        details.addRow(2, new Label("Bank:"), new Label(employee.getBankName() != null ? employee.getBankName() : "-"), new Label("Account:"), new Label(employee.getMaskedAccountNumber()));
        details.addRow(3, new Label("Tax ID:"), new Label(employee.getPanOrTaxId() != null ? employee.getPanOrTaxId() : "-"), new Label("Address:"), new Label(employee.getAddress() != null ? employee.getAddress() : "-"));

        // Salary Breakdown Card
        VBox salBox = new VBox(8);
        salBox.getStyleClass().add("card");
        Label salTitle = new Label("Configured Monthly Compensation");
        salTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        GridPane salGrid = createGrid();
        salGrid.addRow(0, new Label("Basic Salary:"), new Label(CurrencyUtils.format(struct.getBasicSalary())), new Label("HRA:"), new Label(CurrencyUtils.format(struct.getHra())));
        salGrid.addRow(1, new Label("Special Allow:"), new Label(CurrencyUtils.format(struct.getSpecialAllowance())), new Label("Conveyance:"), new Label(CurrencyUtils.format(struct.getConveyanceAllowance())));
        salGrid.addRow(2, new Label("Medical Allow:"), new Label(CurrencyUtils.format(struct.getMedicalAllowance())), new Label("Gross Pay:"), new Label(CurrencyUtils.format(struct.calculateGrossSalary())));
        salGrid.addRow(3, new Label("PF Deduction:"), new Label(CurrencyUtils.format(struct.calculatePfAmount())), new Label("Estimated Net:"), new Label(CurrencyUtils.format(struct.calculateNetSalary())));

        salBox.getChildren().addAll(salTitle, salGrid);
        content.getChildren().addAll(header, details, salBox);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void toggleEmployeeStatus(Employee employee) {
        Employee.EmploymentStatus nextStatus = (employee.getEmploymentStatus() == Employee.EmploymentStatus.ACTIVE) 
                ? Employee.EmploymentStatus.SUSPENDED : Employee.EmploymentStatus.ACTIVE;

        if (DialogUtils.showConfirmation("Update Status", "Change status for " + employee.getFullName() + "?", 
                "Current: " + employee.getEmploymentStatus() + "\nNew Status: " + nextStatus)) {
            employeeService.setEmployeeStatus(employee.getId(), nextStatus);
            loadEmployees();
        }
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        return grid;
    }
}
