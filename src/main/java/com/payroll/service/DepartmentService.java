package com.payroll.service;

import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.dao.DepartmentDao;
import com.payroll.dao.DepartmentDaoImpl;
import com.payroll.dao.DesignationDao;
import com.payroll.dao.DesignationDaoImpl;
import com.payroll.exception.DuplicateRecordException;
import com.payroll.exception.ValidationException;
import com.payroll.model.AuditLog;
import com.payroll.model.Department;
import com.payroll.model.Designation;
import com.payroll.model.User;
import com.payroll.util.UserSession;
import com.payroll.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class DepartmentService {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentDao departmentDao;
    private final DesignationDao designationDao;
    private final AuditDao auditDao;

    public DepartmentService() {
        this.departmentDao = new DepartmentDaoImpl();
        this.designationDao = new DesignationDaoImpl();
        this.auditDao = new AuditDaoImpl();
    }

    public DepartmentService(DepartmentDao departmentDao, DesignationDao designationDao, AuditDao auditDao) {
        this.departmentDao = departmentDao;
        this.designationDao = designationDao;
        this.auditDao = auditDao;
    }

    // Departments
    public List<Department> getAllDepartments() {
        return departmentDao.findAll();
    }

    public List<Department> getActiveDepartments() {
        return departmentDao.findActive();
    }

    public Optional<Department> getDepartmentById(Long id) {
        return departmentDao.findById(id);
    }

    public Department createDepartment(Department dept) {
        UserSession.requireHrOrAdmin();
        ValidationUtils.requireNonBlank(dept.getName(), "Department Name");
        ValidationUtils.requireNonBlank(dept.getCode(), "Department Code");

        String code = dept.getCode().trim().toUpperCase();
        dept.setCode(code);

        if (departmentDao.findByCode(code).isPresent()) {
            throw new DuplicateRecordException("Department code '" + code + "' already exists.");
        }
        if (departmentDao.findByName(dept.getName().trim()).isPresent()) {
            throw new DuplicateRecordException("Department name '" + dept.getName() + "' already exists.");
        }

        Department saved = departmentDao.save(dept);
        logAudit("CREATE_DEPARTMENT", saved.getId(), "Created department: " + saved.getName() + " (" + saved.getCode() + ")");
        return saved;
    }

    public boolean updateDepartment(Department dept) {
        UserSession.requireHrOrAdmin();
        ValidationUtils.requireNonBlank(dept.getName(), "Department Name");
        ValidationUtils.requireNonBlank(dept.getCode(), "Department Code");

        String code = dept.getCode().trim().toUpperCase();
        dept.setCode(code);

        Optional<Department> existingByCode = departmentDao.findByCode(code);
        if (existingByCode.isPresent() && !existingByCode.get().getId().equals(dept.getId())) {
            throw new DuplicateRecordException("Department code '" + code + "' is used by another department.");
        }

        boolean ok = departmentDao.update(dept);
        if (ok) {
            logAudit("UPDATE_DEPARTMENT", dept.getId(), "Updated department: " + dept.getName());
        }
        return ok;
    }

    public boolean deleteDepartment(Long id) {
        UserSession.requireAdmin();
        int empCount = departmentDao.countEmployeesInDepartment(id);
        if (empCount > 0) {
            throw new ValidationException("Cannot delete department because it is assigned to " + empCount + " employee(s). Reassign employees before deletion.");
        }
        boolean ok = departmentDao.delete(id);
        if (ok) {
            logAudit("DELETE_DEPARTMENT", id, "Deleted department id: " + id);
        }
        return ok;
    }

    // Designations
    public List<Designation> getAllDesignations() {
        return designationDao.findAll();
    }

    public List<Designation> getActiveDesignations() {
        return designationDao.findActive();
    }

    public List<Designation> getDesignationsByDepartment(Long departmentId) {
        return designationDao.findByDepartmentId(departmentId);
    }

    public Optional<Designation> getDesignationById(Long id) {
        return designationDao.findById(id);
    }

    public Designation createDesignation(Designation desig) {
        UserSession.requireHrOrAdmin();
        ValidationUtils.requireNonBlank(desig.getTitle(), "Designation Title");
        ValidationUtils.requireNonBlank(desig.getCode(), "Designation Code");
        if (desig.getDepartmentId() == null || desig.getDepartmentId() <= 0) {
            throw new ValidationException("Please select a valid parent Department.");
        }
        if (desig.getMinSalary() != null && desig.getMaxSalary() != null) {
            if (desig.getMaxSalary().compareTo(desig.getMinSalary()) < 0) {
                throw new ValidationException("Maximum salary band cannot be less than minimum salary band.");
            }
        }

        desig.setCode(desig.getCode().trim().toUpperCase());
        Designation saved = designationDao.save(desig);
        logAudit("CREATE_DESIGNATION", saved.getId(), "Created designation: " + saved.getTitle() + " (" + saved.getCode() + ")");
        return saved;
    }

    public boolean updateDesignation(Designation desig) {
        UserSession.requireHrOrAdmin();
        ValidationUtils.requireNonBlank(desig.getTitle(), "Designation Title");
        ValidationUtils.requireNonBlank(desig.getCode(), "Designation Code");

        if (desig.getMinSalary() != null && desig.getMaxSalary() != null) {
            if (desig.getMaxSalary().compareTo(desig.getMinSalary()) < 0) {
                throw new ValidationException("Maximum salary band cannot be less than minimum salary band.");
            }
        }

        desig.setCode(desig.getCode().trim().toUpperCase());
        boolean ok = designationDao.update(desig);
        if (ok) {
            logAudit("UPDATE_DESIGNATION", desig.getId(), "Updated designation: " + desig.getTitle());
        }
        return ok;
    }

    public boolean deleteDesignation(Long id) {
        UserSession.requireAdmin();
        int empCount = designationDao.countEmployeesInDesignation(id);
        if (empCount > 0) {
            throw new ValidationException("Cannot delete designation because it is assigned to " + empCount + " employee(s).");
        }
        boolean ok = designationDao.delete(id);
        if (ok) {
            logAudit("DELETE_DESIGNATION", id, "Deleted designation id: " + id);
        }
        return ok;
    }

    private void logAudit(String action, Long entityId, String details) {
        User current = UserSession.getCurrentUser();
        Long uid = current != null ? current.getId() : null;
        String uname = current != null ? current.getUsername() : "SYSTEM";
        auditDao.log(new AuditLog(uid, uname, action, "ORGANIZATION", entityId, details, "127.0.0.1"));
    }
}
