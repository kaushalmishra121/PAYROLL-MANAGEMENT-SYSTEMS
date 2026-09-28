package com.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollAdjustment {
    public enum AdjustmentType {
        ADDITION,
        DEDUCTION
    }

    private Long id;
    private Long payrollRecordId;
    private AdjustmentType adjustmentType;
    private BigDecimal amount;
    private String reason;
    private Long authorizedBy;
    private String authorizerName;
    private LocalDateTime createdAt;

    public PayrollAdjustment() {
    }

    public PayrollAdjustment(Long payrollRecordId, AdjustmentType adjustmentType, BigDecimal amount, String reason, Long authorizedBy) {
        this.payrollRecordId = payrollRecordId;
        this.adjustmentType = adjustmentType;
        this.amount = amount;
        this.reason = reason;
        this.authorizedBy = authorizedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPayrollRecordId() {
        return payrollRecordId;
    }

    public void setPayrollRecordId(Long payrollRecordId) {
        this.payrollRecordId = payrollRecordId;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getAuthorizedBy() {
        return authorizedBy;
    }

    public void setAuthorizedBy(Long authorizedBy) {
        this.authorizedBy = authorizedBy;
    }

    public String getAuthorizerName() {
        return authorizerName;
    }

    public void setAuthorizerName(String authorizerName) {
        this.authorizerName = authorizerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
