# 💼 Enterprise Payroll Management System

A commercial-grade, desktop-based **Payroll & Workforce Management System** built with **Java 17+ (JavaFX)**, **JDBC (MySQL Connector/J & HikariCP)**, **BCrypt**, and **MySQL**.

Designed with a high-fidelity enterprise UI/UX (Slate/Navy theme, dark/light mode toggle, dynamic JavaFX charts, responsive data tables, non-blocking asynchronous operations, and PDF/CSV reporting engines).

---

## 🏗️ Architecture & Layered Design

The system implements strict separation of concerns:

```text
com.payroll/
├── MainApp.java                    # JavaFX Application lifecycle & window coordinator
├── config/                         # Database connection pooling & configuration management
│   ├── AppConfig.java              # Multi-tier properties reader (System -> Env -> File)
│   ├── DatabaseConfig.java         # Thread-safe HikariCP DataSource manager
│   └── DatabaseInitializer.java    # Auto schema/seed table verification
├── model/                          # Domain entities & analytical DTOs
│   ├── User.java                   # RBAC entities (ADMIN, HR)
│   ├── Employee.java               # Workforce data with masking helpers
│   ├── Department.java             # Organizational business units
│   ├── Designation.java            # Job titles & salary bands
│   ├── SalaryComponent.java        # Configurable earnings/deductions master
│   ├── EmployeeSalaryStructure.java# Per-employee monetary compensation
│   ├── Attendance.java             # Daily check-in/out records
│   ├── LeaveRequest.java           # Leave approval workflow entities
│   ├── PayrollRun.java             # Monthly batch payroll headers
│   ├── PayrollRecord.java          # Itemized snapshot payslips
│   ├── PayrollAdjustment.java      # Auditable additions/deductions
│   ├── AuditLog.java               # Traceability & audit records
│   └── DashboardSummary.java       # Aggregated KPI & chart metrics
├── dao/                            # Data Access Objects with JDBC PreparedStatements
│   ├── UserDao.java / UserDaoImpl.java
│   ├── EmployeeDao.java / EmployeeDaoImpl.java
│   ├── DepartmentDao.java / DepartmentDaoImpl.java
│   ├── DesignationDao.java / DesignationDaoImpl.java
│   ├── SalaryStructureDao.java / SalaryStructureDaoImpl.java
│   ├── AttendanceDao.java / AttendanceDaoImpl.java
│   ├── LeaveDao.java / LeaveDaoImpl.java
│   ├── PayrollDao.java / PayrollDaoImpl.java
│   └── AuditDao.java / AuditDaoImpl.java
├── service/                        # Business logic, transactions & calculation engine
│   ├── AuthService.java            # Authentication, BCrypt verification, sessions
│   ├── EmployeeService.java        # Transactional employee creation & search
│   ├── DepartmentService.java      # Hierarchy & salary band constraints
│   ├── AttendanceService.java      # Duplicate prevention & attendance tracking
│   ├── LeaveService.java           # Workflow reviews & unpaid day calculations
│   ├── PayrollCalculationEngine.java # Pure BigDecimal deterministic math engine
│   ├── PayrollService.java         # Atomic multi-record payroll transactions
│   ├── DashboardService.java       # Real-time metrics aggregator
│   ├── ReportService.java          # PDF payslip & CSV generation coordinator
│   └── AuditService.java           # Security logging
├── controller/                     # JavaFX Controllers (UI Event Handlers)
│   ├── LoginController.java        # Async credentials authentication
│   ├── MainLayoutController.java   # Dynamic sidebar navigation & theme switcher
│   ├── DashboardController.java    # KPI cards, BarChart & PieChart series
│   ├── EmployeeManagementController.java # Multi-filter directory & modal forms
│   ├── DepartmentManagementController.java # Departments & designations
│   ├── AttendanceController.java   # Monthly logs & record dialogs
│   ├── LeaveManagementController.java # Status filters & review modals
│   ├── PayrollProcessingController.java # Batch run generator & adjustments
│   ├── ReportController.java       # PDF/CSV export hub
│   └── UserManagementController.java # User accounts & live audit logs
├── util/                           # Utilities
│   ├── PasswordHasher.java         # BCrypt log rounds 12
│   ├── UserSession.java            # Thread-safe user context
│   ├── CurrencyUtils.java          # BigDecimal formatters & parsers
│   ├── DateUtils.java              # Date formatters & working day helpers
│   ├── ValidationUtils.java        # Email, phone, number validators
│   ├── CsvExporter.java            # Robust CSV exports with UTF-8 BOM
│   ├── PdfPayslipGenerator.java    # OpenPDF high-resolution payslip engine
│   └── DialogUtils.java            # Styled alerts, toasts, and confirmations
└── exception/                      # Domain exception classes
```

---

## 🧮 Mathematical Model & Payroll Formulas

