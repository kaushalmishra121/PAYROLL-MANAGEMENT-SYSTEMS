package com.payroll.service;

import com.payroll.dao.AttendanceDao;
import com.payroll.dao.AttendanceDaoImpl;
import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.dao.EmployeeDao;
import com.payroll.dao.EmployeeDaoImpl;
import com.payroll.dao.LeaveDao;
import com.payroll.dao.LeaveDaoImpl;
import com.payroll.dao.PayrollDao;
import com.payroll.dao.PayrollDaoImpl;
import com.payroll.exception.ValidationException;
import com.payroll.model.Attendance;
import com.payroll.model.AuditLog;
import com.payroll.model.Employee;
import com.payroll.model.LeaveRequest;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import com.payroll.model.User;
import com.payroll.util.CsvExporter;
import com.payroll.util.PdfPayslipGenerator;
import com.payroll.util.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final EmployeeDao employeeDao;
    private final PayrollDao payrollDao;
    private final AttendanceDao attendanceDao;
    private final LeaveDao leaveDao;
    private final AuditDao auditDao;

    public ReportService() {
        this.employeeDao = new EmployeeDaoImpl();
        this.payrollDao = new PayrollDaoImpl();
        this.attendanceDao = new AttendanceDaoImpl();
        this.leaveDao = new LeaveDaoImpl();
        this.auditDao = new AuditDaoImpl();
    }

    public ReportService(EmployeeDao employeeDao, PayrollDao payrollDao, 
                         AttendanceDao attendanceDao, LeaveDao leaveDao, AuditDao auditDao) {
        this.employeeDao = employeeDao;
        this.payrollDao = payrollDao;
        this.attendanceDao = attendanceDao;
        this.leaveDao = leaveDao;
        this.auditDao = auditDao;
    }

    public void exportEmployeesCsv(File dest, String query, Long deptId, Long desigId, Employee.EmploymentStatus status) throws Exception {
        validateDestination(dest);
        List<Employee> list = employeeDao.searchAndFilter(query, deptId, desigId, status);
        CsvExporter.exportEmployees(list, dest);
        logAudit("EXPORT_CSV", null, "Exported " + list.size() + " employees to " + dest.getName());
    }

    public void exportPayrollRegisterCsv(Long runId, File dest) throws Exception {
        validateDestination(dest);
        PayrollRun run = payrollDao.findRunById(runId)
                .orElseThrow(() -> new ValidationException("Payroll run not found"));
        List<PayrollRecord> records = payrollDao.findRecordsByRunId(runId);
        CsvExporter.exportPayrollRegister(run, records, dest);
        logAudit("EXPORT_CSV", runId, "Exported payroll register for " + run.getPeriodDisplay() + " to " + dest.getName());
    }

    public void exportAttendanceCsv(int month, int year, File dest) throws Exception {
        validateDestination(dest);
        List<Attendance> list = attendanceDao.findByMonthAndYear(month, year);
        CsvExporter.exportAttendance(list, dest);
        logAudit("EXPORT_CSV", null, "Exported attendance for " + month + "/" + year + " to " + dest.getName());
    }

    public void exportLeavesCsv(File dest) throws Exception {
        validateDestination(dest);
        List<LeaveRequest> list = leaveDao.findAll();
        CsvExporter.exportLeaves(list, dest);
        logAudit("EXPORT_CSV", null, "Exported leave records to " + dest.getName());
    }

    public void generatePayslipPdf(Long recordId, File dest) throws Exception {
        validateDestination(dest);
        PayrollRecord record = payrollDao.findRecordById(recordId)
                .orElseThrow(() -> new ValidationException("Payroll record not found"));

        PayrollRun run = payrollDao.findRunById(record.getPayrollRunId())
                .orElseThrow(() -> new ValidationException("Payroll run not found"));

        Employee emp = employeeDao.findById(record.getEmployeeId()).orElse(null);

        PdfPayslipGenerator.generatePayslipPdf(run, record, emp, dest);
        logAudit("GENERATE_PAYSLIP_PDF", recordId, "Generated PDF payslip for " + record.getEmployeeCode() + " (" + run.getPeriodDisplay() + ")");
    }

    private void validateDestination(File dest) {
        if (dest == null) {
            throw new ValidationException("Invalid export file destination.");
        }
        if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
    }

    private void logAudit(String action, Long entityId, String details) {
        User current = UserSession.getCurrentUser();
        Long uid = current != null ? current.getId() : null;
        String uname = current != null ? current.getUsername() : "SYSTEM";
        auditDao.log(new AuditLog(uid, uname, action, "REPORT", entityId, details, "127.0.0.1"));
    }
}
