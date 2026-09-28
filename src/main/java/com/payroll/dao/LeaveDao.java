package com.payroll.dao;

import com.payroll.model.LeaveRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LeaveDao {
    Optional<LeaveRequest> findById(Long id);
    List<LeaveRequest> findAll();
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findApprovedLeavesInMonth(Long employeeId, int month, int year);
    LeaveRequest save(LeaveRequest leave);
    boolean updateStatus(Long id, LeaveRequest.LeaveStatus status, Long reviewedBy, String comments);
    int countPendingLeaves();
    BigDecimal getUnpaidLeaveDaysInMonth(Long employeeId, int month, int year);
}
