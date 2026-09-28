package com.payroll.service;

import com.payroll.dao.AttendanceDao;
import com.payroll.dao.AttendanceDaoImpl;
import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.exception.DuplicateRecordException;
import com.payroll.exception.ValidationException;
import com.payroll.model.Attendance;
import com.payroll.model.AuditLog;
import com.payroll.model.User;
import com.payroll.util.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AttendanceService {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceDao attendanceDao;
    private final AuditDao auditDao;

    public AttendanceService() {
        this.attendanceDao = new AttendanceDaoImpl();
        this.auditDao = new AuditDaoImpl();
    }

    public AttendanceService(AttendanceDao attendanceDao, AuditDao auditDao) {
        this.attendanceDao = attendanceDao;
        this.auditDao = auditDao;
    }

    public List<Attendance> getAttendanceForMonth(int month, int year) {
        return attendanceDao.findByMonthAndYear(month, year);
    }

    public List<Attendance> getAttendanceForEmployee(Long employeeId, int month, int year) {
        return attendanceDao.findByEmployeeAndMonth(employeeId, month, year);
    }

    public Optional<Attendance> getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date) {
        return attendanceDao.findByEmployeeAndDate(employeeId, date);
    }

    public Attendance recordAttendance(Attendance att) {
        UserSession.requireHrOrAdmin();
        if (att.getEmployeeId() == null || att.getEmployeeId() <= 0) {
            throw new ValidationException("Please select an employee.");
        }
        if (att.getAttendanceDate() == null) {
            throw new ValidationException("Attendance date is required.");
        }
        if (att.getStatus() == null) {
            att.setStatus(Attendance.AttendanceStatus.PRESENT);
        }

        Optional<Attendance> existing = attendanceDao.findByEmployeeAndDate(att.getEmployeeId(), att.getAttendanceDate());
        if (existing.isPresent()) {
            throw new DuplicateRecordException("Attendance already recorded for this employee on " + att.getAttendanceDate());
        }

        Attendance saved = attendanceDao.save(att);
        logAudit("RECORD_ATTENDANCE", saved.getId(), "Recorded attendance for employee " + att.getEmployeeId() + " on " + att.getAttendanceDate() + " as " + att.getStatus());
        return saved;
    }

    public boolean updateAttendance(Attendance att) {
        UserSession.requireHrOrAdmin();
        if (att.getId() == null) {
            throw new ValidationException("Attendance record ID missing.");
        }
        boolean ok = attendanceDao.update(att);
        if (ok) {
            logAudit("UPDATE_ATTENDANCE", att.getId(), "Updated attendance on " + att.getAttendanceDate() + " to " + att.getStatus());
        }
        return ok;
    }

    public boolean deleteAttendance(Long id) {
        UserSession.requireHrOrAdmin();
        boolean ok = attendanceDao.delete(id);
        if (ok) {
            logAudit("DELETE_ATTENDANCE", id, "Deleted attendance record id " + id);
        }
        return ok;
    }

    private void logAudit(String action, Long entityId, String details) {
        User current = UserSession.getCurrentUser();
        Long uid = current != null ? current.getId() : null;
        String uname = current != null ? current.getUsername() : "SYSTEM";
        auditDao.log(new AuditLog(uid, uname, action, "ATTENDANCE", entityId, details, "127.0.0.1"));
    }
}
