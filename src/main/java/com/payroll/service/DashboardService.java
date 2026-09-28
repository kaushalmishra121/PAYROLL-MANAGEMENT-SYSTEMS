package com.payroll.service;

import com.payroll.dao.DepartmentDao;
import com.payroll.dao.DepartmentDaoImpl;
import com.payroll.dao.EmployeeDao;
import com.payroll.dao.EmployeeDaoImpl;
import com.payroll.dao.PayrollDao;
import com.payroll.dao.PayrollDaoImpl;
import com.payroll.model.DashboardSummary;
import com.payroll.model.MonthlyTrendItem;
import com.payroll.model.PayrollRun;

import java.time.LocalDate;
import java.util.List;

public class DashboardService {
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;
    private final PayrollDao payrollDao;

    public DashboardService() {
        this.employeeDao = new EmployeeDaoImpl();
        this.departmentDao = new DepartmentDaoImpl();
        this.payrollDao = new PayrollDaoImpl();
    }

    public DashboardService(EmployeeDao employeeDao, DepartmentDao departmentDao, PayrollDao payrollDao) {
        this.employeeDao = employeeDao;
        this.departmentDao = departmentDao;
        this.payrollDao = payrollDao;
    }

    public DashboardSummary getDashboardSummary() {
        DashboardSummary summary = new DashboardSummary();
        
        // 1. Employee metrics
        summary.setTotalEmployees(employeeDao.countTotal());
        summary.setActiveEmployees(employeeDao.countActive());
        summary.setInactiveEmployees(employeeDao.countInactive());

        // 2. Department metrics
        summary.setTotalDepartments(departmentDao.countTotalDepartments());

        // 3. Payroll KPIs
        LocalDate now = LocalDate.now();
        summary.setCurrentMonthPayroll(payrollDao.getCurrentMonthPayrollTotal(now.getMonthValue(), now.getYear()));
        summary.setPendingApprovals(payrollDao.countPendingApprovals());
        summary.setTotalPaidPayroll(payrollDao.getTotalPaidPayroll());
        summary.setTotalUnpaidPayroll(payrollDao.getTotalUnpaidPayroll());

        // 4. Charts & Trends
        List<MonthlyTrendItem> trends = payrollDao.getMonthlyPayrollTrends(6);
        summary.setMonthlyTrends(trends);

        summary.setDepartmentDistribution(employeeDao.getDepartmentHeadcounts());

        // 5. Recent Payroll Runs
        List<PayrollRun> recentRuns = payrollDao.findAllRuns();
        if (recentRuns.size() > 5) {
            recentRuns = recentRuns.subList(0, 5);
        }
        summary.setRecentPayrollRuns(recentRuns);

        return summary;
    }
}
