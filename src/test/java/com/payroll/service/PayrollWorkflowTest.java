package com.payroll.service;

import com.payroll.dao.*;
import com.payroll.exception.DuplicateRecordException;
import com.payroll.exception.ValidationException;
import com.payroll.model.*;
import com.payroll.util.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PayrollWorkflowTest {

    private PayrollDao mockPayrollDao;
    private EmployeeDao mockEmployeeDao;
    private SalaryStructureDao mockSalaryStructureDao;
    private LeaveDao mockLeaveDao;
    private AuditDao mockAuditDao;
    private PayrollService payrollService;

    private final List<PayrollRun> runs = new ArrayList<>();
    private final List<PayrollRecord> records = new ArrayList<>();

    @BeforeEach
    void setUp() {
        runs.clear();
        records.clear();

        User admin = new User(1L, "admin", "admin@enterprise.com", "hash", "Admin", User.Role.ADMIN, User.UserStatus.ACTIVE);
        UserSession.setCurrentUser(admin);

        mockPayrollDao = new PayrollDao() {
            @Override
            public Optional<PayrollRun> findRunById(Long runId) {
                return runs.stream().filter(r -> r.getId().equals(runId)).findFirst();
            }

            @Override
            public Optional<PayrollRun> findRunByPeriod(int month, int year) {
                return runs.stream().filter(r -> r.getPayrollMonth() == month && r.getPayrollYear() == year).findFirst();
            }

            @Override public List<PayrollRun> findAllRuns() { return runs; }

            @Override
            public PayrollRun saveRun(PayrollRun run, Connection conn) {
                run.setId((long) (runs.size() + 1));
                runs.add(run);
                return run;
            }

            @Override public boolean updateRunHeader(PayrollRun run, Connection conn) { return true; }

            @Override
            public boolean updateRunStatus(Long runId, PayrollRun.PayrollStatus status, Long approvedBy, LocalDate paymentDate) {
                findRunById(runId).ifPresent(r -> {
                    r.setStatus(status);
                    r.setApprovedBy(approvedBy);
                    r.setPaymentDate(paymentDate);
                });
                return true;
            }

            @Override
            public boolean deleteRun(Long runId) {
                return runs.removeIf(r -> r.getId().equals(runId));
            }

            @Override
            public boolean savePayrollRecords(List<PayrollRecord> recs, Connection conn) {
                records.addAll(recs);
                return true;
            }

            @Override public List<PayrollRecord> findRecordsByRunId(Long runId) { return records; }
            @Override public Optional<PayrollRecord> findRecordById(Long recordId) { return records.stream().findFirst(); }
            @Override public List<PayrollRecord> findRecordsByEmployeeId(Long employeeId) { return records; }
            @Override public boolean updateRecordPaymentStatus(Long recordId, PayrollRecord.PaymentStatus status) { return true; }
            @Override public PayrollAdjustment saveAdjustment(PayrollAdjustment adj) { return adj; }
            @Override public List<PayrollAdjustment> findAdjustmentsByRecordId(Long recordId) { return List.of(); }
            @Override public List<MonthlyTrendItem> getMonthlyPayrollTrends(int limitMonths) { return List.of(); }
            @Override public BigDecimal getCurrentMonthPayrollTotal(int month, int year) { return BigDecimal.ZERO; }
            @Override public BigDecimal getTotalPaidPayroll() { return BigDecimal.ZERO; }
            @Override public BigDecimal getTotalUnpaidPayroll() { return BigDecimal.ZERO; }
            @Override public int countPendingApprovals() { return 0; }
        };

        mockEmployeeDao = new EmployeeDao() {
            @Override public Optional<Employee> findById(Long id) { return Optional.empty(); }
            @Override public Optional<Employee> findByCode(String code) { return Optional.empty(); }
            @Override public Optional<Employee> findByEmail(String email) { return Optional.empty(); }
            @Override public List<Employee> findAll() { return List.of(); }
            @Override
            public List<Employee> findActiveEmployees() {
                Employee e = new Employee(1L, "EMP1001", "Alexander", "Wright", "alex@enterprise.com", "555-1234", 1L, 1L, LocalDate.of(2023, 1, 1), Employee.EmploymentStatus.ACTIVE);
                return List.of(e);
            }
            @Override public List<Employee> searchAndFilter(String query, Long deptId, Long desigId, Employee.EmploymentStatus status) { return List.of(); }
            @Override public Employee save(Employee employee) { return employee; }
            @Override public Employee saveWithConnection(Employee employee, Connection conn) { return employee; }
            @Override public boolean update(Employee employee) { return true; }
            @Override public boolean updateStatus(Long id, Employee.EmploymentStatus status) { return true; }
            @Override public int countTotal() { return 1; }
            @Override public int countActive() { return 1; }
            @Override public int countInactive() { return 0; }
            @Override public List<DepartmentHeadcount> getDepartmentHeadcounts() { return List.of(); }
        };

        mockSalaryStructureDao = new SalaryStructureDao() {
            @Override
            public Optional<EmployeeSalaryStructure> findByEmployeeId(Long employeeId) {
                return Optional.of(new EmployeeSalaryStructure(employeeId, new BigDecimal("60000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("12.00"), new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now()));
            }
            @Override public boolean saveOrUpdate(EmployeeSalaryStructure structure) { return true; }
            @Override public boolean saveOrUpdateWithConnection(EmployeeSalaryStructure structure, Connection conn) { return true; }
            @Override public boolean deleteByEmployeeId(Long employeeId) { return true; }
            @Override public List<SalaryComponent> findAllComponents() { return List.of(); }
            @Override public SalaryComponent saveComponent(SalaryComponent component) { return component; }
            @Override public boolean updateComponent(SalaryComponent component) { return true; }
        };

        mockLeaveDao = new LeaveDao() {
            @Override public Optional<LeaveRequest> findById(Long id) { return Optional.empty(); }
            @Override public List<LeaveRequest> findAll() { return List.of(); }
            @Override public List<LeaveRequest> findByEmployeeId(Long employeeId) { return List.of(); }
            @Override public List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status) { return List.of(); }
            @Override public List<LeaveRequest> findApprovedLeavesInMonth(Long employeeId, int month, int year) { return List.of(); }
            @Override public LeaveRequest save(LeaveRequest leave) { return leave; }
            @Override public boolean updateStatus(Long id, LeaveRequest.LeaveStatus status, Long reviewedBy, String comments) { return true; }
            @Override public int countPendingLeaves() { return 0; }
            @Override public BigDecimal getUnpaidLeaveDaysInMonth(Long employeeId, int month, int year) { return BigDecimal.ZERO; }
        };

        mockAuditDao = new AuditDao() {
            @Override public void log(AuditLog log) {}
            @Override public List<AuditLog> findRecentLogs(int limit) { return List.of(); }
            @Override public List<AuditLog> findLogsByEntity(String entityType, Long entityId) { return List.of(); }
        };

        payrollService = new PayrollService(mockPayrollDao, mockEmployeeDao, mockSalaryStructureDao, mockLeaveDao, mockAuditDao);
    }

    @Test
    @DisplayName("Payroll status workflow: DRAFT -> APPROVED -> PAID")
    void testPayrollStatusTransitions() {
        PayrollRun run = new PayrollRun(5, 2026, 1L);
        run.setId(1L);
        run.setStatus(PayrollRun.PayrollStatus.DRAFT);
        runs.add(run);

        // Approve
        payrollService.approvePayroll(1L);
        assertEquals(PayrollRun.PayrollStatus.APPROVED, run.getStatus());

        // Pay
        payrollService.markPayrollPaid(1L, LocalDate.now());
        assertEquals(PayrollRun.PayrollStatus.PAID, run.getStatus());

        // Cancel on PAID should fail
        assertThrows(ValidationException.class, () -> payrollService.cancelPayroll(1L));
    }

    @Test
    @DisplayName("Duplicate payroll run prevention: PAID payroll cannot be regenerated")
    void testDuplicatePaidPayrollPrevention() {
        PayrollRun paidRun = new PayrollRun(5, 2026, 1L);
        paidRun.setId(1L);
        paidRun.setStatus(PayrollRun.PayrollStatus.PAID);
        runs.add(paidRun);

        assertThrows(DuplicateRecordException.class, () -> payrollService.processMonthlyPayroll(5, 2026, "Duplicate attempt"));
    }
}
