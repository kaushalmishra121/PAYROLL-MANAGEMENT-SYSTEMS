package com.payroll.service;

import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.model.AuditLog;
import com.payroll.model.User;
import com.payroll.util.UserSession;

import java.util.List;

public class AuditService {
    private final AuditDao auditDao;

    public AuditService() {
        this.auditDao = new AuditDaoImpl();
    }

    public AuditService(AuditDao auditDao) {
        this.auditDao = auditDao;
    }

    public void log(String action, String entityType, Long entityId, String details) {
        User current = UserSession.getCurrentUser();
        Long uid = current != null ? current.getId() : null;
        String uname = current != null ? current.getUsername() : "SYSTEM";
        auditDao.log(new AuditLog(uid, uname, action, entityType, entityId, details, "127.0.0.1"));
    }

    public List<AuditLog> getRecentLogs(int limit) {
        UserSession.requireAdmin();
        return auditDao.findRecentLogs(limit);
    }
}
