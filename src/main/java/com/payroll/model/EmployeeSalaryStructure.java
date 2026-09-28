package com.payroll.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeSalaryStructure {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    // Monthly Earnings
    private BigDecimal basicSalary = BigDecimal.ZERO;
    private BigDecimal hra = BigDecimal.ZERO;
    private BigDecimal specialAllowance = BigDecimal.ZERO;
    private BigDecimal conveyanceAllowance = BigDecimal.ZERO;
    private BigDecimal medicalAllowance = BigDecimal.ZERO;

    // Monthly Deductions Configuration
    private BigDecimal pfRatePct = new BigDecimal("12.00");
    private BigDecimal professionalTax = new BigDecimal("200.00");
    private BigDecimal tdsMonthly = BigDecimal.ZERO;
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    private LocalDate effectiveDate = LocalDate.now();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmployeeSalaryStructure() {
    }

    public EmployeeSalaryStructure(Long employeeId, BigDecimal basicSalary, BigDecimal hra, 
                                   BigDecimal specialAllowance, BigDecimal conveyanceAllowance, 
                                   BigDecimal medicalAllowance, BigDecimal pfRatePct, 
                                   BigDecimal professionalTax, BigDecimal tdsMonthly, 
                                   BigDecimal otherDeductions, LocalDate effectiveDate) {
        this.employeeId = employeeId;
        this.basicSalary = basicSalary != null ? basicSalary : BigDecimal.ZERO;
        this.hra = hra != null ? hra : BigDecimal.ZERO;
        this.specialAllowance = specialAllowance != null ? specialAllowance : BigDecimal.ZERO;
        this.conveyanceAllowance = conveyanceAllowance != null ? conveyanceAllowance : BigDecimal.ZERO;
        this.medicalAllowance = medicalAllowance != null ? medicalAllowance : BigDecimal.ZERO;
        this.pfRatePct = pfRatePct != null ? pfRatePct : new BigDecimal("12.00");
        this.professionalTax = professionalTax != null ? professionalTax : new BigDecimal("200.00");
        this.tdsMonthly = tdsMonthly != null ? tdsMonthly : BigDecimal.ZERO;
        this.otherDeductions = otherDeductions != null ? otherDeductions : BigDecimal.ZERO;
        this.effectiveDate = effectiveDate != null ? effectiveDate : LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary != null ? basicSalary : BigDecimal.ZERO;
    }

    public BigDecimal getHra() {
        return hra;
    }

    public void setHra(BigDecimal hra) {
        this.hra = hra != null ? hra : BigDecimal.ZERO;
    }

    public BigDecimal getSpecialAllowance() {
        return specialAllowance;
    }

    public void setSpecialAllowance(BigDecimal specialAllowance) {
        this.specialAllowance = specialAllowance != null ? specialAllowance : BigDecimal.ZERO;
    }

    public BigDecimal getConveyanceAllowance() {
        return conveyanceAllowance;
    }

    public void setConveyanceAllowance(BigDecimal conveyanceAllowance) {
        this.conveyanceAllowance = conveyanceAllowance != null ? conveyanceAllowance : BigDecimal.ZERO;
    }

    public BigDecimal getMedicalAllowance() {
        return medicalAllowance;
    }

    public void setMedicalAllowance(BigDecimal medicalAllowance) {
        this.medicalAllowance = medicalAllowance != null ? medicalAllowance : BigDecimal.ZERO;
    }

    public BigDecimal getPfRatePct() {
        return pfRatePct;
    }

    public void setPfRatePct(BigDecimal pfRatePct) {
        this.pfRatePct = pfRatePct != null ? pfRatePct : BigDecimal.ZERO;
    }

    public BigDecimal getProfessionalTax() {
        return professionalTax;
    }

    public void setProfessionalTax(BigDecimal professionalTax) {
        this.professionalTax = professionalTax != null ? professionalTax : BigDecimal.ZERO;
    }

    public BigDecimal getTdsMonthly() {
        return tdsMonthly;
    }

    public void setTdsMonthly(BigDecimal tdsMonthly) {
        this.tdsMonthly = tdsMonthly != null ? tdsMonthly : BigDecimal.ZERO;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions != null ? otherDeductions : BigDecimal.ZERO;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper calculation methods
    public BigDecimal calculateGrossSalary() {
        return basicSalary.add(hra).add(specialAllowance).add(conveyanceAllowance).add(medicalAllowance)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePfAmount() {
        if (pfRatePct == null || pfRatePct.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return basicSalary.multiply(pfRatePct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalDeductions() {
        return calculatePfAmount().add(professionalTax).add(tdsMonthly).add(otherDeductions)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetSalary() {
        return calculateGrossSalary().subtract(calculateTotalDeductions())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
