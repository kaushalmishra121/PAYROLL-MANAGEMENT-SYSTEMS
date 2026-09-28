package com.payroll.service;

import com.payroll.model.Employee;
import com.payroll.model.EmployeeSalaryStructure;
import com.payroll.model.PayrollRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PayrollCalculationEngineTest {

    @Test
    @DisplayName("Standard full-month calculation: basic, HRA, allowances, PF, Tax, and Net Pay")
    void testStandardFullMonthCalculation() {
        Employee emp = new Employee();
        emp.setId(1L);
        emp.setEmployeeCode("EMP1001");
        emp.setFirstName("Alexander");
        emp.setLastName("Wright");
        emp.setJoiningDate(LocalDate.of(2023, 1, 1));

        // Basic: 60000, HRA: 24000, Special: 12000, Conv: 1600, Med: 1250 -> Gross = 98850.00
        // Deductions: PF (12% of 60000) = 7200.00, Prof Tax = 200.00, TDS = 5000.00 -> Total Deductions = 12400.00
        // Net Pay = 98850.00 - 12400.00 = 86450.00
        EmployeeSalaryStructure struct = new EmployeeSalaryStructure(
                1L,
                new BigDecimal("60000.00"),
                new BigDecimal("24000.00"),
                new BigDecimal("12000.00"),
                new BigDecimal("1600.00"),
                new BigDecimal("1250.00"),
                new BigDecimal("12.00"),
                new BigDecimal("200.00"),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                LocalDate.of(2023, 1, 1)
        );

        PayrollCalculationEngine.CalculationInput input = new PayrollCalculationEngine.CalculationInput();
        input.employee = emp;
        input.structure = struct;
        input.month = 4; // April (30 days)
        input.year = 2026;
        input.unpaidLeaveDays = BigDecimal.ZERO;
        input.overtimeOrBonus = BigDecimal.ZERO;

        PayrollRecord rec = PayrollCalculationEngine.calculateEmployeePayroll(input);

        assertEquals(new BigDecimal("98850.00"), rec.getGrossEarnings());
        assertEquals(new BigDecimal("7200.00"), rec.getPfDeduction());
        assertEquals(new BigDecimal("200.00"), rec.getProfessionalTax());
        assertEquals(new BigDecimal("5000.00"), rec.getTdsDeduction());
        assertEquals(new BigDecimal("0.00"), rec.getUnpaidLeaveDeduction());
        assertEquals(new BigDecimal("12400.00"), rec.getTotalDeductions());
        assertEquals(new BigDecimal("86450.00"), rec.getNetSalary());
        assertEquals(new BigDecimal("30.00"), rec.getPayableDays());
    }

    @Test
    @DisplayName("Unpaid leaves calculation: correctly computes loss of pay deduction")
    void testUnpaidLeaveCalculation() {
        Employee emp = new Employee();
        emp.setId(2L);
        emp.setEmployeeCode("EMP1002");
        emp.setFirstName("Sophia");
        emp.setLastName("Chen");
        emp.setJoiningDate(LocalDate.of(2023, 1, 1));

        // Basic: 60000, Gross: 60000 (other allowances 0)
        // 2 days unpaid in April (30 days)
        // Daily rate = 60000 / 30 = 2000.00. Unpaid deduction = 4000.00
        // PF (12% of 60000) = 7200.00, Prof Tax = 200.00
        // Total Deductions = 7200 + 200 + 4000 = 11400.00
        // Net Pay = 60000 - 11400 = 48600.00
        EmployeeSalaryStructure struct = new EmployeeSalaryStructure(
                2L,
                new BigDecimal("60000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("12.00"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2023, 1, 1)
        );

        PayrollCalculationEngine.CalculationInput input = new PayrollCalculationEngine.CalculationInput();
        input.employee = emp;
        input.structure = struct;
        input.month = 4; // April (30 days)
        input.year = 2026;
        input.unpaidLeaveDays = new BigDecimal("2.00");
        input.overtimeOrBonus = BigDecimal.ZERO;

        PayrollRecord rec = PayrollCalculationEngine.calculateEmployeePayroll(input);

        assertEquals(new BigDecimal("60000.00"), rec.getGrossEarnings());
        assertEquals(new BigDecimal("4000.00"), rec.getUnpaidLeaveDeduction());
        assertEquals(new BigDecimal("11400.00"), rec.getTotalDeductions());
        assertEquals(new BigDecimal("48600.00"), rec.getNetSalary());
        assertEquals(new BigDecimal("28.00"), rec.getPayableDays());
    }

    @Test
    @DisplayName("Mid-month joining proration: calculates partial month pay accurately")
    void testMidMonthJoiningProration() {
        Employee emp = new Employee();
        emp.setId(3L);
        emp.setEmployeeCode("EMP1003");
        emp.setFirstName("Marcus");
        emp.setLastName("Johnson");
        // Joined on April 16 in a 30-day month -> 15 active days out of 30 (50% proration)
        emp.setJoiningDate(LocalDate.of(2026, 4, 16));

        EmployeeSalaryStructure struct = new EmployeeSalaryStructure(
                3L,
                new BigDecimal("60000.00"),
                new BigDecimal("20000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("12.00"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 4, 16)
        );

        PayrollCalculationEngine.CalculationInput input = new PayrollCalculationEngine.CalculationInput();
        input.employee = emp;
        input.structure = struct;
        input.month = 4;
        input.year = 2026;
        input.unpaidLeaveDays = BigDecimal.ZERO;
        input.overtimeOrBonus = BigDecimal.ZERO;

        PayrollRecord rec = PayrollCalculationEngine.calculateEmployeePayroll(input);

        // 50% of 60000 basic = 30000.00, 50% of 20000 HRA = 10000.00 -> Gross = 40000.00
        assertEquals(new BigDecimal("40000.00"), rec.getGrossEarnings());
        // PF = 12% of 30000.00 = 3600.00 + 200 prof tax = 3800.00
        assertEquals(new BigDecimal("3600.00"), rec.getPfDeduction());
        assertEquals(new BigDecimal("3800.00"), rec.getTotalDeductions());
        assertEquals(new BigDecimal("36200.00"), rec.getNetSalary());
        assertEquals(new BigDecimal("15.00"), rec.getPayableDays());
    }
}
