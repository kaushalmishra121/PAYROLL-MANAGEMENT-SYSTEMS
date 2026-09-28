package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.exception.DatabaseException;
import com.payroll.model.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttendanceDaoImpl implements AttendanceDao {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceDaoImpl.class);

    private static final String BASE_SELECT =
            "SELECT a.*, e.employee_code, CONCAT(e.first_name, ' ', e.last_name) AS emp_name, d.name AS dept_name " +
            "FROM attendance a " +
            "JOIN employees e ON a.employee_id = e.id " +
            "LEFT JOIN departments d ON e.department_id = d.id ";

    @Override
    public Optional<Attendance> findByEmployeeAndDate(Long employeeId, LocalDate date) {
        String sql = BASE_SELECT + "WHERE a.employee_id = ? AND a.attendance_date = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error querying attendance for employee {} on {}", employeeId, date, e);
            throw new DatabaseException("Failed to query attendance record", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Attendance> findByMonthAndYear(int month, int year) {
        String sql = BASE_SELECT + "WHERE MONTH(a.attendance_date) = ? AND YEAR(a.attendance_date) = ? ORDER BY a.attendance_date DESC, e.employee_code ASC";
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding attendance by month/year", e);
            throw new DatabaseException("Failed to retrieve attendance list", e);
        }
        return list;
    }

    @Override
    public List<Attendance> findByEmployeeAndMonth(Long employeeId, int month, int year) {
        String sql = BASE_SELECT + "WHERE a.employee_id = ? AND MONTH(a.attendance_date) = ? AND YEAR(a.attendance_date) = ? ORDER BY a.attendance_date ASC";
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding attendance for employee {} by month/year", employeeId, e);
            throw new DatabaseException("Failed to retrieve employee attendance", e);
        }
        return list;
    }

    @Override
    public Attendance save(Attendance att) {
        String sql = "INSERT INTO attendance (employee_id, attendance_date, status, check_in_time, check_out_time, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, att.getEmployeeId());
            ps.setDate(2, Date.valueOf(att.getAttendanceDate()));
            ps.setString(3, att.getStatus().name());
            ps.setTime(4, att.getCheckInTime() != null ? Time.valueOf(att.getCheckInTime()) : null);
            ps.setTime(5, att.getCheckOutTime() != null ? Time.valueOf(att.getCheckOutTime()) : null);
            ps.setString(6, att.getNotes());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        att.setId(rs.getLong(1));
                    }
                }
            }
            return att;
        } catch (SQLException e) {
            logger.error("Error saving attendance for employee {} on {}", att.getEmployeeId(), att.getAttendanceDate(), e);
            throw new DatabaseException("Failed to record attendance: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Attendance att) {
        String sql = "UPDATE attendance SET status = ?, check_in_time = ?, check_out_time = ?, notes = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, att.getStatus().name());
            ps.setTime(2, att.getCheckInTime() != null ? Time.valueOf(att.getCheckInTime()) : null);
            ps.setTime(3, att.getCheckOutTime() != null ? Time.valueOf(att.getCheckOutTime()) : null);
            ps.setString(4, att.getNotes());
            ps.setLong(5, att.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating attendance id: {}", att.getId(), e);
            throw new DatabaseException("Failed to update attendance record", e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM attendance WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting attendance id: {}", id, e);
            throw new DatabaseException("Failed to delete attendance record", e);
        }
    }

    @Override
    public int countStatusForEmployeeInMonth(Long employeeId, Attendance.AttendanceStatus status, int month, int year) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employee_id = ? AND status = ? AND MONTH(attendance_date) = ? AND YEAR(attendance_date) = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.setString(2, status.name());
            ps.setInt(3, month);
            ps.setInt(4, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting status {} for employee {} in month/year", status, employeeId, e);
        }
        return 0;
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getLong("id"));
        a.setEmployeeId(rs.getLong("employee_id"));
        if (rs.getDate("attendance_date") != null) {
            a.setAttendanceDate(rs.getDate("attendance_date").toLocalDate());
        }
        a.setStatus(Attendance.AttendanceStatus.valueOf(rs.getString("status")));
        if (rs.getTime("check_in_time") != null) {
            a.setCheckInTime(rs.getTime("check_in_time").toLocalTime());
        }
        if (rs.getTime("check_out_time") != null) {
            a.setCheckOutTime(rs.getTime("check_out_time").toLocalTime());
        }
        a.setNotes(rs.getString("notes"));
        try {
            a.setEmployeeCode(rs.getString("employee_code"));
            a.setEmployeeName(rs.getString("emp_name"));
            a.setDepartmentName(rs.getString("dept_name"));
        } catch (Exception ignored) {
        }
        if (rs.getTimestamp("created_at") != null) {
            a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return a;
    }
}
