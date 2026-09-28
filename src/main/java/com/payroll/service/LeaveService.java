package com.payroll.service;

import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.dao.LeaveDao;
import com.payroll.dao.LeaveDaoImpl;
import com.payroll.exception.ValidationException;
import com.payroll.model.AuditLog;
import com.payroll.model.LeaveRequest;
import com.payroll.model.User;
import com.payroll.util.UserSession;
import com.payroll.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class LeaveService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveDao leaveDao;
    private final AuditDao auditDao;

    public LeaveService() {
        this.leaveDao = new LeaveDaoImpl();
        this.auditDao = new AuditDaoImpl();
    }

    public LeaveService(LeaveDao leaveDao, AuditDao auditDao) {
        this.leaveDao = leaveDao;
        this.auditDao = auditDao;
    }

    public List<LeaveRequest> getAllLeaves() {
        return leaveDao.findAll();
    }

    public List<LeaveRequest> getLeavesByEmployee(Long employeeId) {
        return leaveDao.findByEmployeeId(employeeId);
    }

    public List<LeaveRequest> getLeavesByStatus(LeaveRequest.LeaveStatus status) {
        return leaveDao.findByStatus(status);
    }

    public Optional<LeaveRequest> getLeaveById(Long id) {
        return leaveDao.findById(id);
    }

    public LeaveRequest applyLeave(LeaveRequest request) {
        UserSession.requireHrOrAdmin();
        if (request.getEmployeeId() == null || request.getEmployeeId() <= 0) {
            throw new ValidationException("Please select an employee.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ValidationException("Start date and End date are required.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot be before Start date.");
        }
        ValidationUtils.requireNonBlank(request.getReason(), "Leave Reason");

        long daysBetween = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (request.getTotalDays() == null || request.getTotalDays().compareTo(BigDecimal.ZERO) <= 0) {
            request.setTotalDays(BigDecimal.valueOf(daysBetween));
        }

        request.setStatus(LeaveRequest.LeaveStatus.PENDING);
        LeaveRequest saved = leaveDao.save(request);
        logAudit("APPLY_LEAVE", saved.getId(), "Submitted leave application for employee " + request.getEmployeeId() + " from " + request.getStartDate() + " to " + request.getEndDate());
        return saved;
    }

    public boolean reviewLeave(Long leaveId, LeaveRequest.LeaveStatus newStatus, String comments) {
        UserSession.requireHrOrAdmin();
        User currentUser = UserSession.getCurrentUser();
        Long reviewerId = currentUser != null ? currentUser.getId() : null;

        boolean ok = leaveDao.updateStatus(leaveId, newStatus, reviewerId, comments);
        if (ok) {
            logAudit("REVIEW_LEAVE", leaveId, "Leave status updated to: " + newStatus + ". Comments: " + comments);
        }
        return ok;
    }

    public int countPendingLeaves() {
        return leaveDao.countPendingLeaves();
    }

    private void logAudit(String action, Long entityId, String details) {
        User current = UserSession.getCurrentUser();
        Long uid = current != null ? current.getId() : null;
        String uname = current != null ? current.getUsername() : "SYSTEM";
        auditDao.log(new AuditLog(uid, uname, action, "LEAVE", entityId, details, "127.0.0.1"));
    }
}
