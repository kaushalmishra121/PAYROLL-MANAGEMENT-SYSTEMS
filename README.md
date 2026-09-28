# Payroll Management System

A desktop-based **Payroll & Workforce Management System** developed as a **college academic project** using Java, JavaFX, JDBC, and MySQL.

The application is designed to demonstrate practical implementation of **Object-Oriented Programming, database management, layered architecture, authentication, payroll calculation, attendance and leave management, reporting, and JavaFX-based GUI development**.

---

## 📌 Project Overview

The Payroll Management System provides a centralized desktop application for managing employee-related operations and payroll workflows.

It includes modules for:

- Employee management
- Department and designation management
- Attendance tracking
- Leave management
- Salary structure management
- Payroll processing
- Payslip generation
- Reports and data export
- User authentication and role-based access
- Audit logging
- Dashboard analytics

> **Project Type:** College / Academic Project  
> **Application:** Desktop Application  
> **Primary Language:** Java

---

## ✨ Key Features

### 🔐 Authentication & User Management
- User login with password hashing using BCrypt
- Role-based access for administrative and HR operations
- Session management
- User account management
- Audit logging for important actions

### 👨‍💼 Employee Management
- Add, update, search, and manage employee records
- Department and designation assignment
- Salary structure configuration
- Employee-related information management

### 🏢 Organization Management
- Department management
- Designation management
- Salary-band related configuration
- Referential integrity through the database layer

### 🕒 Attendance & Leave Management
- Attendance record management
- Check-in / check-out tracking
- Duplicate attendance prevention
- Leave request management
- Leave approval workflow
- Unpaid leave / Loss of Pay calculation

### 💰 Payroll Processing
- Monthly payroll processing
- Salary calculation using `BigDecimal`
- Earnings and deductions
- PF and professional-tax style configurable deductions
- Overtime / bonus and other adjustments
- Prevention of duplicate payroll runs
- Payroll approval and payment-status tracking

### 📄 Reports & Payslips
- PDF payslip generation
- CSV data export
- Payroll reporting
- Employee and payroll information summaries

### 📊 Dashboard
- Payroll KPIs
- Employee / department statistics
- Payroll trend charts
- Recent transaction information
- JavaFX-based data visualisation

### 🎨 User Interface
- JavaFX desktop interface
- Dark / light theme support
- Responsive tables and forms
- Navigation sidebar
- Styled dialogs and notifications

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Java 17** | Core application development |
| **JavaFX 21** | Desktop GUI |
| **JDBC** | Database connectivity |
| **MySQL 8+** | Relational database |
| **HikariCP** | JDBC connection pooling |
| **BCrypt** | Password hashing |
| **Maven** | Dependency & build management |
| **JUnit 5** | Testing |
| **H2** | In-memory database for tests |
| **OpenPDF** | PDF payslip generation |
| **SLF4J + Logback** | Application logging |

---

## 🏗️ Architecture

The project follows a layered structure to keep the user interface, business logic, and database operations separated.

```text
com.payroll/
├── config/       # Application & database configuration
├── controller/   # JavaFX controllers / UI event handling
├── dao/          # Database access using JDBC
├── model/        # Domain models and DTOs
├── service/      # Business logic and payroll processing
├── util/         # Validation, security, PDF/CSV and utility classes
└── exception/    # Application-specific exceptions
```

### Application Flow

```text
JavaFX UI
   ↓
Controllers
   ↓
Services / Business Logic
   ↓
DAO Layer
   ↓
MySQL Database
```

---

## 🧮 Payroll Calculation

The payroll calculation engine uses Java's `BigDecimal` for monetary calculations.

The system considers factors such as:

- Basic salary
- HRA and other allowances
- Joining-date based proration
- Overtime / bonus
- Approved unpaid leave
- PF and other configurable deductions
- Other payroll adjustments

A simplified calculation flow is:

```text
Gross Earnings
      ↓
Total Deductions
      ↓
Net Salary
```

The exact deduction rates are configurable within the application and should be reviewed according to the requirements of the intended deployment environment.

---

## 🚀 Getting Started

### Prerequisites

Install the following:

- **JDK 17 or later**
- **Apache Maven 3.8+**
- **MySQL 8.0+**
- MySQL client / MySQL Workbench

Verify the installations:

```bash
java -version
mvn -version
mysql --version
```

### 1. Clone the Repository

```bash
git clone https://github.com/kaushalmishra121/PAYROLL-MANAGEMENT-SYSTEMS.git
cd PAYROLL-MANAGEMENT-SYSTEMS
```

### 2. Set Up the Database

The SQL scripts are available in the `database/` directory.

Run:

```sql
source database/schema.sql;
source database/seed.sql;
```

You can also execute the scripts through MySQL Workbench.

### 3. Configure Database Credentials

Configure the application's database connection using the project's configuration mechanism.

**Do not commit your real MySQL password or other credentials to GitHub.**

For local development, use your own database username and password.

### 4. Run Tests

```bash
mvn test
```

### 5. Run the Application

```bash
mvn clean javafx:run
```

---

## 📂 Project Structure

```text
PAYROLL-MANAGEMENT-SYSTEMS/
│
├── database/
│   ├── schema.sql
│   └── seed.sql
│
├── src/
│   ├── main/
│   │   ├── java/com/payroll/
│   │   └── resources/
│   │
│   └── test/
│
├── .gitignore
├── pom.xml
└── README.md
```

---

## 🧪 Testing

The project includes automated tests using **JUnit 5**.

The test suite covers areas such as:

- Authentication
- Employee validation
- Payroll calculations
- Leave and unpaid-day calculations
- Payroll workflow
- Export functionality
- JavaFX/FXML loading

Run all tests with:

```bash
mvn test
```

---

## 🔒 Security & Configuration Notes

- Passwords are hashed using BCrypt rather than being stored as plain text.
- Database credentials should remain local and must not be committed to the repository.
- The included seed data is intended for development / academic demonstration.
- Before using the application with real employee information, additional production security, privacy, authorization, and compliance measures would be required.

---

## 🎓 Academic Purpose

This project was developed as a **college academic project** to apply software-development concepts in a practical application.

### Concepts Demonstrated

- Object-Oriented Programming
- Java Collections and exception handling
- Layered architecture
- JDBC and SQL
- Relational database design
- CRUD operations
- Authentication and authorization concepts
- Payroll/business-rule implementation
- File generation and data export
- Unit and integration testing
- JavaFX GUI development
- Maven project management

---

## 🔮 Possible Future Enhancements

- Standalone Windows installer
- Improved deployment configuration
- Cloud database support
- Email notifications
- Advanced role and permission management
- Additional analytics and reports
- Automated database backup
- CI/CD pipeline
- Improved cross-platform packaging

---

## 👨‍💻 Author

**Kaushal Mishra**

GitHub: [@kaushalmishra121](https://github.com/kaushalmishra121)

---

## 📄 License

This repository is primarily intended for **academic and educational purposes**.
