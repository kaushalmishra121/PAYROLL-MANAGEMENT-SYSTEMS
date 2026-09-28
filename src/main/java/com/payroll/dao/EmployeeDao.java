package com.payroll.dao;

import com.payroll.model.DepartmentHeadcount;
import com.payroll.model.Employee;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface EmployeeDao {
    Optional<Employee> findById(Long id);
    Optional<Employee> findByCode(String code);
    Optional<Employee> findByEmail(String email);
    List<Employee> findAll();
    List<Employee> findActiveEmployees();
    List<Employee> searchAndFilter(String query, Long departmentId, Long designationId, Employee.EmploymentStatus status);
    Employee save(Employee employee);
    Employee saveWithConnection(Employee employee, Connection conn);
    boolean update(Employee employee);
    boolean updateStatus(Long id, Employee.EmploymentStatus status);
    int countTotal();
    int countActive();
    int countInactive();
    List<DepartmentHeadcount> getDepartmentHeadcounts();
}
