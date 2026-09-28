package com.payroll.controller;

import com.payroll.model.Department;
import com.payroll.model.Designation;
import com.payroll.service.DepartmentService;
import com.payroll.util.CurrencyUtils;
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

import java.math.BigDecimal;
import java.util.List;

public class DepartmentManagementController {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentManagementController.class);

    @FXML private TableView<Department> tblDepartments;
    @FXML private TableColumn<Department, String> colDeptCode;
    @FXML private TableColumn<Department, String> colDeptName;
    @FXML private TableColumn<Department, String> colDeptEmployees;
    @FXML private TableColumn<Department, String> colDeptStatus;
    @FXML private TableColumn<Department, Void> colDeptActions;

    @FXML private TableView<Designation> tblDesignations;
    @FXML private TableColumn<Designation, String> colDesigCode;
    @FXML private TableColumn<Designation, String> colDesigTitle;
    @FXML private TableColumn<Designation, String> colDesigDept;
    @FXML private TableColumn<Designation, String> colDesigPayBand;
    @FXML private TableColumn<Designation, Void> colDesigActions;

    private final DepartmentService departmentService = new DepartmentService();

    private final ObservableList<Department> departmentList = FXCollections.observableArrayList();
    private final ObservableList<Designation> designationList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDepartmentColumns();
        setupDesignationColumns();
        loadAllData();
    }

    private void setupDepartmentColumns() {
        colDeptCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        colDeptName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colDeptEmployees.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEmployeeCount())));

        colDeptStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colDeptStatus.setCellFactory(col -> new TableCell<>() {
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

        colDeptActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDel = new Button("Delete");
            private final HBox box = new HBox(6, btnEdit, btnDel);
            {
                btnEdit.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnDel.getStyleClass().addAll("btn-danger", "btn-sm");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> showEditDepartmentDialog(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> handleDeleteDepartment(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblDepartments.setItems(departmentList);
    }

    private void setupDesignationColumns() {
        colDesigCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        colDesigTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colDesigDept.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartmentName()));
        colDesigPayBand.setCellValueFactory(c -> new SimpleStringProperty(
                CurrencyUtils.formatPlain(c.getValue().getMinSalary()) + " - " + CurrencyUtils.formatPlain(c.getValue().getMaxSalary())
        ));

        colDesigActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDel = new Button("Delete");
            private final HBox box = new HBox(6, btnEdit, btnDel);
            {
                btnEdit.getStyleClass().addAll("btn-secondary", "btn-sm");
                btnDel.getStyleClass().addAll("btn-danger", "btn-sm");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> showEditDesignationDialog(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> handleDeleteDesignation(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblDesignations.setItems(designationList);
    }

    public void loadAllData() {
        Task<List<Department>> deptTask = new Task<>() {
            @Override
            protected List<Department> call() {
                return departmentService.getAllDepartments();
            }
        };
        deptTask.setOnSucceeded(e -> departmentList.setAll(deptTask.getValue()));
        new Thread(deptTask).start();

        Task<List<Designation>> desigTask = new Task<>() {
            @Override
            protected List<Designation> call() {
                return departmentService.getAllDesignations();
            }
        };
        desigTask.setOnSucceeded(e -> designationList.setAll(desigTask.getValue()));
        new Thread(desigTask).start();
    }

    @FXML
    public void showAddDepartmentDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Create Department");
        dialog.setHeaderText("Add a new organizational business unit.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        TextField txtName = new TextField();
        TextField txtCode = new TextField();
        TextField txtDesc = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Department Name:*"), txtName);
        grid.addRow(1, new Label("Code (e.g. ENG, FIN):*"), txtCode);
        grid.addRow(2, new Label("Description:"), txtDesc);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    Department d = new Department();
                    d.setName(txtName.getText());
                    d.setCode(txtCode.getText());
                    d.setDescription(txtDesc.getText());
                    d.setStatus(Department.Status.ACTIVE);
                    departmentService.createDepartment(d);
                    DialogUtils.showSuccess("Department Created", "Department '" + d.getName() + "' created successfully.");
                    loadAllData();
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

    public void showEditDepartmentDialog(Department dept) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Department: " + dept.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        TextField txtName = new TextField(dept.getName());
        TextField txtCode = new TextField(dept.getCode());
        TextField txtDesc = new TextField(dept.getDescription() != null ? dept.getDescription() : "");
        ComboBox<Department.Status> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(Department.Status.values()));
        cmbStatus.setValue(dept.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Department Name:*"), txtName);
        grid.addRow(1, new Label("Department Code:*"), txtCode);
        grid.addRow(2, new Label("Description:"), txtDesc);
        grid.addRow(3, new Label("Status:"), cmbStatus);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    dept.setName(txtName.getText());
                    dept.setCode(txtCode.getText());
                    dept.setDescription(txtDesc.getText());
                    dept.setStatus(cmbStatus.getValue());
                    departmentService.updateDepartment(dept);
                    DialogUtils.showSuccess("Updated", "Department updated successfully.");
                    loadAllData();
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

    private void handleDeleteDepartment(Department dept) {
        if (DialogUtils.showConfirmation("Delete Department", "Delete department '" + dept.getName() + "'?", "This action cannot be undone.")) {
            try {
                departmentService.deleteDepartment(dept.getId());
                DialogUtils.showSuccess("Deleted", "Department deleted successfully.");
                loadAllData();
            } catch (Exception ex) {
                DialogUtils.showError("Delete Failed", ex.getMessage());
            }
        }
    }

    @FXML
    public void showAddDesignationDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Create Designation");
        dialog.setHeaderText("Add a job position and define salary band boundaries.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        ComboBox<Department> cmbDept = new ComboBox<>(FXCollections.observableArrayList(departmentService.getActiveDepartments()));
        if (!cmbDept.getItems().isEmpty()) cmbDept.getSelectionModel().selectFirst();

        TextField txtTitle = new TextField();
        TextField txtCode = new TextField();
        TextField txtMin = new TextField("50000.00");
        TextField txtMax = new TextField("120000.00");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Department:*"), cmbDept);
        grid.addRow(1, new Label("Job Title:*"), txtTitle);
        grid.addRow(2, new Label("Code:*"), txtCode);
        grid.addRow(3, new Label("Minimum Salary ($):"), txtMin);
        grid.addRow(4, new Label("Maximum Salary ($):"), txtMax);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    Designation ds = new Designation();
                    if (cmbDept.getValue() != null) ds.setDepartmentId(cmbDept.getValue().getId());
                    ds.setTitle(txtTitle.getText());
                    ds.setCode(txtCode.getText());
                    ds.setMinSalary(CurrencyUtils.parse(txtMin.getText()));
                    ds.setMaxSalary(CurrencyUtils.parse(txtMax.getText()));
                    ds.setStatus(Designation.Status.ACTIVE);
                    departmentService.createDesignation(ds);
                    DialogUtils.showSuccess("Designation Created", "Designation '" + ds.getTitle() + "' created successfully.");
                    loadAllData();
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

    public void showEditDesignationDialog(Designation ds) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Designation: " + ds.getTitle());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ButtonType btnSaveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);

        ComboBox<Department> cmbDept = new ComboBox<>(FXCollections.observableArrayList(departmentService.getActiveDepartments()));
        for (Department d : cmbDept.getItems()) {
            if (d.getId().equals(ds.getDepartmentId())) {
                cmbDept.setValue(d);
                break;
            }
        }

        TextField txtTitle = new TextField(ds.getTitle());
        TextField txtCode = new TextField(ds.getCode());
        TextField txtMin = new TextField(ds.getMinSalary().toString());
        TextField txtMax = new TextField(ds.getMaxSalary().toString());
        ComboBox<Designation.Status> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(Designation.Status.values()));
        cmbStatus.setValue(ds.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Department:*"), cmbDept);
        grid.addRow(1, new Label("Job Title:*"), txtTitle);
        grid.addRow(2, new Label("Code:*"), txtCode);
        grid.addRow(3, new Label("Minimum Salary ($):"), txtMin);
        grid.addRow(4, new Label("Maximum Salary ($):"), txtMax);
        grid.addRow(5, new Label("Status:"), cmbStatus);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                try {
                    if (cmbDept.getValue() != null) ds.setDepartmentId(cmbDept.getValue().getId());
                    ds.setTitle(txtTitle.getText());
                    ds.setCode(txtCode.getText());
                    ds.setMinSalary(CurrencyUtils.parse(txtMin.getText()));
                    ds.setMaxSalary(CurrencyUtils.parse(txtMax.getText()));
                    ds.setStatus(cmbStatus.getValue());
                    departmentService.updateDesignation(ds);
                    DialogUtils.showSuccess("Updated", "Designation updated successfully.");
                    loadAllData();
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

    private void handleDeleteDesignation(Designation ds) {
        if (DialogUtils.showConfirmation("Delete Designation", "Delete designation '" + ds.getTitle() + "'?", "This action cannot be undone.")) {
            try {
                departmentService.deleteDesignation(ds.getId());
                DialogUtils.showSuccess("Deleted", "Designation deleted successfully.");
                loadAllData();
            } catch (Exception ex) {
                DialogUtils.showError("Delete Failed", ex.getMessage());
            }
        }
    }
}
