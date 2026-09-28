package com.payroll.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateUtils {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    public static String getMonthName(int month) {
        if (month >= 1 && month <= 12) {
            return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
        return String.valueOf(month);
    }

    public static String getMonthShortName(int month) {
        if (month >= 1 && month <= 12) {
            return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        }
        return String.valueOf(month);
    }

    public static int getDaysInMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return ym.lengthOfMonth();
    }
}
