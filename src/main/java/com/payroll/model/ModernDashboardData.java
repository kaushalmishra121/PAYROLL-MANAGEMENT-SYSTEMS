package com.payroll.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ModernDashboardData {
    private int totalEmployees;
    private BigDecimal salaryPerMonth;
    private BigDecimal providentFund;
    private String currencyUnit = "BDT";
    private List<GradeStat> gradeStats = new ArrayList<>();
    private List<LeaveTrendItem> leaveTrends = new ArrayList<>();

    public ModernDashboardData() {
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public BigDecimal getSalaryPerMonth() {
        return salaryPerMonth;
    }

    public void setSalaryPerMonth(BigDecimal salaryPerMonth) {
        this.salaryPerMonth = salaryPerMonth;
    }

    public BigDecimal getProvidentFund() {
        return providentFund;
    }

    public void setProvidentFund(BigDecimal providentFund) {
        this.providentFund = providentFund;
    }

    public String getCurrencyUnit() {
        return currencyUnit;
    }

    public void setCurrencyUnit(String currencyUnit) {
        this.currencyUnit = currencyUnit;
    }

    public List<GradeStat> getGradeStats() {
        return gradeStats;
    }

    public void setGradeStats(List<GradeStat> gradeStats) {
        this.gradeStats = gradeStats;
    }

    public List<LeaveTrendItem> getLeaveTrends() {
        return leaveTrends;
    }

    public void setLeaveTrends(List<LeaveTrendItem> leaveTrends) {
        this.leaveTrends = leaveTrends;
    }
}
