package com.payroll.model;

import java.math.BigDecimal;

public class GradeStat {
    private int gradeNumber;
    private String gradeName;
    private int employeeCount;
    private BigDecimal totalSalary;

    public GradeStat() {
    }

    public GradeStat(int gradeNumber, String gradeName, int employeeCount, BigDecimal totalSalary) {
        this.gradeNumber = gradeNumber;
        this.gradeName = gradeName;
        this.employeeCount = employeeCount;
        this.totalSalary = totalSalary;
    }

    public int getGradeNumber() {
        return gradeNumber;
    }

    public void setGradeNumber(int gradeNumber) {
        this.gradeNumber = gradeNumber;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
    }

    public BigDecimal getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(BigDecimal totalSalary) {
        this.totalSalary = totalSalary;
    }
}
