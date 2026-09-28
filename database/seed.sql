-- =====================================================================
-- Enterprise Payroll Management System - Sample Seed Data
-- =====================================================================

USE `payroll_db`;

-- ---------------------------------------------------------------------
-- 1. SEED USERS
-- Default passwords:
-- admin / Admin@123 (BCrypt hash)
-- hr_manager / Hr@12345 (BCrypt hash)
-- ---------------------------------------------------------------------
INSERT INTO `users` (`id`, `username`, `email`, `password_hash`, `full_name`, `role`, `status`) VALUES
(1, 'admin', 'admin@enterprise.com', '$2a$12$WG/bWfr8TJzjVtFbz9YRHOpJMSFJJHO.MgfkU7sYmf3waxEr9SPPK', 'System Administrator', 'ADMIN', 'ACTIVE'),
(2, 'hrmanager', 'hr@enterprise.com', '$2a$12$sxbcBVy1JJxhxZOthtgwY.LRFmP7Q6bJpJGnr/JMJEqEiJlBHXOZS', 'Eleanor Vance', 'HR', 'ACTIVE');

-- ---------------------------------------------------------------------
-- 2. SEED DEPARTMENTS
-- ---------------------------------------------------------------------
INSERT INTO `departments` (`id`, `name`, `code`, `description`, `status`) VALUES
(1, 'Engineering & Technology', 'ENG', 'Software development, cloud architecture, and DevOps', 'ACTIVE'),
(2, 'Human Resources', 'HR', 'Talent acquisition, employee relations, and payroll compliance', 'ACTIVE'),
(3, 'Finance & Accounting', 'FIN', 'Financial reporting, budgets, taxes, and audits', 'ACTIVE'),
(4, 'Product & Design', 'PRD', 'UI/UX design, product strategy, and analytics', 'ACTIVE'),
(5, 'Sales & Marketing', 'MKT', 'Client acquisition, branding, and enterprise sales', 'ACTIVE');

-- ---------------------------------------------------------------------
-- 3. SEED DESIGNATIONS
-- ---------------------------------------------------------------------
INSERT INTO `designations` (`id`, `department_id`, `title`, `code`, `min_salary`, `max_salary`, `status`) VALUES
(1, 1, 'Senior Software Engineer', 'ENG-SR-DEV', 80000.00, 180000.00, 'ACTIVE'),
(2, 1, 'Lead Cloud Architect', 'ENG-LEAD-ARCH', 120000.00, 250000.00, 'ACTIVE'),
(3, 1, 'QA Automation Engineer', 'ENG-QA-ENG', 60000.00, 130000.00, 'ACTIVE'),
(4, 2, 'HR Manager', 'HR-MGR', 70000.00, 140000.00, 'ACTIVE'),
(5, 2, 'Payroll Specialist', 'HR-PAY-SPEC', 50000.00, 95000.00, 'ACTIVE'),
(6, 3, 'Senior Financial Analyst', 'FIN-SR-ANL', 75000.00, 150000.00, 'ACTIVE'),
(7, 4, 'Principal UX Designer', 'PRD-PR-DSG', 85000.00, 160000.00, 'ACTIVE'),
(8, 5, 'Enterprise Account Executive', 'MKT-EAE', 65000.00, 140000.00, 'ACTIVE');

-- ---------------------------------------------------------------------
-- 4. SEED SALARY COMPONENTS MASTER
-- ---------------------------------------------------------------------
INSERT INTO `salary_components` (`id`, `name`, `code`, `type`, `calculation_type`, `default_rate`, `is_taxable`, `is_mandatory`, `description`, `status`) VALUES
(1, 'Basic Salary', 'BASIC', 'EARNING', 'FIXED_AMOUNT', 0.0000, TRUE, TRUE, 'Base salary component forming foundation for calculations', 'ACTIVE'),
(2, 'House Rent Allowance (HRA)', 'HRA', 'EARNING', 'PERCENTAGE_OF_BASIC', 40.0000, TRUE, FALSE, 'Housing subsidy typically 40-50% of basic pay', 'ACTIVE'),
(3, 'Special Allowance', 'SPECIAL', 'EARNING', 'FIXED_AMOUNT', 0.0000, TRUE, FALSE, 'Customizable supplementary monthly allowance', 'ACTIVE'),
(4, 'Conveyance Allowance', 'CONV', 'EARNING', 'FIXED_AMOUNT', 1600.0000, TRUE, FALSE, 'Commute and transit reimbursement support', 'ACTIVE'),
(5, 'Medical Allowance', 'MED', 'EARNING', 'FIXED_AMOUNT', 1250.0000, TRUE, FALSE, 'Healthcare support allowance', 'ACTIVE'),
(6, 'Provident Fund (PF)', 'PF', 'DEDUCTION', 'PERCENTAGE_OF_BASIC', 12.0000, FALSE, TRUE, 'Statutory retirement fund contribution (12% of basic)', 'ACTIVE'),
(7, 'Professional Tax', 'PTAX', 'DEDUCTION', 'FIXED_AMOUNT', 200.0000, FALSE, TRUE, 'State-mandated employment tax', 'ACTIVE'),
(8, 'TDS / Income Tax', 'TDS', 'DEDUCTION', 'FIXED_AMOUNT', 0.0000, FALSE, FALSE, 'Tax deducted at source based on estimated tax bracket', 'ACTIVE');

