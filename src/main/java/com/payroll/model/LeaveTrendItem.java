package com.payroll.model;

public class LeaveTrendItem {
    private long id;
    private int no;
    private String employeeName;
    private String avatarInitials;
    private int leaveCount;
    private int dueCount;
    private String month;
    private int year;

    public LeaveTrendItem() {
    }

    public LeaveTrendItem(long id, int no, String employeeName, String avatarInitials, int leaveCount, int dueCount, String month, int year) {
        this.id = id;
        this.no = no;
        this.employeeName = employeeName;
        this.avatarInitials = avatarInitials;
        this.leaveCount = leaveCount;
        this.dueCount = dueCount;
        this.month = month;
        this.year = year;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getAvatarInitials() {
        return avatarInitials;
    }

    public void setAvatarInitials(String avatarInitials) {
        this.avatarInitials = avatarInitials;
    }

    public int getLeaveCount() {
        return leaveCount;
    }

    public void setLeaveCount(int leaveCount) {
        this.leaveCount = leaveCount;
    }

    public int getDueCount() {
        return dueCount;
    }

    public void setDueCount(int dueCount) {
        this.dueCount = dueCount;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
