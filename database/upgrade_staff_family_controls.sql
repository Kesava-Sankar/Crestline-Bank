-- Crestline staff/payroll/parent UPI/merchant controls; additive migration.
USE crestline_db;
CREATE TABLE IF NOT EXISTS staff (
 id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(120) NOT NULL, email VARCHAR(190) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, role ENUM('CASHIER','LOAN_OFFICER','CUSTOMER_SUPPORT','ACCOUNT_OFFICER','BRANCH_MANAGER','AUDITOR') NOT NULL,
 salary DECIMAL(12,2) NOT NULL DEFAULT 0, max_loan_approval DECIMAL(12,2) NOT NULL DEFAULT 0,
 status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE', joined_on DATE NOT NULL DEFAULT (CURRENT_DATE), employment_note VARCHAR(500) NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS staff_activity_log (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, staff_id INT NOT NULL, action VARCHAR(50) NOT NULL, details VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_staff_activity_staff FOREIGN KEY(staff_id) REFERENCES staff(id), INDEX idx_staff_activity(staff_id,created_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS staff_salary_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, staff_id INT NOT NULL, salary DECIMAL(12,2) NOT NULL,
 effective_from DATE NOT NULL DEFAULT (CURRENT_DATE), changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_salary_staff FOREIGN KEY(staff_id) REFERENCES staff(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS staff_payroll (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, staff_id INT NOT NULL, pay_month CHAR(7) NOT NULL,
 amount DECIMAL(12,2) NOT NULL, status ENUM('PENDING','PAID') NOT NULL DEFAULT 'PENDING',
 paid_at TIMESTAMP NULL, UNIQUE KEY uq_staff_pay_month(staff_id,pay_month),
 CONSTRAINT fk_payroll_staff FOREIGN KEY(staff_id) REFERENCES staff(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS family_parent_upi (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, parent_customer_id INT NOT NULL UNIQUE,
 parent_upi_id VARCHAR(120) NOT NULL UNIQUE,
 CONSTRAINT fk_parent_upi_customer FOREIGN KEY(parent_customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS family_blocked_merchants (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, profile_id BIGINT NOT NULL, merchant_key VARCHAR(180) NOT NULL,
 display_name VARCHAR(180) NOT NULL, blocked TINYINT(1) NOT NULL DEFAULT 1, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uq_profile_merchant(profile_id,merchant_key),
 CONSTRAINT fk_blocked_merchant_profile FOREIGN KEY(profile_id) REFERENCES family_upi_profiles(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS staff_tasks (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, staff_id INT NOT NULL, title VARCHAR(180) NOT NULL, details VARCHAR(500),
 status ENUM('PENDING','COMPLETED') NOT NULL DEFAULT 'PENDING', assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, completed_at TIMESTAMP NULL,
 CONSTRAINT fk_staff_task_staff FOREIGN KEY(staff_id) REFERENCES staff(id), INDEX idx_staff_task_status(staff_id,status)
) ENGINE=InnoDB;
