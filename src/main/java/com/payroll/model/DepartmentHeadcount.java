package com.payroll.model;

public class DepartmentHeadcount {
    private String departmentName;
    private int headcount;

    public DepartmentHeadcount() {
    }

    public DepartmentHeadcount(String departmentName, int headcount) {
        this.departmentName = departmentName;
        this.headcount = headcount;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getHeadcount() {
        return headcount;
    }

    public void setHeadcount(int headcount) {
        this.headcount = headcount;
    }
}
