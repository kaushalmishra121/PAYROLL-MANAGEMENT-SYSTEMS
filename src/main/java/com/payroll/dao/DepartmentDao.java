package com.payroll.dao;

import com.payroll.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentDao {
    Optional<Department> findById(Long id);
    Optional<Department> findByCode(String code);
    Optional<Department> findByName(String name);
    List<Department> findAll();
    List<Department> findActive();
    Department save(Department department);
    boolean update(Department department);
    boolean delete(Long id);
    int countEmployeesInDepartment(Long departmentId);
    int countTotalDepartments();
}
