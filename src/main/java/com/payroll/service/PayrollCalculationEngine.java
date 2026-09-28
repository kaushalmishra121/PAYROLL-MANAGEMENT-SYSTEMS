package com.payroll.service;

import com.payroll.model.Employee;
import com.payroll.model.EmployeeSalaryStructure;
import com.payroll.model.PayrollRecord;
import com.payroll.util.DateUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Pure, deterministic mathematical calculation engine for payroll computations.
 * Uses BigDecimal for all monetary calculations with explicit HALF_UP rounding.
 */
public class PayrollCalculationEngine {

    public static class CalculationInput {
        public Employee employee;
        public EmployeeSalaryStructure structure;
        public int month;
        public int year;
        public BigDecimal unpaidLeaveDays = BigDecimal.ZERO;
        public BigDecimal overtimeOrBonus = BigDecimal.ZERO;
    }

    public static PayrollRecord calculateEmployeePayroll(CalculationInput input) {
        if (input == null || input.employee == null || input.structure == null) {
            throw new IllegalArgumentException("Calculation input and structures cannot be null.");
        }

        Employee emp = input.employee;
        EmployeeSalaryStructure struct = input.structure;
        int month = input.month;
        int year = input.year;

        int totalMonthDays = DateUtils.getDaysInMonth(year, month);
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = LocalDate.of(year, month, totalMonthDays);

        // Calculate proration for mid-month joining
        int activeDaysInMonth = totalMonthDays;
        if (emp.getJoiningDate() != null && emp.getJoiningDate().isAfter(monthStart)) {
            if (emp.getJoiningDate().isAfter(monthEnd)) {
                activeDaysInMonth = 0; // Joined in a future month
            } else {
                activeDaysInMonth = totalMonthDays - emp.getJoiningDate().getDayOfMonth() + 1;
            }
        }

        BigDecimal prorationFactor = BigDecimal.ONE;
        if (activeDaysInMonth < totalMonthDays) {
            prorationFactor = BigDecimal.valueOf(activeDaysInMonth)
                    .divide(BigDecimal.valueOf(totalMonthDays), 6, RoundingMode.HALF_UP);
        }

        // Unpaid leave days
        BigDecimal unpaidDays = input.unpaidLeaveDays != null ? input.unpaidLeaveDays : BigDecimal.ZERO;
        if (unpaidDays.compareTo(BigDecimal.valueOf(activeDaysInMonth)) > 0) {
            unpaidDays = BigDecimal.valueOf(activeDaysInMonth);
        }

        BigDecimal payableDays = BigDecimal.valueOf(activeDaysInMonth).subtract(unpaidDays)
                .setScale(2, RoundingMode.HALF_UP);

        // Prorated Base Earnings
        BigDecimal baseBasic = struct.getBasicSalary().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseHra = struct.getHra().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseSpecial = struct.getSpecialAllowance().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseConv = struct.getConveyanceAllowance().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseMed = struct.getMedicalAllowance().multiply(prorationFactor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bonus = input.overtimeOrBonus != null ? input.overtimeOrBonus.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal grossEarnings = baseBasic.add(baseHra).add(baseSpecial).add(baseConv).add(baseMed).add(bonus)
                .setScale(2, RoundingMode.HALF_UP);

        // Unpaid leave deduction (Loss of Pay)
        BigDecimal unpaidLeaveDeduction = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (unpaidDays.compareTo(BigDecimal.ZERO) > 0 && totalMonthDays > 0) {
            BigDecimal dailyRate = grossEarnings.divide(BigDecimal.valueOf(totalMonthDays), 4, RoundingMode.HALF_UP);
            unpaidLeaveDeduction = dailyRate.multiply(unpaidDays).setScale(2, RoundingMode.HALF_UP);
        }

        // Statutory & Configured Deductions
        BigDecimal pfRate = struct.getPfRatePct() != null ? struct.getPfRatePct() : BigDecimal.ZERO;
        BigDecimal pfDeduction = baseBasic.multiply(pfRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal profTax = struct.getProfessionalTax() != null ? struct.getProfessionalTax() : BigDecimal.ZERO;
        BigDecimal tds = struct.getTdsMonthly() != null ? struct.getTdsMonthly() : BigDecimal.ZERO;
        BigDecimal otherDeduct = struct.getOtherDeductions() != null ? struct.getOtherDeductions() : BigDecimal.ZERO;

        BigDecimal totalDeductions = pfDeduction.add(profTax).add(tds).add(unpaidLeaveDeduction).add(otherDeduct)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSalary = grossEarnings.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            netSalary = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Build itemized snapshot record
        PayrollRecord rec = new PayrollRecord();
        rec.setEmployeeId(emp.getId());
        rec.setEmployeeCode(emp.getEmployeeCode());
        rec.setEmployeeName(emp.getFullName());
        rec.setDepartmentName(emp.getDepartmentName() != null ? emp.getDepartmentName() : "General");
        rec.setDesignationTitle(emp.getDesignationTitle() != null ? emp.getDesignationTitle() : "Staff");
        rec.setTotalWorkingDays(totalMonthDays);
        rec.setPayableDays(payableDays);
        rec.setUnpaidLeaveDays(unpaidDays);

        rec.setBasicSalary(baseBasic);
        rec.setHra(baseHra);
        rec.setSpecialAllowance(baseSpecial);
        rec.setConveyanceAllowance(baseConv);
        rec.setMedicalAllowance(baseMed);
        rec.setOvertimeOrBonus(bonus);
        rec.setGrossEarnings(grossEarnings);

        rec.setPfDeduction(pfDeduction);
        rec.setProfessionalTax(profTax);
        rec.setTdsDeduction(tds);
        rec.setUnpaidLeaveDeduction(unpaidLeaveDeduction);
        rec.setOtherDeductions(otherDeduct);
        rec.setTotalDeductions(totalDeductions);

        rec.setNetSalary(netSalary);
        rec.setPaymentStatus(PayrollRecord.PaymentStatus.PENDING);
        rec.setPaymentMethod("BANK_TRANSFER");
        rec.setBankName(emp.getBankName());
        rec.setAccountNumberMasked(emp.getMaskedAccountNumber());

        return rec;
    }
}
