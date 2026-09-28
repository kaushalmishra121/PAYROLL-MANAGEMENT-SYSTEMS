package com.payroll.service;

import com.payroll.model.Employee;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import com.payroll.util.CsvExporter;
import com.payroll.util.PdfPayslipGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("CSV export should generate valid formatted CSV file")
    void testCsvExport(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("test_employees.csv").toFile();
        Employee emp = new Employee(1L, "EMP1001", "Alexander", "Wright", "alex@enterprise.com", "+1 555-0192", 1L, 1L, LocalDate.of(2023, 1, 1), Employee.EmploymentStatus.ACTIVE);
        emp.setDepartmentName("Engineering");
        emp.setDesignationTitle("Lead Architect");
        emp.setBasicSalary(new BigDecimal("80000.00"));
        emp.setGrossSalary(new BigDecimal("134850.00"));

        CsvExporter.exportEmployees(List.of(emp), csvFile);

        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("EMP1001"));
        assertTrue(content.contains("Alexander Wright"));
    }

    @Test
    @DisplayName("OpenPDF payslip generation should produce non-empty PDF document")
    void testPdfPayslipGeneration(@TempDir Path tempDir) throws Exception {
        File pdfFile = tempDir.resolve("test_payslip.pdf").toFile();

        PayrollRun run = new PayrollRun(4, 2026, 1L);
        run.setId(1L);

        PayrollRecord rec = new PayrollRecord();
        rec.setEmployeeCode("EMP1001");
        rec.setEmployeeName("Alexander Wright");
        rec.setDepartmentName("Engineering");
        rec.setDesignationTitle("Lead Architect");
        rec.setBasicSalary(new BigDecimal("80000.00"));
        rec.setHra(new BigDecimal("32000.00"));
        rec.setSpecialAllowance(new BigDecimal("20000.00"));
        rec.setConveyanceAllowance(new BigDecimal("1600.00"));
        rec.setMedicalAllowance(new BigDecimal("1250.00"));
        rec.setGrossEarnings(new BigDecimal("134850.00"));
        rec.setPfDeduction(new BigDecimal("9600.00"));
        rec.setProfessionalTax(new BigDecimal("200.00"));
        rec.setTdsDeduction(new BigDecimal("12500.00"));
        rec.setTotalDeductions(new BigDecimal("22300.00"));
        rec.setNetSalary(new BigDecimal("112550.00"));
        rec.setTotalWorkingDays(30);
        rec.setPayableDays(new BigDecimal("30.00"));

        Employee emp = new Employee();
        emp.setJoiningDate(LocalDate.of(2023, 1, 15));

        PdfPayslipGenerator.generatePayslipPdf(run, rec, emp, pdfFile);

        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 500); // Standard PDF header + contents
    }
}
