package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.exception.DatabaseException;
import com.payroll.model.LeaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LeaveDaoImpl implements LeaveDao {
    private static final Logger logger = LoggerFactory.getLogger(LeaveDaoImpl.class);

    private static final String BASE_SELECT =
            "SELECT l.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name, d.name AS dept_name, u.full_name AS reviewer_name " +
            "FROM leave_requests l " +
            "JOIN employees e ON l.employee_id = e.id " +
            "LEFT JOIN departments d ON e.department_id = d.id " +
            "LEFT JOIN users u ON l.reviewed_by = u.id ";

    @Override
    public Optional<LeaveRequest> findById(Long id) {
        String sql = BASE_SELECT + "WHERE l.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLeave(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding leave request by id: {}", id, e);
            throw new DatabaseException("Failed to find leave request", e);
        }
        return Optional.empty();
    }

    @Override
    public List<LeaveRequest> findAll() {
        String sql = BASE_SELECT + "ORDER BY l.id DESC";
        List<LeaveRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToLeave(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all leave requests", e);
            throw new DatabaseException("Failed to retrieve leave requests", e);
        }
        return list;
    }

    @Override
    public List<LeaveRequest> findByEmployeeId(Long employeeId) {
        String sql = BASE_SELECT + "WHERE l.employee_id = ? ORDER BY l.id DESC";
        List<LeaveRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeave(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding leave requests for employee {}", employeeId, e);
            throw new DatabaseException("Failed to retrieve employee leaves", e);
        }
        return list;
    }

    @Override
    public List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status) {
        String sql = BASE_SELECT + "WHERE l.status = ? ORDER BY l.id DESC";
        List<LeaveRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeave(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding leave requests by status {}", status, e);
            throw new DatabaseException("Failed to retrieve leaves by status", e);
        }
        return list;
    }

    @Override
    public List<LeaveRequest> findApprovedLeavesInMonth(Long employeeId, int month, int year) {
        String sql = BASE_SELECT + "WHERE l.employee_id = ? AND l.status = 'APPROVED' " +
                     "AND ((MONTH(l.start_date) = ? AND YEAR(l.start_date) = ?) OR (MONTH(l.end_date) = ? AND YEAR(l.end_date) = ?))";
        List<LeaveRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ps.setInt(4, month);
            ps.setInt(5, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeave(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding approved leaves in month", e);
            throw new DatabaseException("Failed to retrieve approved leaves for month", e);
        }
        return list;
    }

    @Override
    public LeaveRequest save(LeaveRequest leave) {
        String sql = "INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, total_days, reason, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, leave.getEmployeeId());
            ps.setString(2, leave.getLeaveType().name());
            ps.setDate(3, Date.valueOf(leave.getStartDate()));
            ps.setDate(4, Date.valueOf(leave.getEndDate()));
            ps.setBigDecimal(5, leave.getTotalDays());
            ps.setString(6, leave.getReason());
            ps.setString(7, leave.getStatus().name());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        leave.setId(rs.getLong(1));
                    }
                }
            }
            return leave;
        } catch (SQLException e) {
            logger.error("Error saving leave request for employee {}", leave.getEmployeeId(), e);
            throw new DatabaseException("Failed to apply for leave: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(Long id, LeaveRequest.LeaveStatus status, Long reviewedBy, String comments) {
        String sql = "UPDATE leave_requests SET status = ?, reviewed_by = ?, reviewed_at = ?, comments = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, reviewedBy);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, comments);
            ps.setLong(5, id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating leave request status for id {}", id, e);
            throw new DatabaseException("Failed to update leave status", e);
        }
    }

    @Override
    public int countPendingLeaves() {
        String sql = "SELECT COUNT(*) FROM leave_requests WHERE status = 'PENDING'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting pending leaves", e);
        }
        return 0;
    }

    @Override
    public BigDecimal getUnpaidLeaveDaysInMonth(Long employeeId, int month, int year) {
        String sql = "SELECT COALESCE(SUM(total_days), 0) FROM leave_requests " +
                     "WHERE employee_id = ? AND status = 'APPROVED' AND leave_type = 'UNPAID' " +
                     "AND MONTH(start_date) = ? AND YEAR(start_date) = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error computing unpaid leave days for employee {}", employeeId, e);
        }
        return BigDecimal.ZERO;
    }

    private LeaveRequest mapResultSetToLeave(ResultSet rs) throws SQLException {
        LeaveRequest l = new LeaveRequest();
        l.setId(rs.getLong("id"));
        l.setEmployeeId(rs.getLong("employee_id"));
        l.setLeaveType(LeaveRequest.LeaveType.valueOf(rs.getString("leave_type")));
        if (rs.getDate("start_date") != null) {
            l.setStartDate(rs.getDate("start_date").toLocalDate());
        }
        if (rs.getDate("end_date") != null) {
            l.setEndDate(rs.getDate("end_date").toLocalDate());
        }
        l.setTotalDays(rs.getBigDecimal("total_days"));
        l.setReason(rs.getString("reason"));
        l.setStatus(LeaveRequest.LeaveStatus.valueOf(rs.getString("status")));
        long rev = rs.getLong("reviewed_by");
        if (!rs.wasNull()) {
            l.setReviewedBy(rev);
        }
        if (rs.getTimestamp("reviewed_at") != null) {
            l.setReviewedAt(rs.getTimestamp("reviewed_at").toLocalDateTime());
        }
        l.setComments(rs.getString("comments"));
        try {
            l.setEmployeeCode(rs.getString("employee_code"));
            l.setEmployeeName(rs.getString("emp_name"));
            l.setDepartmentName(rs.getString("dept_name"));
            l.setReviewerName(rs.getString("reviewer_name"));
        } catch (Exception ignored) {
        }
        if (rs.getTimestamp("created_at") != null) {
            l.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return l;
    }
}
