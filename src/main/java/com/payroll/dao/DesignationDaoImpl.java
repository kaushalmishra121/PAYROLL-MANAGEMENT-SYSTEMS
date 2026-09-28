package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.exception.DatabaseException;
import com.payroll.model.Designation;
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

public class DesignationDaoImpl implements DesignationDao {
    private static final Logger logger = LoggerFactory.getLogger(DesignationDaoImpl.class);

    @Override
    public Optional<Designation> findById(Long id) {
        String sql = "SELECT ds.*, d.name AS dept_name, (SELECT COUNT(*) FROM employees e WHERE e.designation_id = ds.id) AS emp_count " +
                     "FROM designations ds " +
                     "JOIN departments d ON ds.department_id = d.id " +
                     "WHERE ds.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDesignation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding designation by id: {}", id, e);
            throw new DatabaseException("Failed to find designation by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Designation> findByDepartmentId(Long departmentId) {
        String sql = "SELECT ds.*, d.name AS dept_name, (SELECT COUNT(*) FROM employees e WHERE e.designation_id = ds.id) AS emp_count " +
                     "FROM designations ds " +
                     "JOIN departments d ON ds.department_id = d.id " +
                     "WHERE ds.department_id = ? AND ds.status = 'ACTIVE' ORDER BY ds.title ASC";
        List<Designation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDesignation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding designations by department: {}", departmentId, e);
            throw new DatabaseException("Failed to find designations by department", e);
        }
        return list;
    }

    @Override
    public List<Designation> findAll() {
        String sql = "SELECT ds.*, d.name AS dept_name, (SELECT COUNT(*) FROM employees e WHERE e.designation_id = ds.id) AS emp_count " +
                     "FROM designations ds " +
                     "JOIN departments d ON ds.department_id = d.id " +
                     "ORDER BY d.name ASC, ds.title ASC";
        List<Designation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDesignation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all designations", e);
            throw new DatabaseException("Failed to retrieve designations", e);
        }
        return list;
    }

    @Override
    public List<Designation> findActive() {
        String sql = "SELECT ds.*, d.name AS dept_name, (SELECT COUNT(*) FROM employees e WHERE e.designation_id = ds.id) AS emp_count " +
                     "FROM designations ds " +
                     "JOIN departments d ON ds.department_id = d.id " +
                     "WHERE ds.status = 'ACTIVE' ORDER BY d.name ASC, ds.title ASC";
        List<Designation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDesignation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding active designations", e);
            throw new DatabaseException("Failed to retrieve active designations", e);
        }
        return list;
    }

    @Override
    public Designation save(Designation desig) {
        String sql = "INSERT INTO designations (department_id, title, code, min_salary, max_salary, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, desig.getDepartmentId());
            ps.setString(2, desig.getTitle());
            ps.setString(3, desig.getCode());
            ps.setBigDecimal(4, desig.getMinSalary());
            ps.setBigDecimal(5, desig.getMaxSalary());
            ps.setString(6, desig.getStatus().name());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        desig.setId(rs.getLong(1));
                    }
                }
            }
            return desig;
        } catch (SQLException e) {
            logger.error("Error saving designation: {}", desig.getTitle(), e);
            throw new DatabaseException("Failed to save designation: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Designation desig) {
        String sql = "UPDATE designations SET department_id = ?, title = ?, code = ?, min_salary = ?, max_salary = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, desig.getDepartmentId());
            ps.setString(2, desig.getTitle());
            ps.setString(3, desig.getCode());
            ps.setBigDecimal(4, desig.getMinSalary());
            ps.setBigDecimal(5, desig.getMaxSalary());
            ps.setString(6, desig.getStatus().name());
            ps.setLong(7, desig.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating designation id: {}", desig.getId(), e);
            throw new DatabaseException("Failed to update designation", e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM designations WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting designation: {}", id, e);
            throw new DatabaseException("Failed to delete designation. Cannot delete if assigned to employees.", e);
        }
    }

    @Override
    public int countEmployeesInDesignation(Long designationId) {
        String sql = "SELECT COUNT(*) FROM employees WHERE designation_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, designationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting employees in designation {}", designationId, e);
        }
        return 0;
    }

    private Designation mapResultSetToDesignation(ResultSet rs) throws SQLException {
        Designation ds = new Designation();
        ds.setId(rs.getLong("id"));
        ds.setDepartmentId(rs.getLong("department_id"));
        ds.setTitle(rs.getString("title"));
        ds.setCode(rs.getString("code"));
        ds.setMinSalary(rs.getBigDecimal("min_salary"));
        ds.setMaxSalary(rs.getBigDecimal("max_salary"));
        ds.setStatus(Designation.Status.valueOf(rs.getString("status")));
        try {
            ds.setDepartmentName(rs.getString("dept_name"));
            ds.setEmployeeCount(rs.getInt("emp_count"));
        } catch (Exception ignored) {
        }
        if (rs.getTimestamp("created_at") != null) {
            ds.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            ds.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return ds;
    }
}
