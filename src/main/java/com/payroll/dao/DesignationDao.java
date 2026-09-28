package com.payroll.dao;

import com.payroll.model.Designation;

import java.util.List;
import java.util.Optional;

public interface DesignationDao {
    Optional<Designation> findById(Long id);
    List<Designation> findByDepartmentId(Long departmentId);
    List<Designation> findAll();
    List<Designation> findActive();
    Designation save(Designation designation);
    boolean update(Designation designation);
    boolean delete(Long id);
    int countEmployeesInDesignation(Long designationId);
}
