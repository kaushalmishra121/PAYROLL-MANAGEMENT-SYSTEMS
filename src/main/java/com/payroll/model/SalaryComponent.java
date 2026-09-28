package com.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SalaryComponent {
    public enum ComponentType {
        EARNING,
        DEDUCTION
    }

    public enum CalculationType {
        FIXED_AMOUNT,
        PERCENTAGE_OF_BASIC,
        PERCENTAGE_OF_GROSS
    }

    private Long id;
    private String name;
    private String code;
    private ComponentType type;
    private CalculationType calculationType;
    private BigDecimal defaultRate = BigDecimal.ZERO;
    private boolean taxable = true;
    private boolean mandatory = false;
    private String description;
    private boolean active = true;
    private LocalDateTime createdAt;

    public SalaryComponent() {
    }

    public SalaryComponent(Long id, String name, String code, ComponentType type, 
                           CalculationType calculationType, BigDecimal defaultRate, boolean taxable, boolean mandatory) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
        this.calculationType = calculationType;
        this.defaultRate = defaultRate;
        this.taxable = taxable;
        this.mandatory = mandatory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    public CalculationType getCalculationType() {
        return calculationType;
    }

    public void setCalculationType(CalculationType calculationType) {
        this.calculationType = calculationType;
    }

    public BigDecimal getDefaultRate() {
        return defaultRate;
    }

    public void setDefaultRate(BigDecimal defaultRate) {
        this.defaultRate = defaultRate;
    }

    public boolean isTaxable() {
        return taxable;
    }

    public void setTaxable(boolean taxable) {
        this.taxable = taxable;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
