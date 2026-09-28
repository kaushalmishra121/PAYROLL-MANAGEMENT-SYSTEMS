package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.exception.DatabaseException;
import com.payroll.model.DepartmentHeadcount;
import com.payroll.model.Employee;
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

public class EmployeeDaoImpl implements EmployeeDao {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDaoImpl.class);

    private static final String BASE_SELECT = 
            "SELECT e.*, d.name AS dept_name, ds.title AS desig_title, " +
            "       s.basic_salary, " +
            "       (COALESCE(s.basic_salary,0) + COALESCE(s.hra,0) + COALESCE(s.special_allowance,0) + COALESCE(s.conveyance_allowance,0) + COALESCE(s.medical_allowance,0)) AS gross_calc " +
            "FROM employees e " +
            "LEFT JOIN departments d ON e.department_id = d.id " +
            "LEFT JOIN designations ds ON e.designation_id = ds.id " +
            "LEFT JOIN employee_salary_structures s ON e.id = s.employee_id ";

    @Override
    public Optional<Employee> findById(Long id) {
        String sql = BASE_SELECT + "WHERE e.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding employee by id: {}", id, e);
            throw new DatabaseException("Failed to find employee by id", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Employee> findByCode(String code) {
        String sql = BASE_SELECT + "WHERE e.employee_code = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding employee by code: {}", code, e);
            throw new DatabaseException("Failed to find employee by code", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        String sql = BASE_SELECT + "WHERE e.email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding employee by email: {}", email, e);
            throw new DatabaseException("Failed to find employee by email", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Employee> findAll() {
        String sql = BASE_SELECT + "ORDER BY e.id ASC";
        List<Employee> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all employees", e);
            throw new DatabaseException("Failed to retrieve employees", e);
        }
        return list;
    }

    @Override
    public List<Employee> findActiveEmployees() {
        String sql = BASE_SELECT + "WHERE e.employment_status IN ('ACTIVE', 'PROBATION') ORDER BY e.id ASC";
        List<Employee> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding active employees", e);
            throw new DatabaseException("Failed to retrieve active employees", e);
        }
        return list;
    }

    @Override
    public List<Employee> searchAndFilter(String query, Long departmentId, Long designationId, Employee.EmploymentStatus status) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(e.first_name) LIKE ? OR LOWER(e.last_name) LIKE ? OR LOWER(e.employee_code) LIKE ? OR LOWER(e.email) LIKE ? OR LOWER(d.name) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (departmentId != null && departmentId > 0) {
            sql.append("AND e.department_id = ? ");
            params.add(departmentId);
        }

        if (designationId != null && designationId > 0) {
            sql.append("AND e.designation_id = ? ");
            params.add(designationId);
        }

        if (status != null) {
            sql.append("AND e.employment_status = ? ");
            params.add(status.name());
        }

        sql.append("ORDER BY e.id ASC");

        List<Employee> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching and filtering employees", e);
            throw new DatabaseException("Failed to filter employees", e);
        }
        return list;
    }

    @Override
    public Employee save(Employee employee) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return saveWithConnection(employee, conn);
        } catch (SQLException e) {
            logger.error("Error saving employee: {}", employee.getEmployeeCode(), e);
            throw new DatabaseException("Failed to save employee: " + e.getMessage(), e);
        }
    }

    @Override
    public Employee saveWithConnection(Employee employee, Connection conn) {
        String sql = "INSERT INTO employees (employee_code, first_name, last_name, email, phone, address, " +
                     "department_id, designation_id, joining_date, employment_status, bank_name, account_number, ifsc_or_routing, pan_or_tax_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getEmployeeCode());
            ps.setString(2, employee.getFirstName());
            ps.setString(3, employee.getLastName());
            ps.setString(4, employee.getEmail());
            ps.setString(5, employee.getPhone());
            ps.setString(6, employee.getAddress());
            ps.setLong(7, employee.getDepartmentId());
            ps.setLong(8, employee.getDesignationId());
            ps.setDate(9, Date.valueOf(employee.getJoiningDate()));
            ps.setString(10, employee.getEmploymentStatus().name());
            ps.setString(11, employee.getBankName());
            ps.setString(12, employee.getAccountNumber());
            ps.setString(13, employee.getIfscOrRouting());
            ps.setString(14, employee.getPanOrTaxId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        employee.setId(rs.getLong(1));
                    }
                }
            }
            return employee;
        } catch (SQLException e) {
            logger.error("Error saving employee in transaction: {}", employee.getEmployeeCode(), e);
            throw new DatabaseException("Failed to save employee: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Employee employee) {
        String sql = "UPDATE employees SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ?, " +
                     "department_id = ?, designation_id = ?, joining_date = ?, employment_status = ?, " +
                     "bank_name = ?, account_number = ?, ifsc_or_routing = ?, pan_or_tax_id = ? " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getFirstName());
            ps.setString(2, employee.getLastName());
            ps.setString(3, employee.getEmail());
            ps.setString(4, employee.getPhone());
            ps.setString(5, employee.getAddress());
            ps.setLong(6, employee.getDepartmentId());
            ps.setLong(7, employee.getDesignationId());
            ps.setDate(8, Date.valueOf(employee.getJoiningDate()));
            ps.setString(9, employee.getEmploymentStatus().name());
            ps.setString(10, employee.getBankName());
            ps.setString(11, employee.getAccountNumber());
            ps.setString(12, employee.getIfscOrRouting());
            ps.setString(13, employee.getPanOrTaxId());
            ps.setLong(14, employee.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating employee id: {}", employee.getId(), e);
            throw new DatabaseException("Failed to update employee: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(Long id, Employee.EmploymentStatus status) {
        String sql = "UPDATE employees SET employment_status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating employee status id: {}", id, e);
            throw new DatabaseException("Failed to update employee status", e);
        }
    }

    @Override
    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM employees";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting total employees", e);
        }
        return 0;
    }

    @Override
    public int countActive() {
        String sql = "SELECT COUNT(*) FROM employees WHERE employment_status IN ('ACTIVE', 'PROBATION')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting active employees", e);
        }
        return 0;
    }

    @Override
    public int countInactive() {
        String sql = "SELECT COUNT(*) FROM employees WHERE employment_status NOT IN ('ACTIVE', 'PROBATION')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting inactive employees", e);
        }
        return 0;
    }

    @Override
    public List<DepartmentHeadcount> getDepartmentHeadcounts() {
        String sql = "SELECT d.name AS dept_name, COUNT(e.id) AS headcount " +
                     "FROM departments d " +
                     "LEFT JOIN employees e ON d.id = e.department_id AND e.employment_status IN ('ACTIVE', 'PROBATION') " +
                     "GROUP BY d.id, d.name ORDER BY headcount DESC";
        List<DepartmentHeadcount> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DepartmentHeadcount(rs.getString("dept_name"), rs.getInt("headcount")));
            }
        } catch (SQLException e) {
            logger.error("Error getting department headcounts", e);
        }
        return list;
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setId(rs.getLong("id"));
        emp.setEmployeeCode(rs.getString("employee_code"));
        emp.setFirstName(rs.getString("first_name"));
        emp.setLastName(rs.getString("last_name"));
        emp.setEmail(rs.getString("email"));
        emp.setPhone(rs.getString("phone"));
        emp.setAddress(rs.getString("address"));
        emp.setDepartmentId(rs.getLong("department_id"));
        emp.setDesignationId(rs.getLong("designation_id"));
        if (rs.getDate("joining_date") != null) {
            emp.setJoiningDate(rs.getDate("joining_date").toLocalDate());
        }
        emp.setEmploymentStatus(Employee.EmploymentStatus.valueOf(rs.getString("employment_status")));
        emp.setBankName(rs.getString("bank_name"));
        emp.setAccountNumber(rs.getString("account_number"));
        emp.setIfscOrRouting(rs.getString("ifsc_or_routing"));
        emp.setPanOrTaxId(rs.getString("pan_or_tax_id"));

        try {
            emp.setDepartmentName(rs.getString("dept_name"));
            emp.setDesignationTitle(rs.getString("desig_title"));
            emp.setBasicSalary(rs.getBigDecimal("basic_salary"));
            emp.setGrossSalary(rs.getBigDecimal("gross_calc"));
        } catch (Exception ignored) {
        }

        if (rs.getTimestamp("created_at") != null) {
            emp.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            emp.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return emp;
    }
}
