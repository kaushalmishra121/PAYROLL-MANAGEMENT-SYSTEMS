package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AuditDaoImpl implements AuditDao {
    private static final Logger logger = LoggerFactory.getLogger(AuditDaoImpl.class);

    @Override
    public void log(AuditLog auditLog) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (auditLog.getUserId() != null) {
                ps.setLong(1, auditLog.getUserId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, auditLog.getAction());
            ps.setString(3, auditLog.getEntityType());
            if (auditLog.getEntityId() != null) {
                ps.setLong(4, auditLog.getEntityId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, auditLog.getDetails());
            ps.setString(6, auditLog.getIpAddress() != null ? auditLog.getIpAddress() : "127.0.0.1");

            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Could not save audit log: {}", e.getMessage());
        }
    }

    @Override
    public List<AuditLog> findRecentLogs(int limit) {
        String sql = "SELECT a.*, u.username FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id ORDER BY a.id DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding recent audit logs", e);
        }
        return list;
    }

    @Override
    public List<AuditLog> findLogsByEntity(String entityType, Long entityId) {
        String sql = "SELECT a.*, u.username FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id WHERE a.entity_type = ? AND a.entity_id = ? ORDER BY a.id DESC";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setLong(2, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding audit logs by entity", e);
        }
        return list;
    }

    private AuditLog mapResultSetToLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        if (!rs.wasNull()) {
            log.setUserId(uid);
        }
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        long eid = rs.getLong("entity_id");
        if (!rs.wasNull()) {
            log.setEntityId(eid);
        }
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUsername(rs.getString("username"));
        if (rs.getTimestamp("created_at") != null) {
            log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return log;
    }
}
