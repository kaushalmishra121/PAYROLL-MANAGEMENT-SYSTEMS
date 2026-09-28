package com.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Designation {
    public enum Status {
        ACTIVE,
        INACTIVE
    }

    private Long id;
    private Long departmentId;
    private String departmentName;
    private String title;
    private String code;
    private BigDecimal minSalary = BigDecimal.ZERO;
    private BigDecimal maxSalary = BigDecimal.ZERO;
    private Status status = Status.ACTIVE;
    private int employeeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Designation() {
    }

    public Designation(Long id, Long departmentId, String title, String code, BigDecimal minSalary, BigDecimal maxSalary, Status status) {
        this.id = id;
        this.departmentId = departmentId;
        this.title = title;
        this.code = code;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
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

    @Override
    public String toString() {
        return title + (departmentName != null ? " (" + departmentName + ")" : "");
    }
}
