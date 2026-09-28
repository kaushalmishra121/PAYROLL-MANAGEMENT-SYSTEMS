package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.exception.DatabaseException;
import com.payroll.model.EmployeeSalaryStructure;
import com.payroll.model.SalaryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SalaryStructureDaoImpl implements SalaryStructureDao {
    private static final Logger logger = LoggerFactory.getLogger(SalaryStructureDaoImpl.class);

    @Override
    public Optional<EmployeeSalaryStructure> findByEmployeeId(Long employeeId) {
        String sql = "SELECT s.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name " +
                     "FROM employee_salary_structures s " +
                     "JOIN employees e ON s.employee_id = e.id " +
                     "WHERE s.employee_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStructure(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding salary structure for employee id: {}", employeeId, e);
            throw new DatabaseException("Failed to find employee salary structure", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean saveOrUpdate(EmployeeSalaryStructure structure) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return saveOrUpdateWithConnection(structure, conn);
        } catch (SQLException e) {
            logger.error("Error saving salary structure for employee id: {}", structure.getEmployeeId(), e);
            throw new DatabaseException("Failed to save salary structure", e);
        }
    }

    @Override
    public boolean saveOrUpdateWithConnection(EmployeeSalaryStructure structure, Connection conn) {
        String sql = "INSERT INTO employee_salary_structures " +
                     "(employee_id, basic_salary, hra, special_allowance, conveyance_allowance, medical_allowance, " +
                     " pf_rate_pct, professional_tax, tds_monthly, other_deductions, effective_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "basic_salary = VALUES(basic_salary), " +
                     "hra = VALUES(hra), " +
                     "special_allowance = VALUES(special_allowance), " +
                     "conveyance_allowance = VALUES(conveyance_allowance), " +
                     "medical_allowance = VALUES(medical_allowance), " +
                     "pf_rate_pct = VALUES(pf_rate_pct), " +
                     "professional_tax = VALUES(professional_tax), " +
                     "tds_monthly = VALUES(tds_monthly), " +
                     "other_deductions = VALUES(other_deductions), " +
                     "effective_date = VALUES(effective_date)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, structure.getEmployeeId());
            ps.setBigDecimal(2, structure.getBasicSalary());
            ps.setBigDecimal(3, structure.getHra());
            ps.setBigDecimal(4, structure.getSpecialAllowance());
            ps.setBigDecimal(5, structure.getConveyanceAllowance());
            ps.setBigDecimal(6, structure.getMedicalAllowance());
            ps.setBigDecimal(7, structure.getPfRatePct());
            ps.setBigDecimal(8, structure.getProfessionalTax());
            ps.setBigDecimal(9, structure.getTdsMonthly());
            ps.setBigDecimal(10, structure.getOtherDeductions());
            ps.setDate(11, Date.valueOf(structure.getEffectiveDate()));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error saving/updating salary structure in transaction", e);
            throw new DatabaseException("Failed to save salary structure: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByEmployeeId(Long employeeId) {
        String sql = "DELETE FROM employee_salary_structures WHERE employee_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting salary structure for employee id: {}", employeeId, e);
            throw new DatabaseException("Failed to delete salary structure", e);
        }
    }

    @Override
    public List<SalaryComponent> findAllComponents() {
        String sql = "SELECT * FROM salary_components ORDER BY type ASC, name ASC";
        List<SalaryComponent> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToComponent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all salary components", e);
            throw new DatabaseException("Failed to retrieve salary components", e);
        }
        return list;
    }

    @Override
    public SalaryComponent saveComponent(SalaryComponent component) {
        String sql = "INSERT INTO salary_components (name, code, type, calculation_type, default_rate, is_taxable, is_mandatory, description, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, component.getName());
            ps.setString(2, component.getCode());
            ps.setString(3, component.getType().name());
            ps.setString(4, component.getCalculationType().name());
            ps.setBigDecimal(5, component.getDefaultRate());
            ps.setBoolean(6, component.isTaxable());
            ps.setBoolean(7, component.isMandatory());
            ps.setString(8, component.getDescription());
            ps.setString(9, component.isActive() ? "ACTIVE" : "INACTIVE");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        component.setId(rs.getLong(1));
                    }
                }
            }
            return component;
        } catch (SQLException e) {
            logger.error("Error saving salary component: {}", component.getName(), e);
            throw new DatabaseException("Failed to save salary component: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateComponent(SalaryComponent component) {
        String sql = "UPDATE salary_components SET name = ?, calculation_type = ?, default_rate = ?, is_taxable = ?, is_mandatory = ?, description = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, component.getName());
            ps.setString(2, component.getCalculationType().name());
            ps.setBigDecimal(3, component.getDefaultRate());
            ps.setBoolean(4, component.isTaxable());
            ps.setBoolean(5, component.isMandatory());
            ps.setString(6, component.getDescription());
            ps.setString(7, component.isActive() ? "ACTIVE" : "INACTIVE");
            ps.setLong(8, component.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating salary component id: {}", component.getId(), e);
            throw new DatabaseException("Failed to update salary component", e);
        }
    }

    private EmployeeSalaryStructure mapResultSetToStructure(ResultSet rs) throws SQLException {
        EmployeeSalaryStructure s = new EmployeeSalaryStructure();
        s.setId(rs.getLong("id"));
        s.setEmployeeId(rs.getLong("employee_id"));
        s.setBasicSalary(rs.getBigDecimal("basic_salary"));
        s.setHra(rs.getBigDecimal("hra"));
        s.setSpecialAllowance(rs.getBigDecimal("special_allowance"));
        s.setConveyanceAllowance(rs.getBigDecimal("conveyance_allowance"));
        s.setMedicalAllowance(rs.getBigDecimal("medical_allowance"));
        s.setPfRatePct(rs.getBigDecimal("pf_rate_pct"));
        s.setProfessionalTax(rs.getBigDecimal("professional_tax"));
        s.setTdsMonthly(rs.getBigDecimal("tds_monthly"));
        s.setOtherDeductions(rs.getBigDecimal("other_deductions"));
        if (rs.getDate("effective_date") != null) {
            s.setEffectiveDate(rs.getDate("effective_date").toLocalDate());
        }
        try {
            s.setEmployeeCode(rs.getString("employee_code"));
            s.setEmployeeName(rs.getString("emp_name"));
        } catch (Exception ignored) {
        }
        if (rs.getTimestamp("created_at") != null) {
            s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            s.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return s;
    }

    private SalaryComponent mapResultSetToComponent(ResultSet rs) throws SQLException {
        SalaryComponent c = new SalaryComponent();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setCode(rs.getString("code"));
        c.setType(SalaryComponent.ComponentType.valueOf(rs.getString("type")));
        c.setCalculationType(SalaryComponent.CalculationType.valueOf(rs.getString("calculation_type")));
        c.setDefaultRate(rs.getBigDecimal("default_rate"));
        c.setTaxable(rs.getBoolean("is_taxable"));
        c.setMandatory(rs.getBoolean("is_mandatory"));
        c.setDescription(rs.getString("description"));
        c.setActive("ACTIVE".equalsIgnoreCase(rs.getString("status")));
        if (rs.getTimestamp("created_at") != null) {
            c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return c;
    }
}