All financial calculations in `PayrollCalculationEngine.java` strictly use `java.math.BigDecimal` with `RoundingMode.HALF_UP` (scaled to 2 decimal places):

1. **Active Working Days & Proration ($F_{join}$)**:
   $$\text{Days In Month} = \text{Length of selected month (e.g. 28, 29, 30, or 31)}$$
   $$\text{Active Days} = \text{Days In Month} - \text{Joining Day} + 1 \quad (\text{if joined mid-month})$$
   $$F_{join} = \frac{\text{Active Days}}{\text{Days In Month}}$$

2. **Gross Base Earnings**:
   $$\text{Basic} = \text{Structure.basicSalary} \times F_{join}$$
   $$\text{HRA} = \text{Structure.hra} \times F_{join}$$
   $$\text{Allowances} = (\text{Special} + \text{Conveyance} + \text{Medical}) \times F_{join}$$
   $$\text{Gross Base} = \text{Basic} + \text{HRA} + \text{Allowances}$$
   $$\text{Gross Earnings} = \text{Gross Base} + \text{Overtime/Bonus}$$

3. **Loss of Pay (Unpaid Leave Deduction)**:
   $$\text{Daily Rate} = \frac{\text{Gross Base}}{\text{Days In Month}}$$
   $$\text{Unpaid Loss} = \text{Daily Rate} \times \text{Approved Unpaid Leave Days}$$

4. **Statutory & Configurable Deductions**:
   $$\text{Provident Fund (PF)} = \text{Basic} \times \left(\frac{\text{PF Rate \%}}{100}\right)$$
   $$\text{Total Deductions} = \text{PF} + \text{Professional Tax} + \text{TDS / Income Tax} + \text{Unpaid Loss} + \text{Other Deductions}$$

5. **Net Payable Salary**:
   $$\text{Net Salary} = \max(0.00, \text{Gross Earnings} - \text{Total Deductions})$$

> **⚠️ Statutory Notice**: Tax rates and provident fund rates are configurable parameters in the system. Employers must verify deduction settings against relevant local labor and tax regulations.

---

## 🚀 Setup & Execution Guide

### Prerequisites
- **Java**: JDK 17, 21, or 25 LTS installed (`java -version`).
- **Maven**: Apache Maven 3.8+ installed (`mvn -version`).
- **MySQL**: MySQL Server 8.0+ running on port 3306.

---

### Step 1: Database Setup
1. Open your MySQL client (MySQL Workbench, HeidiSQL, or MySQL CLI):
```bash
mysql -u root -p
```
2. Execute the database schema and seed data scripts located in `database/`:
```sql
source database/schema.sql;
source database/seed.sql;
```

---

### Step 2: Configure Database Credentials
Edit `src/main/resources/config/db.properties` or provide environment variables:

```properties
db.url=jdbc:mysql://localhost:3306/payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
db.username=root
db.password=your_mysql_password
```

*(You can also override parameters at runtime using `-Ddb.password=...` or environment variable `DB_PASSWORD`)*.

---

### Step 3: Run Automated Tests
Execute the comprehensive test suite with JUnit 5:

```bash
mvn test
```

All unit and integration tests (covering authentication, BCrypt hashing, salary calculations, unpaid leave deductions, mid-month proration, transaction state machines, duplicate run prevention, CSV and PDF generation) will execute and report results.

---

### Step 4: Run the Application
Launch the desktop application via Maven:

```bash
mvn clean javafx:run
```

---

## 🔑 Default Pre-Configured Demo Accounts

| Role | Username | Email | Password | Access Level |
| :--- | :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin@enterprise.com` | `Admin@123` | Full Access (User Governance, Audit Logs, Approvals, All Modules) |
| **HR Manager** | `hrmanager` | `hr@enterprise.com` | `Hr@12345` | Workforce Management, Leaves, Attendance, Payroll Processing |

*(Passwords are securely hashed using BCrypt with 12 rounds of salting and never stored in plaintext).*

---

## 📄 Key Features Summary

- **Executive Dashboard**: Live KPI cards, 6-Month Gross vs Net Payroll Trends BarChart, Department Headcount Distribution PieChart, and recent transactions.
- **Workforce Directory**: Full CRUD, live multi-parameter search/filter, and multi-tab modal for profile, banking, and customizable compensation structures.
- **Organization Management**: Department and Designation master with salary bands and referential integrity protection.
- **Attendance & Leaves**: Daily check-in/out logging with duplicate prevention, leave requests, approval workflow, and automated Loss of Pay computation.
- **Transactional Payroll Runs**: Automated draft calculations, approval gates, payment disbursement tracking, auditable adjustments, and duplicate period prevention.
- **High-Fidelity PDF Payslips**: Crisp two-column payslips with employee details, earnings, deductions, net salary card, authorized signatures, and disclaimers generated via OpenPDF.
- **Audit & Security**: Comprehensive audit log trail tracking every sensitive action, operator ID, and timestamp.
