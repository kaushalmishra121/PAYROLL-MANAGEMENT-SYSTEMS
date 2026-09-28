package com.payroll.dao;

import com.payroll.model.AuditLog;

import java.util.List;

public interface AuditDao {
    void log(AuditLog log);
    List<AuditLog> findRecentLogs(int limit);
    List<AuditLog> findLogsByEntity(String entityType, Long entityId);
}
