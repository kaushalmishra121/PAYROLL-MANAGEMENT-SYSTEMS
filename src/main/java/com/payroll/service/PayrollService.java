package com.payroll.service;

import com.payroll.config.DatabaseConfig;
import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.dao.EmployeeDao;
import com.payroll.dao.EmployeeDaoImpl;
import com.payroll.dao.LeaveDao;
import com.payroll.dao.LeaveDaoImpl;
import com.payroll.dao.PayrollDao;
import com.payroll.dao.PayrollDaoImpl;
import com.payroll.dao.SalaryStructureDao;
import com.payroll.dao.SalaryStructureDaoImpl;
import com.payroll.exception.DatabaseException;
import com.payroll.exception.DuplicateRecordException;
import com.payroll.exception.ValidationException;
import com.payroll.model.AuditLog;
import com.payroll.model.Employee;
import com.payroll.model.EmployeeSalaryStructure;
import com.payroll.model.PayrollAdjustment;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import com.payroll.model.User;
import com.payroll.util.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PayrollService {
    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollDao payrollDao;
    private final EmployeeDao employeeDao;
    private final SalaryStructureDao salaryStructureDao;
    private final LeaveDao leaveDao;
    private final AuditDao auditDao;

    public PayrollService() {
        this.payrollDao = new PayrollDaoImpl();
        this.employeeDao = new EmployeeDaoImpl();
        this.salaryStructureDao = new SalaryStructureDaoImpl();
        this.leaveDao = new LeaveDaoImpl();
        this.auditDao = new AuditDaoImpl();
    }

    public PayrollService(PayrollDao payrollDao, EmployeeDao employeeDao, 
                          SalaryStructureDao salaryStructureDao, LeaveDao leaveDao, AuditDao auditDao) {
        this.payrollDao = payrollDao;
        this.employeeDao = employeeDao;
        this.salaryStructureDao = salaryStructureDao;
        this.leaveDao = leaveDao;
        this.auditDao = auditDao;
    }

    public List<PayrollRun> getAllPayrollRuns() {
        return payrollDao.findAllRuns();
    }

    public Optional<PayrollRun> getPayrollRunById(Long id) {
        return payrollDao.findRunById(id);
    }

    public Optional<PayrollRun> getPayrollRunByPeriod(int month, int year) {
        return payrollDao.findRunByPeriod(month, year);
    }

    public List<PayrollRecord> getPayrollRecords(Long runId) {
        return payrollDao.findRecordsByRunId(runId);
    }

    public Optional<PayrollRecord> getPayrollRecordById(Long recordId) {
        return payrollDao.findRecordById(recordId);
    }

    public List<PayrollRecord> getPayrollRecordsForEmployee(Long employeeId) {
        return payrollDao.findRecordsByEmployeeId(employeeId);
    }

    public PayrollRun processMonthlyPayroll(int month, int year, String notes) {
        UserSession.requireHrOrAdmin();
        if (month < 1 || month > 12) {
            throw new ValidationException("Invalid month: " + month + ". Must be between 1 and 12.");
        }
        if (year < 2000 || year > 2100) {
            throw new ValidationException("Invalid year: " + year);
        }

        User currentUser = UserSession.getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : 1L;

        // Check if payroll already exists for this period
        Optional<PayrollRun> existingRunOpt = payrollDao.findRunByPeriod(month, year);
        if (existingRunOpt.isPresent()) {
            PayrollRun existingRun = existingRunOpt.get();
            if (existingRun.getStatus() == PayrollRun.PayrollStatus.PAID) {
                throw new DuplicateRecordException("Payroll for " + existingRun.getPeriodDisplay() + " has already been PAID and cannot be regenerated.");
            }
            if (existingRun.getStatus() == PayrollRun.PayrollStatus.APPROVED) {
                throw new DuplicateRecordException("Payroll for " + existingRun.getPeriodDisplay() + " is already APPROVED. Reopen or cancel approval before regenerating.");
            }
            // If in DRAFT or CANCELLED, remove previous run and recreate
            payrollDao.deleteRun(existingRun.getId());
            logger.info("Removed previous DRAFT payroll run for period {}/{}", month, year);
        }

        List<Employee> activeEmployees = employeeDao.findActiveEmployees();
        if (activeEmployees.isEmpty()) {
            throw new ValidationException("No active employees found to process payroll for " + month + "/" + year);
        }

        List<PayrollRecord> calculatedRecords = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (Employee emp : activeEmployees) {
            // Check joining date - don't process if joining date is in future months
            LocalDate monthEnd = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());
            if (emp.getJoiningDate() != null && emp.getJoiningDate().isAfter(monthEnd)) {
                continue;
            }

            EmployeeSalaryStructure struct = salaryStructureDao.findByEmployeeId(emp.getId())
                    .orElseGet(() -> new EmployeeSalaryStructure(emp.getId(), emp.getBasicSalary(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("12.00"), new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now()));

            BigDecimal unpaidDays = leaveDao.getUnpaidLeaveDaysInMonth(emp.getId(), month, year);

            PayrollCalculationEngine.CalculationInput input = new PayrollCalculationEngine.CalculationInput();
            input.employee = emp;
            input.structure = struct;
            input.month = month;
            input.year = year;
            input.unpaidLeaveDays = unpaidDays;
            input.overtimeOrBonus = BigDecimal.ZERO;

            PayrollRecord rec = PayrollCalculationEngine.calculateEmployeePayroll(input);
            calculatedRecords.add(rec);

            totalGross = totalGross.add(rec.getGrossEarnings());
            totalDeductions = totalDeductions.add(rec.getTotalDeductions());
            totalNet = totalNet.add(rec.getNetSalary());
        }

        if (calculatedRecords.isEmpty()) {
            throw new ValidationException("No eligible employees were found for the period " + month + "/" + year);
        }

        // Execute Transactional Database Write
        PayrollRun run = new PayrollRun(month, year, userId);
        run.setTotalEmployees(calculatedRecords.size());
        run.setTotalGrossPay(totalGross.setScale(2, RoundingMode.HALF_UP));
        run.setTotalDeductions(totalDeductions.setScale(2, RoundingMode.HALF_UP));
        run.setTotalNetPay(totalNet.setScale(2, RoundingMode.HALF_UP));
        run.setStatus(PayrollRun.PayrollStatus.DRAFT);
        run.setNotes(notes != null ? notes : "Monthly payroll generated automatically.");

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PayrollRun savedRun = payrollDao.saveRun(run, conn);
                for (PayrollRecord rec : calculatedRecords) {
                    rec.setPayrollRunId(savedRun.getId());
                }
                payrollDao.savePayrollRecords(calculatedRecords, conn);

                conn.commit();
                logAudit("PROCESS_PAYROLL", savedRun.getId(), "Generated DRAFT payroll run for " + savedRun.getPeriodDisplay() + " (" + calculatedRecords.size() + " employees)");
                logger.info("Successfully generated payroll run id {} for period {}/{}", savedRun.getId(), month, year);
                return savedRun;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Payroll transaction failed and rolled back for period {}/{}", month, year, e);
                throw new DatabaseException("Failed to process payroll: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection failure during payroll processing", e);
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    public boolean approvePayroll(Long runId) {
        UserSession.requireAdmin();
        PayrollRun run = payrollDao.findRunById(runId)
                .orElseThrow(() -> new ValidationException("Payroll run not found"));

        if (run.getStatus() != PayrollRun.PayrollStatus.DRAFT) {
            throw new ValidationException("Only DRAFT payroll runs can be approved. Current status: " + run.getStatus());
        }

        User currentUser = UserSession.getCurrentUser();
        Long approverId = currentUser != null ? currentUser.getId() : 1L;

        boolean ok = payrollDao.updateRunStatus(runId, PayrollRun.PayrollStatus.APPROVED, approverId, null);
        if (ok) {
            logAudit("APPROVE_PAYROLL", runId, "Approved payroll run for period: " + run.getPeriodDisplay());
        }
        return ok;
    }

    public boolean markPayrollPaid(Long runId, LocalDate paymentDate) {
        UserSession.requireAdmin();
        PayrollRun run = payrollDao.findRunById(runId)
                .orElseThrow(() -> new ValidationException("Payroll run not found"));

        if (run.getStatus() != PayrollRun.PayrollStatus.APPROVED) {
            throw new ValidationException("Payroll run must be APPROVED before marking as PAID. Current status: " + run.getStatus());
        }

        LocalDate payDate = paymentDate != null ? paymentDate : LocalDate.now();
        boolean ok = payrollDao.updateRunStatus(runId, PayrollRun.PayrollStatus.PAID, null, payDate);
        if (ok) {
            // Update individual record statuses
            List<PayrollRecord> records = payrollDao.findRecordsByRunId(runId);
            for (PayrollRecord rec : records) {
                payrollDao.updateRecordPaymentStatus(rec.getId(), PayrollRecord.PaymentStatus.PAID);
            }
            logAudit("PAY_PAYROLL", runId, "Marked payroll run as PAID on " + payDate + " for " + run.getPeriodDisplay());
        }
        return ok;
    }

    public boolean cancelPayroll(Long runId) {
        UserSession.requireAdmin();
        PayrollRun run = payrollDao.findRunById(runId)
                .orElseThrow(() -> new ValidationException("Payroll run not found"));

        if (run.getStatus() == PayrollRun.PayrollStatus.PAID) {
            throw new ValidationException("Cannot cancel a PAID payroll run.");
        }

        boolean ok = payrollDao.updateRunStatus(runId, PayrollRun.PayrollStatus.CANCELLED, null, null);
        if (ok) {
            logAudit("CANCEL_PAYROLL", runId, "Cancelled payroll run for " + run.getPeriodDisplay());
        }
        return ok;
    }

    public PayrollAdjustment addAdjustment(Long recordId, PayrollAdjustment.AdjustmentType type, BigDecimal amount, String reason) {
        UserSession.requireHrOrAdmin();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Adjustment amount must be greater than zero.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Reason for adjustment is required.");
        }

        PayrollRecord record = payrollDao.findRecordById(recordId)
                .orElseThrow(() -> new ValidationException("Payroll record not found"));

        PayrollRun run = payrollDao.findRunById(record.getPayrollRunId())
                .orElseThrow(() -> new ValidationException("Associated payroll run not found"));

        if (run.getStatus() == PayrollRun.PayrollStatus.PAID) {
            throw new ValidationException("Cannot modify records of a PAID payroll run.");
        }

        User current = UserSession.getCurrentUser();
        Long authorizerId = current != null ? current.getId() : 1L;

        PayrollAdjustment adj = new PayrollAdjustment(recordId, type, amount, reason, authorizerId);
        PayrollAdjustment saved = payrollDao.saveAdjustment(adj);

        // Adjust record net salary
        if (type == PayrollAdjustment.AdjustmentType.ADDITION) {
            record.setGrossEarnings(record.getGrossEarnings().add(amount).setScale(2, RoundingMode.HALF_UP));
            record.setNetSalary(record.getNetSalary().add(amount).setScale(2, RoundingMode.HALF_UP));
        } else {
            record.setTotalDeductions(record.getTotalDeductions().add(amount).setScale(2, RoundingMode.HALF_UP));
            record.setNetSalary(record.getNetSalary().subtract(amount).setScale(2, RoundingMode.HALF_UP));
        }

        logAudit("PAYROLL_ADJUSTMENT", saved.getId(), "Adjustment of " + type + " " + amount + " applied to record " + recordId + ": " + reason);
        return saved;
    }

    private void logAudit(String action, Long entityId, String details) {
        User current = UserSession.getCurrentUser();
        Long uid = current != null ? current.getId() : null;
        String uname = current != null ? current.getUsername() : "SYSTEM";
        auditDao.log(new AuditLog(uid, uname, action, "PAYROLL", entityId, details, "127.0.0.1"));
    }
}
