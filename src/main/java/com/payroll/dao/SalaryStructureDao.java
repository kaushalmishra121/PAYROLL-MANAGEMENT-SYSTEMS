package com.payroll.dao;

import com.payroll.model.EmployeeSalaryStructure;
import com.payroll.model.SalaryComponent;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface SalaryStructureDao {
    Optional<EmployeeSalaryStructure> findByEmployeeId(Long employeeId);
    boolean saveOrUpdate(EmployeeSalaryStructure structure);
    boolean saveOrUpdateWithConnection(EmployeeSalaryStructure structure, Connection conn);
    boolean deleteByEmployeeId(Long employeeId);

    List<SalaryComponent> findAllComponents();
    SalaryComponent saveComponent(SalaryComponent component);
    boolean updateComponent(SalaryComponent component);
}
