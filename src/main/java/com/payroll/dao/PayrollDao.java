package com.payroll.dao;

import com.payroll.model.MonthlyTrendItem;
import com.payroll.model.PayrollAdjustment;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollDao {
    Optional<PayrollRun> findRunById(Long runId);
    Optional<PayrollRun> findRunByPeriod(int month, int year);
    List<PayrollRun> findAllRuns();
    PayrollRun saveRun(PayrollRun run, Connection conn);
    boolean updateRunHeader(PayrollRun run, Connection conn);
    boolean updateRunStatus(Long runId, PayrollRun.PayrollStatus status, Long approvedBy, LocalDate paymentDate);
    boolean deleteRun(Long runId);

    boolean savePayrollRecords(List<PayrollRecord> records, Connection conn);
    List<PayrollRecord> findRecordsByRunId(Long runId);
    Optional<PayrollRecord> findRecordById(Long recordId);
    List<PayrollRecord> findRecordsByEmployeeId(Long employeeId);
    boolean updateRecordPaymentStatus(Long recordId, PayrollRecord.PaymentStatus status);

    PayrollAdjustment saveAdjustment(PayrollAdjustment adjustment);
    List<PayrollAdjustment> findAdjustmentsByRecordId(Long recordId);

    // Dashboard & Reporting Aggregations
    List<MonthlyTrendItem> getMonthlyPayrollTrends(int limitMonths);
    BigDecimal getCurrentMonthPayrollTotal(int month, int year);
    BigDecimal getTotalPaidPayroll();
    BigDecimal getTotalUnpaidPayroll();
    int countPendingApprovals();
}
