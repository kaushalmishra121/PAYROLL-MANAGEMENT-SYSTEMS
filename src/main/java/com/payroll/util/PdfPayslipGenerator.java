package com.payroll.util;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.payroll.model.Employee;
import com.payroll.model.PayrollRecord;
import com.payroll.model.PayrollRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PdfPayslipGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PdfPayslipGenerator.class);

    private static final Color PRIMARY_NAVY = new Color(26, 42, 74);
    private static final Color SECONDARY_BLUE = new Color(37, 99, 235);
    private static final Color ACCENT_BG = new Color(241, 245, 249);
    private static final Color LIGHT_BORDER = new Color(226, 232, 240);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_NAVY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MUTED);
    private static final Font FONT_HEADER_WHITE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_NAVY);
    private static final Font FONT_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
    private static final Font FONT_NET_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_NAVY);
    private static final Font FONT_NET_AMOUNT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, SECONDARY_BLUE);
    private static final Font FONT_DISCLAIMER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, TEXT_MUTED);

    public static void generatePayslipPdf(PayrollRun run, PayrollRecord record, Employee employee, File destination) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(destination));
        document.open();

        try {
            // Header table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{65, 35});

            PdfPCell compCell = new PdfPCell();
            compCell.setBorder(Rectangle.NO_BORDER);
            Paragraph compName = new Paragraph("ENTERPRISE CORPORATION", FONT_TITLE);
            Paragraph compAddr = new Paragraph("Enterprise Business Park, Suite 500\nGlobal Financial District | payroll@enterprise.com", FONT_SUBTITLE);
            compCell.addElement(compName);
            compCell.addElement(compAddr);
            headerTable.addCell(compCell);

            PdfPCell paySlipCell = new PdfPCell();
            paySlipCell.setBorder(Rectangle.NO_BORDER);
            paySlipCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph psTitle = new Paragraph("PAYSLIP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, SECONDARY_BLUE));
            psTitle.setAlignment(Element.ALIGN_RIGHT);
            Paragraph psPeriod = new Paragraph("Period: " + run.getPeriodDisplay() + "\nDate: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), FONT_SUBTITLE);
            psPeriod.setAlignment(Element.ALIGN_RIGHT);
            paySlipCell.addElement(psTitle);
            paySlipCell.addElement(psPeriod);
            headerTable.addCell(paySlipCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // Employee Information Box
            PdfPTable empTable = new PdfPTable(4);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{22, 28, 22, 28});

            addEmpInfoRow(empTable, "Employee Code:", record.getEmployeeCode(), "Employee Name:", record.getEmployeeName());
            addEmpInfoRow(empTable, "Department:", record.getDepartmentName(), "Designation:", record.getDesignationTitle());
            addEmpInfoRow(empTable, "Date of Joining:", employee != null && employee.getJoiningDate() != null ? employee.getJoiningDate().toString() : "-", "Payment Mode:", record.getPaymentMethod());
            addEmpInfoRow(empTable, "Bank Name:", record.getBankName() != null ? record.getBankName() : "-", "Account No:", record.getAccountNumberMasked() != null ? record.getAccountNumberMasked() : "-");
            addEmpInfoRow(empTable, "Working Days:", String.valueOf(record.getTotalWorkingDays()), "Payable Days:", String.valueOf(record.getPayableDays()));

            document.add(empTable);
            document.add(new Paragraph(" "));

            // Salary Breakdown: Two Columns (Earnings | Deductions)
            PdfPTable salaryTable = new PdfPTable(4);
            salaryTable.setWidthPercentage(100);
            salaryTable.setWidths(new float[]{32, 18, 32, 18});

            // Headers
            PdfPCell earnHead = new PdfPCell(new Phrase("EARNINGS", FONT_HEADER_WHITE));
            earnHead.setBackgroundColor(PRIMARY_NAVY);
            earnHead.setColspan(2);
            earnHead.setPadding(6);
            salaryTable.addCell(earnHead);

            PdfPCell dedHead = new PdfPCell(new Phrase("DEDUCTIONS", FONT_HEADER_WHITE));
            dedHead.setBackgroundColor(PRIMARY_NAVY);
            dedHead.setColspan(2);
            dedHead.setPadding(6);
            salaryTable.addCell(dedHead);

            // Row 1
            addSalaryRow(salaryTable, "Basic Salary", CurrencyUtils.format(record.getBasicSalary()), "Provident Fund (PF)", CurrencyUtils.format(record.getPfDeduction()));
            // Row 2
            addSalaryRow(salaryTable, "House Rent Allowance (HRA)", CurrencyUtils.format(record.getHra()), "Professional Tax", CurrencyUtils.format(record.getProfessionalTax()));
            // Row 3
            addSalaryRow(salaryTable, "Special Allowance", CurrencyUtils.format(record.getSpecialAllowance()), "TDS / Income Tax", CurrencyUtils.format(record.getTdsDeduction()));
            // Row 4
            addSalaryRow(salaryTable, "Conveyance Allowance", CurrencyUtils.format(record.getConveyanceAllowance()), "Unpaid Leave Loss", CurrencyUtils.format(record.getUnpaidLeaveDeduction()));
            // Row 5
            addSalaryRow(salaryTable, "Medical Allowance", CurrencyUtils.format(record.getMedicalAllowance()), "Other Deductions", CurrencyUtils.format(record.getOtherDeductions()));
            // Row 6
            addSalaryRow(salaryTable, "Bonus / Incentives", CurrencyUtils.format(record.getOvertimeOrBonus()), "-", "$0.00");

            // Subtotals
            PdfPCell grossLabel = new PdfPCell(new Phrase("Gross Earnings", FONT_BOLD));
            grossLabel.setBackgroundColor(ACCENT_BG);
            grossLabel.setPadding(6);
            salaryTable.addCell(grossLabel);

            PdfPCell grossVal = new PdfPCell(new Phrase(CurrencyUtils.format(record.getGrossEarnings()), FONT_BOLD));
            grossVal.setBackgroundColor(ACCENT_BG);
            grossVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            grossVal.setPadding(6);
            salaryTable.addCell(grossVal);

            PdfPCell dedLabel = new PdfPCell(new Phrase("Total Deductions", FONT_BOLD));
            dedLabel.setBackgroundColor(ACCENT_BG);
            dedLabel.setPadding(6);
            salaryTable.addCell(dedLabel);

            PdfPCell dedVal = new PdfPCell(new Phrase(CurrencyUtils.format(record.getTotalDeductions()), FONT_BOLD));
            dedVal.setBackgroundColor(ACCENT_BG);
            dedVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dedVal.setPadding(6);
            salaryTable.addCell(dedVal);

            document.add(salaryTable);
            document.add(new Paragraph(" "));

            // Net Pay Banner Card
            PdfPTable netTable = new PdfPTable(2);
            netTable.setWidthPercentage(100);
            netTable.setWidths(new float[]{60, 40});

            PdfPCell netLeft = new PdfPCell();
            netLeft.setBackgroundColor(ACCENT_BG);
            netLeft.setPadding(10);
            netLeft.setBorderColor(LIGHT_BORDER);
            Paragraph pNet = new Paragraph("NET PAYABLE SALARY", FONT_NET_LABEL);
            Paragraph pSub = new Paragraph("(Gross Earnings minus Total Deductions)", FONT_SUBTITLE);
            netLeft.addElement(pNet);
            netLeft.addElement(pSub);
            netTable.addCell(netLeft);

            PdfPCell netRight = new PdfPCell();
            netRight.setBackgroundColor(ACCENT_BG);
            netRight.setPadding(10);
            netRight.setBorderColor(LIGHT_BORDER);
            netRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph pAmount = new Paragraph(CurrencyUtils.format(record.getNetSalary()), FONT_NET_AMOUNT);
            pAmount.setAlignment(Element.ALIGN_RIGHT);
            netRight.addElement(pAmount);
            netTable.addCell(netRight);

            document.add(netTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Signatures
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50, 50});

            PdfPCell signLeft = new PdfPCell(new Paragraph("\n\n____________________________________\nEmployee Signature", FONT_REGULAR));
            signLeft.setBorder(Rectangle.NO_BORDER);
            signTable.addCell(signLeft);

            PdfPCell signRight = new PdfPCell(new Paragraph("\n\n____________________________________\nAuthorized Signatory (HR / Finance)", FONT_REGULAR));
            signRight.setBorder(Rectangle.NO_BORDER);
            signRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signTable.addCell(signRight);

            document.add(signTable);
            document.add(new Paragraph(" "));

            // Legal Disclaimer
            Paragraph disc = new Paragraph(
                    "DISCLAIMER: This is a system-generated payslip. The tax and statutory deductions calculated herein are based on configurable employer parameters. Statutory compliance should be verified against relevant regional regulatory requirements.",
                    FONT_DISCLAIMER
            );
            disc.setAlignment(Element.ALIGN_CENTER);
            document.add(disc);

            logger.info("PDF Payslip generated successfully for {}: {}", record.getEmployeeCode(), destination.getAbsolutePath());
        } finally {
            document.close();
        }
    }

    private static void addEmpInfoRow(PdfPTable table, String label1, String val1, String label2, String val2) {
        PdfPCell c1 = new PdfPCell(new Phrase(label1, FONT_BOLD));
        c1.setBackgroundColor(ACCENT_BG);
        c1.setBorderColor(LIGHT_BORDER);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val1 != null ? val1 : "-", FONT_REGULAR));
        c2.setBorderColor(LIGHT_BORDER);
        c2.setPadding(4);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(label2, FONT_BOLD));
        c3.setBackgroundColor(ACCENT_BG);
        c3.setBorderColor(LIGHT_BORDER);
        c3.setPadding(4);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(val2 != null ? val2 : "-", FONT_REGULAR));
        c4.setBorderColor(LIGHT_BORDER);
        c4.setPadding(4);
        table.addCell(c4);
    }

    private static void addSalaryRow(PdfPTable table, String earnLabel, String earnVal, String dedLabel, String dedVal) {
        PdfPCell c1 = new PdfPCell(new Phrase(earnLabel, FONT_REGULAR));
        c1.setBorderColor(LIGHT_BORDER);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(earnVal, FONT_REGULAR));
        c2.setBorderColor(LIGHT_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(4);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(dedLabel, FONT_REGULAR));
        c3.setBorderColor(LIGHT_BORDER);
        c3.setPadding(4);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(dedVal, FONT_REGULAR));
        c4.setBorderColor(LIGHT_BORDER);
        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c4.setPadding(4);
        table.addCell(c4);
    }
}
