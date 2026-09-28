package com.payroll.dao;

import com.payroll.model.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceDao {
    Optional<Attendance> findByEmployeeAndDate(Long employeeId, LocalDate date);
    List<Attendance> findByMonthAndYear(int month, int year);
    List<Attendance> findByEmployeeAndMonth(Long employeeId, int month, int year);
    Attendance save(Attendance attendance);
    boolean update(Attendance attendance);
    boolean delete(Long id);
    int countStatusForEmployeeInMonth(Long employeeId, Attendance.AttendanceStatus status, int month, int year);
}
