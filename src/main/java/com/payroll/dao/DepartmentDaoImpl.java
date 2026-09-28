package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.exception.DatabaseException;
import com.payroll.model.Department;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepartmentDaoImpl implements DepartmentDao {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentDaoImpl.class);

    @Override
    public Optional<Department> findById(Long id) {
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM employees e WHERE e.department_id = d.id) AS emp_count " +
                     "FROM departments d WHERE d.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDepartment(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding department by id: {}", id, e);
            throw new DatabaseException("Failed to find department by id", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Department> findByCode(String code) {
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM employees e WHERE e.department_id = d.id) AS emp_count " +
                     "FROM departments d WHERE d.code = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDepartment(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding department by code: {}", code, e);
            throw new DatabaseException("Failed to find department by code", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Department> findByName(String name) {
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM employees e WHERE e.department_id = d.id) AS emp_count " +
                     "FROM departments d WHERE d.name = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDepartment(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding department by name: {}", name, e);
            throw new DatabaseException("Failed to find department by name", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Department> findAll() {
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM employees e WHERE e.department_id = d.id) AS emp_count " +
                     "FROM departments d ORDER BY d.name ASC";
        List<Department> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDepartment(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all departments", e);
            throw new DatabaseException("Failed to retrieve departments", e);
        }
        return list;
    }

    @Override
    public List<Department> findActive() {
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM employees e WHERE e.department_id = d.id) AS emp_count " +
                     "FROM departments d WHERE d.status = 'ACTIVE' ORDER BY d.name ASC";
        List<Department> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDepartment(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding active departments", e);
            throw new DatabaseException("Failed to retrieve active departments", e);
        }
        return list;
    }

    @Override
    public Department save(Department department) {
        String sql = "INSERT INTO departments (name, code, description, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, department.getName());
            ps.setString(2, department.getCode());
            ps.setString(3, department.getDescription());
            ps.setString(4, department.getStatus().name());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        department.setId(rs.getLong(1));
                    }
                }
            }
            return department;
        } catch (SQLException e) {
            logger.error("Error saving department: {}", department.getName(), e);
            throw new DatabaseException("Failed to save department: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Department department) {
        String sql = "UPDATE departments SET name = ?, code = ?, description = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, department.getName());
            ps.setString(2, department.getCode());
            ps.setString(3, department.getDescription());
            ps.setString(4, department.getStatus().name());
            ps.setLong(5, department.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating department: {}", department.getId(), e);
            throw new DatabaseException("Failed to update department", e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM departments WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting department: {}", id, e);
            throw new DatabaseException("Failed to delete department. Note: Cannot delete if linked to employees or designations.", e);
        }
    }

    @Override
    public int countEmployeesInDepartment(Long departmentId) {
        String sql = "SELECT COUNT(*) FROM employees WHERE department_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting employees in department {}", departmentId, e);
        }
        return 0;
    }

    @Override
    public int countTotalDepartments() {
        String sql = "SELECT COUNT(*) FROM departments WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting departments", e);
        }
        return 0;
    }

    private Department mapResultSetToDepartment(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setId(rs.getLong("id"));
        d.setName(rs.getString("name"));
        d.setCode(rs.getString("code"));
        d.setDescription(rs.getString("description"));
        d.setStatus(Department.Status.valueOf(rs.getString("status")));
        try {
            d.setEmployeeCount(rs.getInt("emp_count"));
        } catch (Exception ignored) {
        }
        if (rs.getTimestamp("created_at") != null) {
            d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            d.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return d;
    }
}
