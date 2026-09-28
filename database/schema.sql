-- =====================================================================
-- Enterprise Payroll Management System - Complete Database Schema (MySQL)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `payroll_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `payroll_db`;

-- Drop existing tables in reverse dependency order for clean re-creation
DROP TABLE IF EXISTS `audit_logs`;
DROP TABLE IF EXISTS `payroll_adjustments`;
DROP TABLE IF EXISTS `payroll_records`;
DROP TABLE IF EXISTS `payroll_runs`;
DROP TABLE IF EXISTS `leave_requests`;
DROP TABLE IF EXISTS `attendance`;
DROP TABLE IF EXISTS `employee_salary_structures`;
DROP TABLE IF EXISTS `salary_components`;
DROP TABLE IF EXISTS `employees`;
DROP TABLE IF EXISTS `designations`;
DROP TABLE IF EXISTS `departments`;
DROP TABLE IF EXISTS `users`;

-- ---------------------------------------------------------------------
-- 1. USERS & AUTHENTICATION TABLE
-- ---------------------------------------------------------------------
CREATE TABLE `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `role` ENUM('ADMIN', 'HR') NOT NULL DEFAULT 'HR',
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_username` (`username`),
    INDEX `idx_users_email` (`email`),
    INDEX `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. DEPARTMENTS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE `departments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `code` VARCHAR(20) NOT NULL UNIQUE,
    `description` VARCHAR(255) NULL,
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_dept_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. DESIGNATIONS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE `designations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `department_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `code` VARCHAR(20) NOT NULL,
    `min_salary` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `max_salary` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_desig_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON UPDATE CASCADE,
    UNIQUE KEY `uk_dept_desig_code` (`department_id`, `code`),
    INDEX `idx_desig_dept` (`department_id`),
    INDEX `idx_desig_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 4. EMPLOYEES TABLE
-- ---------------------------------------------------------------------
CREATE TABLE `employees` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_code` VARCHAR(30) NOT NULL UNIQUE,
    `first_name` VARCHAR(50) NOT NULL,
    `last_name` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `phone` VARCHAR(20) NOT NULL,
    `address` TEXT NULL,
    `department_id` BIGINT NOT NULL,
    `designation_id` BIGINT NOT NULL,
    `joining_date` DATE NOT NULL,
    `employment_status` ENUM('ACTIVE', 'PROBATION', 'SUSPENDED', 'RESIGNED', 'TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    `bank_name` VARCHAR(100) NULL,
    `account_number` VARCHAR(50) NULL,
    `ifsc_or_routing` VARCHAR(30) NULL,
    `pan_or_tax_id` VARCHAR(30) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_emp_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON UPDATE CASCADE,
    CONSTRAINT `fk_emp_desig` FOREIGN KEY (`designation_id`) REFERENCES `designations` (`id`) ON UPDATE CASCADE,
    INDEX `idx_emp_code` (`employee_code`),
    INDEX `idx_emp_email` (`email`),
    INDEX `idx_emp_dept` (`department_id`),
    INDEX `idx_emp_desig` (`designation_id`),
    INDEX `idx_emp_status` (`employment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 5. SALARY COMPONENTS MASTER (Configurable Allowances & Deductions)
-- ---------------------------------------------------------------------
CREATE TABLE `salary_components` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `code` VARCHAR(30) NOT NULL UNIQUE,
    `type` ENUM('EARNING', 'DEDUCTION') NOT NULL,
    `calculation_type` ENUM('FIXED_AMOUNT', 'PERCENTAGE_OF_BASIC', 'PERCENTAGE_OF_GROSS') NOT NULL,
    `default_rate` DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    `is_taxable` BOOLEAN NOT NULL DEFAULT TRUE,
    `is_mandatory` BOOLEAN NOT NULL DEFAULT FALSE,
    `description` VARCHAR(255) NULL,
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 6. EMPLOYEE SALARY STRUCTURE (Configurable structure per employee)
-- ---------------------------------------------------------------------
CREATE TABLE `employee_salary_structures` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` BIGINT NOT NULL UNIQUE,
    `basic_salary` DECIMAL(12,2) NOT NULL,
    `hra` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `special_allowance` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `conveyance_allowance` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `medical_allowance` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `pf_rate_pct` DECIMAL(5,2) NOT NULL DEFAULT 12.00,
    `professional_tax` DECIMAL(12,2) NOT NULL DEFAULT 200.00,
    `tds_monthly` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `other_deductions` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `effective_date` DATE NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sal_emp` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 7. ATTENDANCE TABLE
