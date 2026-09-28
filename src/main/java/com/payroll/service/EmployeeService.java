package com.payroll.service;

import com.payroll.config.DatabaseConfig;
import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.dao.EmployeeDao;
import com.payroll.dao.EmployeeDaoImpl;
import com.payroll.dao.SalaryStructureDao;
import com.payroll.dao.SalaryStructureDaoImpl;
import com.payroll.exception.DatabaseException;
import com.payroll.exception.DuplicateRecordException;
import com.payroll.exception.ValidationException;
import com.payroll.model.AuditLog;
import com.payroll.model.Employee;
import com.payroll.model.EmployeeSalaryStructure;
import com.payroll.model.User;
import com.payroll.util.UserSession;
import com.payroll.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EmployeeService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeDao employeeDao;
    private final SalaryStructureDao salaryStructureDao;
    private final AuditDao auditDao;

    public EmployeeService() {
        this.employeeDao = new EmployeeDaoImpl();
        this.salaryStructureDao = new SalaryStructureDaoImpl();
        this.auditDao = new AuditDaoImpl();
    }

    public EmployeeService(EmployeeDao employeeDao, SalaryStructureDao salaryStructureDao, AuditDao auditDao) {
        this.employeeDao = employeeDao;
        this.salaryStructureDao = salaryStructureDao;
        this.auditDao = auditDao;
    }

    public List<Employee> getAllEmployees() {
        return employeeDao.findAll();
    }

    public List<Employee> getActiveEmployees() {
        return employeeDao.findActiveEmployees();
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeDao.findById(id);
    }

    public Optional<Employee> getEmployeeByCode(String code) {
        return employeeDao.findByCode(code);
    }

    public List<Employee> searchAndFilter(String query, Long deptId, Long desigId, Employee.EmploymentStatus status) {
        return employeeDao.searchAndFilter(query, deptId, desigId, status);
    }

    public Employee createEmployee(Employee employee, EmployeeSalaryStructure salaryStructure) {
        UserSession.requireHrOrAdmin();
        validateEmployeeInput(employee);

        if (employeeDao.findByCode(employee.getEmployeeCode().trim()).isPresent()) {
            throw new DuplicateRecordException("Employee Code '" + employee.getEmployeeCode() + "' is already in use.");
        }
        if (employeeDao.findByEmail(employee.getEmail().trim()).isPresent()) {
            throw new DuplicateRecordException("Email address '" + employee.getEmail() + "' is already assigned to another employee.");
        }

        if (salaryStructure == null || salaryStructure.getBasicSalary() == null || salaryStructure.getBasicSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Basic salary must be specified and greater than zero.");
        }

        // Transactional creation of employee + salary structure
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Employee savedEmp = employeeDao.saveWithConnection(employee, conn);
                salaryStructure.setEmployeeId(savedEmp.getId());
                salaryStructureDao.saveOrUpdateWithConnection(salaryStructure, conn);

                conn.commit();
                logAudit("CREATE_EMPLOYEE", savedEmp.getId(), "Registered new employee: " + savedEmp.getFullName() + " (" + savedEmp.getEmployeeCode() + ")");
                logger.info("Successfully created employee {} with ID {}", savedEmp.getEmployeeCode(), savedEmp.getId());
                return savedEmp;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction rolled back during employee creation for {}", employee.getEmployeeCode(), e);
                throw new DatabaseException("Failed to register employee: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure during employee registration", e);
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    public boolean updateEmployee(Employee employee) {
        UserSession.requireHrOrAdmin();
        validateEmployeeInput(employee);

        Optional<Employee> existingByEmail = employeeDao.findByEmail(employee.getEmail().trim());
        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(employee.getId())) {
            throw new DuplicateRecordException("Email address '" + employee.getEmail() + "' is already in use.");
        }

        boolean ok = employeeDao.update(employee);
        if (ok) {
            logAudit("UPDATE_EMPLOYEE", employee.getId(), "Updated employee details for: " + employee.getFullName());
        }
        return ok;
    }

    public boolean setEmployeeStatus(Long employeeId, Employee.EmploymentStatus status) {
        UserSession.requireHrOrAdmin();
        boolean ok = employeeDao.updateStatus(employeeId, status);
        if (ok) {
            logAudit("UPDATE_EMPLOYEE_STATUS", employeeId, "Changed employment status to: " + status);
        }
        return ok;
    }

    public Optional<EmployeeSalaryStructure> getSalaryStructure(Long employeeId) {
        return salaryStructureDao.findByEmployeeId(employeeId);
    }

    public boolean updateSalaryStructure(EmployeeSalaryStructure structure) {
        UserSession.requireHrOrAdmin();
        if (structure.getBasicSalary() == null || structure.getBasicSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Basic salary must be greater than zero.");
        }
        ValidationUtils.validatePositiveOrZero(structure.getHra(), "HRA");
        ValidationUtils.validatePositiveOrZero(structure.getSpecialAllowance(), "Special Allowance");
        ValidationUtils.validatePositiveOrZero(structure.getConveyanceAllowance(), "Conveyance Allowance");
        ValidationUtils.validatePositiveOrZero(structure.getMedicalAllowance(), "Medical Allowance");
        ValidationUtils.validatePositiveOrZero(structure.getProfessionalTax(), "Professional Tax");
        ValidationUtils.validatePositiveOrZero(structure.getTdsMonthly(), "TDS Monthly");

        boolean ok = salaryStructureDao.saveOrUpdate(structure);
        if (ok) {
            logAudit("UPDATE_SALARY_STRUCTURE", structure.getEmployeeId(), "Updated salary structure. Basic: " + structure.getBasicSalary());
        }
        return ok;
    }

    public int countTotal() {
        return employeeDao.countTotal();
    }

    public int countActive() {
        return employeeDao.countActive();
    }

    public int countInactive() {
        return employeeDao.countInactive();
    }

    private void validateEmployeeInput(Employee emp) {
        ValidationUtils.requireNonBlank(emp.getEmployeeCode(), "Employee Code");
        ValidationUtils.requireNonBlank(emp.getFirstName(), "First Name");
        ValidationUtils.requireNonBlank(emp.getLastName(), "Last Name");
        ValidationUtils.validateEmail(emp.getEmail());
        ValidationUtils.validatePhone(emp.getPhone());
        if (emp.getDepartmentId() == null || emp.getDepartmentId() <= 0) {
            throw new ValidationException("Please assign a valid Department.");
        }
        if (emp.getDesignationId() == null || emp.getDesignationId() <= 0) {
            throw new ValidationException("Please assign a valid Designation.");
        }
        if (emp.getJoiningDate() == null) {
            throw new ValidationException("Date of Joining is required.");
        }
    }

    private void logAudit(String action, Long entityId, String details) {
        User current = UserSession.getCurrentUser();
        Long uid = current != null ? current.getId() : null;
        String uname = current != null ? current.getUsername() : "SYSTEM";
        auditDao.log(new AuditLog(uid, uname, action, "EMPLOYEE", entityId, details, "127.0.0.1"));
    }
}
