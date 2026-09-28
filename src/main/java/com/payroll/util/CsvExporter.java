package com.payroll.util;

import com.payroll.model.Attendance;
import com.payroll.model.Employee;
import com.payroll.model.LeaveRequest;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvExporter {
    private static final Logger logger = LoggerFactory.getLogger(CsvExporter.class);

    private static String escape(Object obj) {
        if (obj == null) {
            return "\"\"";
        }
        String str = String.valueOf(obj);
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }

    public static void exportEmployees(List<Employee> employees, File destination) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for Excel compatibility
            writer.write('\ufeff');

            // Header
            writer.write("Employee ID,Full Name,Email,Phone,Department,Designation,Joining Date,Status,Basic Salary,Gross Salary,Bank,Account (Masked),PAN/Tax ID\n");

            for (Employee emp : employees) {
                writer.write(String.join(",",
                        escape(emp.getEmployeeCode()),
                        escape(emp.getFullName()),
                        escape(emp.getEmail()),
                        escape(emp.getPhone()),
                        escape(emp.getDepartmentName()),
                        escape(emp.getDesignationTitle()),
                        escape(emp.getJoiningDate()),
                        escape(emp.getEmploymentStatus()),
                        escape(CurrencyUtils.formatPlain(emp.getBasicSalary())),
                        escape(CurrencyUtils.formatPlain(emp.getGrossSalary())),
                        escape(emp.getBankName()),
                        escape(emp.getMaskedAccountNumber()),
                        escape(emp.getPanOrTaxId())
                ));
                writer.write("\n");
            }
            logger.info("Successfully exported {} employees to CSV: {}", employees.size(), destination.getAbsolutePath());
        }
    }

    public static void exportPayrollRegister(PayrollRun run, List<PayrollRecord> records, File destination) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');

            // Title & Period Header
            writer.write(escape("Payroll Register - " + run.getPeriodDisplay()) + "\n");
            writer.write(escape("Run Date: " + run.getRunDate()) + "," + escape("Status: " + run.getStatus()) + "\n\n");

            // Columns Header
            writer.write("Emp Code,Employee Name,Department,Designation,Working Days,Payable Days,Unpaid Days,Basic,HRA,Special,Conveyance,Medical,Bonus,Gross Earnings,PF,Prof Tax,TDS,Unpaid Loss,Other Deduct,Total Deductions,Net Pay,Payment Status\n");

            for (PayrollRecord rec : records) {
                writer.write(String.join(",",
                        escape(rec.getEmployeeCode()),
                        escape(rec.getEmployeeName()),
                        escape(rec.getDepartmentName()),
                        escape(rec.getDesignationTitle()),
                        escape(rec.getTotalWorkingDays()),
                        escape(rec.getPayableDays()),
                        escape(rec.getUnpaidLeaveDays()),
                        escape(CurrencyUtils.formatPlain(rec.getBasicSalary())),
                        escape(CurrencyUtils.formatPlain(rec.getHra())),
                        escape(CurrencyUtils.formatPlain(rec.getSpecialAllowance())),
                        escape(CurrencyUtils.formatPlain(rec.getConveyanceAllowance())),
                        escape(CurrencyUtils.formatPlain(rec.getMedicalAllowance())),
                        escape(CurrencyUtils.formatPlain(rec.getOvertimeOrBonus())),
                        escape(CurrencyUtils.formatPlain(rec.getGrossEarnings())),
                        escape(CurrencyUtils.formatPlain(rec.getPfDeduction())),
                        escape(CurrencyUtils.formatPlain(rec.getProfessionalTax())),
                        escape(CurrencyUtils.formatPlain(rec.getTdsDeduction())),
                        escape(CurrencyUtils.formatPlain(rec.getUnpaidLeaveDeduction())),
                        escape(CurrencyUtils.formatPlain(rec.getOtherDeductions())),
                        escape(CurrencyUtils.formatPlain(rec.getTotalDeductions())),
                        escape(CurrencyUtils.formatPlain(rec.getNetSalary())),
                        escape(rec.getPaymentStatus())
                ));
                writer.write("\n");
            }
            logger.info("Successfully exported payroll register with {} records to: {}", records.size(), destination.getAbsolutePath());
        }
    }

    public static void exportAttendance(List<Attendance> attendances, File destination) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.write("Date,Employee Code,Employee Name,Department,Status,Check In,Check Out,Notes\n");

            for (Attendance att : attendances) {
                writer.write(String.join(",",
                        escape(att.getAttendanceDate()),
                        escape(att.getEmployeeCode()),
                        escape(att.getEmployeeName()),
                        escape(att.getDepartmentName()),
                        escape(att.getStatus()),
                        escape(att.getCheckInTime() != null ? att.getCheckInTime().toString() : "-"),
                        escape(att.getCheckOutTime() != null ? att.getCheckOutTime().toString() : "-"),
                        escape(att.getNotes() != null ? att.getNotes() : "")
                ));
                writer.write("\n");
            }
            logger.info("Successfully exported {} attendance records to: {}", attendances.size(), destination.getAbsolutePath());
        }
    }

    public static void exportLeaves(List<LeaveRequest> leaves, File destination) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.write("Employee Code,Employee Name,Department,Leave Type,Start Date,End Date,Days,Status,Reason,Reviewer,Review Notes\n");

            for (LeaveRequest leave : leaves) {
                writer.write(String.join(",",
                        escape(leave.getEmployeeCode()),
                        escape(leave.getEmployeeName()),
                        escape(leave.getDepartmentName()),
                        escape(leave.getLeaveType()),
                        escape(leave.getStartDate()),
                        escape(leave.getEndDate()),
                        escape(leave.getTotalDays()),
                        escape(leave.getStatus()),
                        escape(leave.getReason()),
                        escape(leave.getReviewerName() != null ? leave.getReviewerName() : "-"),
                        escape(leave.getComments() != null ? leave.getComments() : "")
                ));
                writer.write("\n");
            }
            logger.info("Successfully exported {} leave records to: {}", leaves.size(), destination.getAbsolutePath());
        }
    }
}