-- ---------------------------------------------------------------------
CREATE TABLE `attendance` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` BIGINT NOT NULL,
    `attendance_date` DATE NOT NULL,
    `status` ENUM('PRESENT', 'ABSENT', 'HALF_DAY', 'ON_LEAVE', 'HOLIDAY', 'WEEKOFF') NOT NULL DEFAULT 'PRESENT',
    `check_in_time` TIME NULL,
    `check_out_time` TIME NULL,
    `notes` VARCHAR(255) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_att_emp` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_emp_attendance_date` (`employee_id`, `attendance_date`),
    INDEX `idx_att_date` (`attendance_date`),
    INDEX `idx_att_emp_date` (`employee_id`, `attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 8. LEAVE REQUESTS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE `leave_requests` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` BIGINT NOT NULL,
    `leave_type` ENUM('CASUAL', 'SICK', 'EARNED', 'UNPAID', 'MATERNITY') NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `total_days` DECIMAL(4,1) NOT NULL,
    `reason` TEXT NOT NULL,
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    `reviewed_by` BIGINT NULL,
    `reviewed_at` TIMESTAMP NULL,
    `comments` VARCHAR(255) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_leave_emp` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_leave_user` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_leave_emp` (`employee_id`),
    INDEX `idx_leave_status` (`status`),
    INDEX `idx_leave_dates` (`start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 9. PAYROLL RUNS (Monthly Execution Header)
-- ---------------------------------------------------------------------
CREATE TABLE `payroll_runs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `payroll_month` INT NOT NULL,
    `payroll_year` INT NOT NULL,
    `run_date` DATE NOT NULL,
    `total_employees` INT NOT NULL DEFAULT 0,
    `total_gross_pay` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `total_deductions` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `total_net_pay` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `status` ENUM('DRAFT', 'APPROVED', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    `processed_by` BIGINT NOT NULL,
    `approved_by` BIGINT NULL,
    `approved_at` TIMESTAMP NULL,
    `payment_date` DATE NULL,
    `notes` TEXT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_run_proc_user` FOREIGN KEY (`processed_by`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_run_appr_user` FOREIGN KEY (`approved_by`) REFERENCES `users` (`id`),
    UNIQUE KEY `uk_payroll_period` (`payroll_year`, `payroll_month`),
    INDEX `idx_payroll_period` (`payroll_year`, `payroll_month`),
    INDEX `idx_payroll_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 10. PAYROLL RECORDS (Individual Employee Snapshot for each Run)
-- ---------------------------------------------------------------------
CREATE TABLE `payroll_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `payroll_run_id` BIGINT NOT NULL,
    `employee_id` BIGINT NOT NULL,
    `employee_code` VARCHAR(30) NOT NULL,
    `employee_name` VARCHAR(100) NOT NULL,
    `department_name` VARCHAR(100) NOT NULL,
    `designation_title` VARCHAR(100) NOT NULL,
    `total_working_days` INT NOT NULL DEFAULT 30,
    `payable_days` DECIMAL(5,2) NOT NULL DEFAULT 30.00,
    `unpaid_leave_days` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    
    -- Earnings
    `basic_salary` DECIMAL(12,2) NOT NULL,
    `hra` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `special_allowance` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `conveyance_allowance` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `medical_allowance` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `overtime_or_bonus` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `gross_earnings` DECIMAL(12,2) NOT NULL,
    
    -- Deductions
    `pf_deduction` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `professional_tax` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `tds_deduction` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `unpaid_leave_deduction` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `other_deductions` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `total_deductions` DECIMAL(12,2) NOT NULL,
    
    -- Net Pay
    `net_salary` DECIMAL(12,2) NOT NULL,
    
    -- Payment Details Snapshot
    `payment_status` ENUM('PENDING', 'PROCESSED', 'PAID', 'HELD') NOT NULL DEFAULT 'PENDING',
    `payment_method` VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER',
    `bank_name` VARCHAR(100) NULL,
    `account_number_masked` VARCHAR(50) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT `fk_rec_run` FOREIGN KEY (`payroll_run_id`) REFERENCES `payroll_runs` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rec_emp` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`),
    UNIQUE KEY `uk_run_employee` (`payroll_run_id`, `employee_id`),
    INDEX `idx_rec_run` (`payroll_run_id`),
    INDEX `idx_rec_emp` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 11. PAYROLL ADJUSTMENTS (Auditable Corrections)
-- ---------------------------------------------------------------------
CREATE TABLE `payroll_adjustments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `payroll_record_id` BIGINT NOT NULL,
    `adjustment_type` ENUM('ADDITION', 'DEDUCTION') NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `reason` VARCHAR(255) NOT NULL,
    `authorized_by` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_adj_rec` FOREIGN KEY (`payroll_record_id`) REFERENCES `payroll_records` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_adj_user` FOREIGN KEY (`authorized_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 12. AUDIT LOGS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NULL,
    `action` VARCHAR(50) NOT NULL,
    `entity_type` VARCHAR(50) NOT NULL,
    `entity_id` BIGINT NULL,
    `details` TEXT NOT NULL,
    `ip_address` VARCHAR(45) DEFAULT '127.0.0.1',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_audit_action` (`action`),
    INDEX `idx_audit_entity` (`entity_type`, `entity_id`),
    INDEX `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