-- ---------------------------------------------------------------------
-- 5. SEED EMPLOYEES
-- ---------------------------------------------------------------------
INSERT INTO `employees` (`id`, `employee_code`, `first_name`, `last_name`, `email`, `phone`, `address`, `department_id`, `designation_id`, `joining_date`, `employment_status`, `bank_name`, `account_number`, `ifsc_or_routing`, `pan_or_tax_id`) VALUES
(1, 'EMP1001', 'Alexander', 'Wright', 'alexander.wright@enterprise.com', '+1 (555) 234-5678', '742 Evergreen Terrace, Seattle, WA', 1, 2, '2023-01-15', 'ACTIVE', 'Chase Bank', '439281928301', 'CHASUS33', 'TXID-99201'),
(2, 'EMP1002', 'Sophia', 'Chen', 'sophia.chen@enterprise.com', '+1 (555) 345-6789', '1088 Sansome St, San Francisco, CA', 1, 1, '2023-03-01', 'ACTIVE', 'Bank of America', '582910492810', 'BOFAUS3N', 'TXID-88192'),
(3, 'EMP1003', 'Marcus', 'Johnson', 'marcus.johnson@enterprise.com', '+1 (555) 456-7890', '450 Lexington Ave, New York, NY', 3, 6, '2023-06-10', 'ACTIVE', 'Wells Fargo', '920194827102', 'WFBIUS6S', 'TXID-77381'),
(4, 'EMP1004', 'Elena', 'Rostova', 'elena.rostova@enterprise.com', '+1 (555) 567-8901', '333 Wacker Dr, Chicago, IL', 4, 7, '2023-09-18', 'ACTIVE', 'Citibank', '391029481920', 'CITIUS33', 'TXID-66492'),
(5, 'EMP1005', 'David', 'Kim', 'david.kim@enterprise.com', '+1 (555) 678-9012', '100 Congress Ave, Austin, TX', 1, 3, '2024-02-01', 'ACTIVE', 'Chase Bank', '847291048201', 'CHASUS33', 'TXID-55102'),
(6, 'EMP1006', 'Olivia', 'Taylor', 'olivia.taylor@enterprise.com', '+1 (555) 789-0123', '500 Boylston St, Boston, MA', 2, 4, '2023-05-15', 'ACTIVE', 'TD Bank', '619284019283', 'TDBKUS33', 'TXID-44291'),
(7, 'EMP1007', 'Liam', 'Miller', 'liam.miller@enterprise.com', '+1 (555) 890-1234', '1200 17th St, Denver, CO', 5, 8, '2024-04-01', 'ACTIVE', 'PNC Bank', '739102948201', 'PNCBUS33', 'TXID-33182');

-- ---------------------------------------------------------------------
-- 6. SEED EMPLOYEE SALARY STRUCTURES
-- ---------------------------------------------------------------------
INSERT INTO `employee_salary_structures` (`id`, `employee_id`, `basic_salary`, `hra`, `special_allowance`, `conveyance_allowance`, `medical_allowance`, `pf_rate_pct`, `professional_tax`, `tds_monthly`, `other_deductions`, `effective_date`) VALUES
(1, 1, 80000.00, 32000.00, 20000.00, 1600.00, 1250.00, 12.00, 200.00, 12500.00, 0.00, '2023-01-15'),
(2, 2, 60000.00, 24000.00, 15000.00, 1600.00, 1250.00, 12.00, 200.00, 8000.00, 0.00, '2023-03-01'),
(3, 3, 50000.00, 20000.00, 12000.00, 1600.00, 1250.00, 12.00, 200.00, 6500.00, 0.00, '2023-06-10'),
(4, 4, 55000.00, 22000.00, 13500.00, 1600.00, 1250.00, 12.00, 200.00, 7200.00, 0.00, '2023-09-18'),
(5, 5, 45000.00, 18000.00, 10000.00, 1600.00, 1250.00, 12.00, 200.00, 4500.00, 0.00, '2024-02-01'),
(6, 6, 48000.00, 19200.00, 11000.00, 1600.00, 1250.00, 12.00, 200.00, 5000.00, 0.00, '2023-05-15'),
(7, 7, 42000.00, 16800.00, 9500.00, 1600.00, 1250.00, 12.00, 200.00, 3800.00, 0.00, '2024-04-01');

-- ---------------------------------------------------------------------
-- 7. SEED AUDIT LOG
-- ---------------------------------------------------------------------
INSERT INTO `audit_logs` (`user_id`, `action`, `entity_type`, `entity_id`, `details`, `ip_address`) VALUES
(1, 'SYSTEM_INIT', 'DATABASE', 1, 'Database initialized with enterprise seed data and sample employees', '127.0.0.1');
