package com.payroll.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardSummary {
    private int totalEmployees;
    private int activeEmployees;
    private int inactiveEmployees;
    private BigDecimal currentMonthPayroll = BigDecimal.ZERO;
    private int pendingApprovals;
    private BigDecimal totalPaidPayroll = BigDecimal.ZERO;
    private BigDecimal totalUnpaidPayroll = BigDecimal.ZERO;
    private int totalDepartments;

    private List<MonthlyTrendItem> monthlyTrends = new ArrayList<>();
    private List<DepartmentHeadcount> departmentDistribution = new ArrayList<>();
    private List<PayrollRun> recentPayrollRuns = new ArrayList<>();

    public DashboardSummary() {
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public int getActiveEmployees() {
        return activeEmployees;
    }

    public void setActiveEmployees(int activeEmployees) {
        this.activeEmployees = activeEmployees;
    }

    public int getInactiveEmployees() {
        return inactiveEmployees;
    }

    public void setInactiveEmployees(int inactiveEmployees) {
        this.inactiveEmployees = inactiveEmployees;
    }

    public BigDecimal getCurrentMonthPayroll() {
        return currentMonthPayroll;
    }

    public void setCurrentMonthPayroll(BigDecimal currentMonthPayroll) {
        this.currentMonthPayroll = currentMonthPayroll != null ? currentMonthPayroll : BigDecimal.ZERO;
    }

    public int getPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(int pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
    }

    public BigDecimal getTotalPaidPayroll() {
        return totalPaidPayroll;
    }

    public void setTotalPaidPayroll(BigDecimal totalPaidPayroll) {
        this.totalPaidPayroll = totalPaidPayroll != null ? totalPaidPayroll : BigDecimal.ZERO;
    }

    public BigDecimal getTotalUnpaidPayroll() {
        return totalUnpaidPayroll;
    }

    public void setTotalUnpaidPayroll(BigDecimal totalUnpaidPayroll) {
        this.totalUnpaidPayroll = totalUnpaidPayroll != null ? totalUnpaidPayroll : BigDecimal.ZERO;
    }

    public int getTotalDepartments() {
        return totalDepartments;
    }

    public void setTotalDepartments(int totalDepartments) {
        this.totalDepartments = totalDepartments;
    }

    public List<MonthlyTrendItem> getMonthlyTrends() {
        return monthlyTrends;
    }

    public void setMonthlyTrends(List<MonthlyTrendItem> monthlyTrends) {
        this.monthlyTrends = monthlyTrends;
    }

    public List<DepartmentHeadcount> getDepartmentDistribution() {
        return departmentDistribution;
    }

    public void setDepartmentDistribution(List<DepartmentHeadcount> departmentDistribution) {
        this.departmentDistribution = departmentDistribution;
    }

    public List<PayrollRun> getRecentPayrollRuns() {
        return recentPayrollRuns;
    }

    public void setRecentPayrollRuns(List<PayrollRun> recentPayrollRuns) {
        this.recentPayrollRuns = recentPayrollRuns;
    }
}
