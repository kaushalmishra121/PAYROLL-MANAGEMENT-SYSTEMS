package com.payroll.dao;

import com.payroll.config.DatabaseConfig;
import com.payroll.exception.DatabaseException;
import com.payroll.model.MonthlyTrendItem;
import com.payroll.model.PayrollAdjustment;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import com.payroll.util.DateUtils;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PayrollDaoImpl implements PayrollDao {
    private static final Logger logger = LoggerFactory.getLogger(PayrollDaoImpl.class);

    private static final String BASE_RUN_SELECT =
            "SELECT r.*, u1.full_name AS processed_by_name, u2.full_name AS approved_by_name " +
            "FROM payroll_runs r " +
            "JOIN users u1 ON r.processed_by = u1.id " +
            "LEFT JOIN users u2 ON r.approved_by = u2.id ";

    @Override
    public Optional<PayrollRun> findRunById(Long runId) {
        String sql = BASE_RUN_SELECT + "WHERE r.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRun(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding payroll run by id: {}", runId, e);
            throw new DatabaseException("Failed to find payroll run", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<PayrollRun> findRunByPeriod(int month, int year) {
        String sql = BASE_RUN_SELECT + "WHERE r.payroll_month = ? AND r.payroll_year = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRun(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding payroll run by period {}/{}", month, year, e);
            throw new DatabaseException("Failed to find payroll run by period", e);
        }
        return Optional.empty();
    }

    @Override
    public List<PayrollRun> findAllRuns() {
        String sql = BASE_RUN_SELECT + "ORDER BY r.payroll_year DESC, r.payroll_month DESC";
        List<PayrollRun> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToRun(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all payroll runs", e);
            throw new DatabaseException("Failed to retrieve payroll runs", e);
        }
        return list;
    }

    @Override
    public PayrollRun saveRun(PayrollRun run, Connection conn) {
        String sql = "INSERT INTO payroll_runs (payroll_month, payroll_year, run_date, total_employees, " +
                     "total_gross_pay, total_deductions, total_net_pay, status, processed_by, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, run.getPayrollMonth());
            ps.setInt(2, run.getPayrollYear());
            ps.setDate(3, Date.valueOf(run.getRunDate()));
            ps.setInt(4, run.getTotalEmployees());
            ps.setBigDecimal(5, run.getTotalGrossPay());
            ps.setBigDecimal(6, run.getTotalDeductions());
            ps.setBigDecimal(7, run.getTotalNetPay());
            ps.setString(8, run.getStatus().name());
            ps.setLong(9, run.getProcessedBy());
            ps.setString(10, run.getNotes());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        run.setId(rs.getLong(1));
                    }
                }
            }
            return run;
        } catch (SQLException e) {
            logger.error("Error saving payroll run in transaction for {}/{}", run.getPayrollMonth(), run.getPayrollYear(), e);
            throw new DatabaseException("Failed to save payroll run header: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateRunHeader(PayrollRun run, Connection conn) {
        String sql = "UPDATE payroll_runs SET total_employees = ?, total_gross_pay = ?, total_deductions = ?, " +
                     "total_net_pay = ?, status = ?, notes = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, run.getTotalEmployees());
            ps.setBigDecimal(2, run.getTotalGrossPay());
            ps.setBigDecimal(3, run.getTotalDeductions());
            ps.setBigDecimal(4, run.getTotalNetPay());
            ps.setString(5, run.getStatus().name());
            ps.setString(6, run.getNotes());
            ps.setLong(7, run.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating payroll run header id: {}", run.getId(), e);
            throw new DatabaseException("Failed to update payroll run header", e);
        }
    }

    @Override
    public boolean updateRunStatus(Long runId, PayrollRun.PayrollStatus status, Long approvedBy, LocalDate paymentDate) {
        StringBuilder sql = new StringBuilder("UPDATE payroll_runs SET status = ? ");
        List<Object> params = new ArrayList<>();
        params.add(status.name());

        if (approvedBy != null) {
            sql.append(", approved_by = ?, approved_at = ? ");
            params.add(approvedBy);
            params.add(Timestamp.valueOf(LocalDateTime.now()));
        }

        if (paymentDate != null) {
            sql.append(", payment_date = ? ");
            params.add(Date.valueOf(paymentDate));
        }

        sql.append("WHERE id = ?");
        params.add(runId);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating payroll run status for id: {}", runId, e);
            throw new DatabaseException("Failed to update payroll run status", e);
        }
    }

    @Override
    public boolean deleteRun(Long runId) {
        String sql = "DELETE FROM payroll_runs WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting payroll run id: {}", runId, e);
            throw new DatabaseException("Failed to delete payroll run", e);
        }
    }

    @Override
    public boolean savePayrollRecords(List<PayrollRecord> records, Connection conn) {
        String sql = "INSERT INTO payroll_records (payroll_run_id, employee_id, employee_code, employee_name, " +
                     "department_name, designation_title, total_working_days, payable_days, unpaid_leave_days, " +
                     "basic_salary, hra, special_allowance, conveyance_allowance, medical_allowance, overtime_or_bonus, gross_earnings, " +
                     "pf_deduction, professional_tax, tds_deduction, unpaid_leave_deduction, other_deductions, total_deductions, " +
                     "net_salary, payment_status, payment_method, bank_name, account_number_masked) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (PayrollRecord rec : records) {
                ps.setLong(1, rec.getPayrollRunId());
                ps.setLong(2, rec.getEmployeeId());
                ps.setString(3, rec.getEmployeeCode());
                ps.setString(4, rec.getEmployeeName());
                ps.setString(5, rec.getDepartmentName());
                ps.setString(6, rec.getDesignationTitle());
                ps.setInt(7, rec.getTotalWorkingDays());
                ps.setBigDecimal(8, rec.getPayableDays());
                ps.setBigDecimal(9, rec.getUnpaidLeaveDays());

                ps.setBigDecimal(10, rec.getBasicSalary());
                ps.setBigDecimal(11, rec.getHra());
                ps.setBigDecimal(12, rec.getSpecialAllowance());
                ps.setBigDecimal(13, rec.getConveyanceAllowance());
                ps.setBigDecimal(14, rec.getMedicalAllowance());
                ps.setBigDecimal(15, rec.getOvertimeOrBonus());
                ps.setBigDecimal(16, rec.getGrossEarnings());

                ps.setBigDecimal(17, rec.getPfDeduction());
                ps.setBigDecimal(18, rec.getProfessionalTax());
                ps.setBigDecimal(19, rec.getTdsDeduction());
                ps.setBigDecimal(20, rec.getUnpaidLeaveDeduction());
                ps.setBigDecimal(21, rec.getOtherDeductions());
                ps.setBigDecimal(22, rec.getTotalDeductions());

                ps.setBigDecimal(23, rec.getNetSalary());
                ps.setString(24, rec.getPaymentStatus().name());
                ps.setString(25, rec.getPaymentMethod());
                ps.setString(26, rec.getBankName());
                ps.setString(27, rec.getAccountNumberMasked());

                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            return results.length == records.size();
        } catch (SQLException e) {
            logger.error("Error batch inserting payroll records", e);
            throw new DatabaseException("Failed to batch save payroll records: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PayrollRecord> findRecordsByRunId(Long runId) {
        String sql = "SELECT pr.*, r.payroll_month, r.payroll_year " +
                     "FROM payroll_records pr " +
                     "JOIN payroll_runs r ON pr.payroll_run_id = r.id " +
                     "WHERE pr.payroll_run_id = ? ORDER BY pr.employee_code ASC";
        List<PayrollRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding payroll records for run id: {}", runId, e);
            throw new DatabaseException("Failed to retrieve payroll records", e);
        }
        return list;
    }

    @Override
    public Optional<PayrollRecord> findRecordById(Long recordId) {
        String sql = "SELECT pr.*, r.payroll_month, r.payroll_year " +
                     "FROM payroll_records pr " +
                     "JOIN payroll_runs r ON pr.payroll_run_id = r.id " +
                     "WHERE pr.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRecord(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding payroll record by id: {}", recordId, e);
            throw new DatabaseException("Failed to find payroll record", e);
        }
        return Optional.empty();
    }

    @Override
    public List<PayrollRecord> findRecordsByEmployeeId(Long employeeId) {
        String sql = "SELECT pr.*, r.payroll_month, r.payroll_year " +
                     "FROM payroll_records pr " +
                     "JOIN payroll_runs r ON pr.payroll_run_id = r.id " +
                     "WHERE pr.employee_id = ? ORDER BY r.payroll_year DESC, r.payroll_month DESC";
        List<PayrollRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding payroll records for employee id: {}", employeeId, e);
            throw new DatabaseException("Failed to retrieve employee payslip history", e);
        }
        return list;
    }

    @Override
    public boolean updateRecordPaymentStatus(Long recordId, PayrollRecord.PaymentStatus status) {
        String sql = "UPDATE payroll_records SET payment_status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, recordId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating record payment status for id: {}", recordId, e);
            throw new DatabaseException("Failed to update payment status", e);
        }
    }

    @Override
    public PayrollAdjustment saveAdjustment(PayrollAdjustment adj) {
        String sql = "INSERT INTO payroll_adjustments (payroll_record_id, adjustment_type, amount, reason, authorized_by) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, adj.getPayrollRecordId());
            ps.setString(2, adj.getAdjustmentType().name());
            ps.setBigDecimal(3, adj.getAmount());
            ps.setString(4, adj.getReason());
            ps.setLong(5, adj.getAuthorizedBy());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        adj.setId(rs.getLong(1));
                    }
                }
            }
            return adj;
        } catch (SQLException e) {
            logger.error("Error saving payroll adjustment", e);
            throw new DatabaseException("Failed to record adjustment: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PayrollAdjustment> findAdjustmentsByRecordId(Long recordId) {
        String sql = "SELECT pa.*, u.full_name AS authorizer_name " +
                     "FROM payroll_adjustments pa " +
                     "JOIN users u ON pa.authorized_by = u.id " +
                     "WHERE pa.payroll_record_id = ? ORDER BY pa.id ASC";
        List<PayrollAdjustment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PayrollAdjustment a = new PayrollAdjustment();
                    a.setId(rs.getLong("id"));
                    a.setPayrollRecordId(rs.getLong("payroll_record_id"));
                    a.setAdjustmentType(PayrollAdjustment.AdjustmentType.valueOf(rs.getString("adjustment_type")));
                    a.setAmount(rs.getBigDecimal("amount"));
                    a.setReason(rs.getString("reason"));
                    a.setAuthorizedBy(rs.getLong("authorized_by"));
                    a.setAuthorizerName(rs.getString("authorizer_name"));
                    if (rs.getTimestamp("created_at") != null) {
                        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding adjustments for record id: {}", recordId, e);
        }
        return list;
    }

    @Override
    public List<MonthlyTrendItem> getMonthlyPayrollTrends(int limitMonths) {
        String sql = "SELECT payroll_month, payroll_year, total_gross_pay, total_net_pay " +
                     "FROM payroll_runs WHERE status != 'CANCELLED' " +
                     "ORDER BY payroll_year ASC, payroll_month ASC LIMIT ?";
        List<MonthlyTrendItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limitMonths);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int m = rs.getInt("payroll_month");
                    int y = rs.getInt("payroll_year");
                    String label = DateUtils.getMonthShortName(m) + " " + String.valueOf(y).substring(2);
                    list.add(new MonthlyTrendItem(label, y, m, rs.getBigDecimal("total_gross_pay"), rs.getBigDecimal("total_net_pay")));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving monthly payroll trends", e);
        }
        return list;
    }

    @Override
    public BigDecimal getCurrentMonthPayrollTotal(int month, int year) {
        String sql = "SELECT total_net_pay FROM payroll_runs WHERE payroll_month = ? AND payroll_year = ? AND status != 'CANCELLED'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving current month payroll total", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalPaidPayroll() {
        String sql = "SELECT COALESCE(SUM(total_net_pay), 0) FROM payroll_runs WHERE status = 'PAID'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting total paid payroll", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalUnpaidPayroll() {
        String sql = "SELECT COALESCE(SUM(total_net_pay), 0) FROM payroll_runs WHERE status IN ('DRAFT', 'APPROVED')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting total unpaid payroll", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public int countPendingApprovals() {
        String sql = "SELECT COUNT(*) FROM payroll_runs WHERE status = 'DRAFT'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting pending payroll approvals", e);
        }
        return 0;
    }

    private PayrollRun mapResultSetToRun(ResultSet rs) throws SQLException {
        PayrollRun r = new PayrollRun();
        r.setId(rs.getLong("id"));
        r.setPayrollMonth(rs.getInt("payroll_month"));
        r.setPayrollYear(rs.getInt("payroll_year"));
        if (rs.getDate("run_date") != null) {
            r.setRunDate(rs.getDate("run_date").toLocalDate());
        }
        r.setTotalEmployees(rs.getInt("total_employees"));
        r.setTotalGrossPay(rs.getBigDecimal("total_gross_pay"));
        r.setTotalDeductions(rs.getBigDecimal("total_deductions"));
        r.setTotalNetPay(rs.getBigDecimal("total_net_pay"));
        r.setStatus(PayrollRun.PayrollStatus.valueOf(rs.getString("status")));
        r.setProcessedBy(rs.getLong("processed_by"));
        long appBy = rs.getLong("approved_by");
        if (!rs.wasNull()) {
            r.setApprovedBy(appBy);
        }
        if (rs.getTimestamp("approved_at") != null) {
            r.setApprovedAt(rs.getTimestamp("approved_at").toLocalDateTime());
        }
        if (rs.getDate("payment_date") != null) {
            r.setPaymentDate(rs.getDate("payment_date").toLocalDate());
        }
        r.setNotes(rs.getString("notes"));
        try {
            r.setProcessedByName(rs.getString("processed_by_name"));
            r.setApprovedByName(rs.getString("approved_by_name"));
        } catch (Exception ignored) {
        }
        if (rs.getTimestamp("created_at") != null) {
            r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            r.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return r;
    }

    private PayrollRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        PayrollRecord pr = new PayrollRecord();
        pr.setId(rs.getLong("id"));
        pr.setPayrollRunId(rs.getLong("payroll_run_id"));
        pr.setEmployeeId(rs.getLong("employee_id"));
        pr.setEmployeeCode(rs.getString("employee_code"));
        pr.setEmployeeName(rs.getString("employee_name"));
        pr.setDepartmentName(rs.getString("department_name"));
        pr.setDesignationTitle(rs.getString("designation_title"));
        pr.setTotalWorkingDays(rs.getInt("total_working_days"));
        pr.setPayableDays(rs.getBigDecimal("payable_days"));
        pr.setUnpaidLeaveDays(rs.getBigDecimal("unpaid_leave_days"));

        pr.setBasicSalary(rs.getBigDecimal("basic_salary"));
        pr.setHra(rs.getBigDecimal("hra"));
        pr.setSpecialAllowance(rs.getBigDecimal("special_allowance"));
        pr.setConveyanceAllowance(rs.getBigDecimal("conveyance_allowance"));
        pr.setMedicalAllowance(rs.getBigDecimal("medical_allowance"));
        pr.setOvertimeOrBonus(rs.getBigDecimal("overtime_or_bonus"));
        pr.setGrossEarnings(rs.getBigDecimal("gross_earnings"));

        pr.setPfDeduction(rs.getBigDecimal("pf_deduction"));
        pr.setProfessionalTax(rs.getBigDecimal("professional_tax"));
        pr.setTdsDeduction(rs.getBigDecimal("tds_deduction"));
        pr.setUnpaidLeaveDeduction(rs.getBigDecimal("unpaid_leave_deduction"));
        pr.setOtherDeductions(rs.getBigDecimal("other_deductions"));
        pr.setTotalDeductions(rs.getBigDecimal("total_deductions"));

        pr.setNetSalary(rs.getBigDecimal("net_salary"));
        pr.setPaymentStatus(PayrollRecord.PaymentStatus.valueOf(rs.getString("payment_status")));
        pr.setPaymentMethod(rs.getString("payment_method"));
        pr.setBankName(rs.getString("bank_name"));
        pr.setAccountNumberMasked(rs.getString("account_number_masked"));

        try {
            pr.setPayrollMonth(rs.getInt("payroll_month"));
            pr.setPayrollYear(rs.getInt("payroll_year"));
        } catch (Exception ignored) {
        }

        if (rs.getTimestamp("created_at") != null) {
            pr.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return pr;
    }
}
