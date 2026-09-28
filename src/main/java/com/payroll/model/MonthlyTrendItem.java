package com.payroll.model;

import java.math.BigDecimal;

public class MonthlyTrendItem {
    private String monthLabel;
    private int year;
    private int month;
    private BigDecimal grossAmount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;

    public MonthlyTrendItem() {
    }

    public MonthlyTrendItem(String monthLabel, int year, int month, BigDecimal grossAmount, BigDecimal netAmount) {
        this.monthLabel = monthLabel;
        this.year = year;
        this.month = month;
        this.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO;
        this.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }
}
