-- Crestline Bank feature upgrade for an EXISTING crestline_db.
-- Run once in MySQL Workbench. It is additive and never drops customer/account data.
USE crestline_db;

DELIMITER $$
CREATE PROCEDURE crestline_add_loan_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='loans' AND column_name='total_interest') THEN
    ALTER TABLE loans ADD COLUMN total_interest DECIMAL(15,2) NOT NULL DEFAULT 0.00;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='loans' AND column_name='interest_paid') THEN
    ALTER TABLE loans ADD COLUMN interest_paid DECIMAL(15,2) NOT NULL DEFAULT 0.00;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='loans' AND column_name='principal_paid') THEN
    ALTER TABLE loans ADD COLUMN principal_paid DECIMAL(15,2) NOT NULL DEFAULT 0.00;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='loans' AND column_name='next_due_date') THEN
    ALTER TABLE loans ADD COLUMN next_due_date DATE NULL;
  END IF;
END$$
DELIMITER ;
CALL crestline_add_loan_columns();
DROP PROCEDURE crestline_add_loan_columns;

CREATE TABLE IF NOT EXISTS account_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_number VARCHAR(20) NOT NULL,
  admin_id INT NULL,
  action VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_account_audit_account(account_number,created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS family_upi_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_customer_id INT NOT NULL,
  child_name VARCHAR(120) NOT NULL,
  child_mobile VARCHAR(20) NOT NULL,
  child_upi_id VARCHAR(120) NOT NULL UNIQUE,
  daily_limit DECIMAL(12,2) NOT NULL DEFAULT 2000.00,
  per_txn_limit DECIMAL(12,2) NOT NULL DEFAULT 1000.00,
  status ENUM('ACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_family_upi_parent FOREIGN KEY(parent_customer_id) REFERENCES customers(id),
  INDEX idx_family_upi_parent(parent_customer_id,status)
) ENGINE=InnoDB;

DELIMITER $$
CREATE PROCEDURE crestline_add_child_mobile()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='family_upi_profiles' AND column_name='child_mobile') THEN
    ALTER TABLE family_upi_profiles ADD COLUMN child_mobile VARCHAR(20) NOT NULL DEFAULT '';
  END IF;
END$$
DELIMITER ;
CALL crestline_add_child_mobile();
DROP PROCEDURE crestline_add_child_mobile;

CREATE TABLE IF NOT EXISTS family_upi_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  parent_customer_id INT NOT NULL,
  recipient VARCHAR(160) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status ENUM('SUCCESS','BLOCKED','PENDING','FAILED') NOT NULL,
  description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_family_tx_profile FOREIGN KEY(profile_id) REFERENCES family_upi_profiles(id),
  CONSTRAINT fk_family_tx_parent FOREIGN KEY(parent_customer_id) REFERENCES customers(id),
  INDEX idx_family_tx_profile_date(profile_id,created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS family_spend_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  requested_amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at TIMESTAMP NULL,
  CONSTRAINT fk_family_request_profile FOREIGN KEY(profile_id) REFERENCES family_upi_profiles(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS family_upi_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  action VARCHAR(40) NOT NULL,
  details VARCHAR(500) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_family_audit_profile FOREIGN KEY(profile_id) REFERENCES family_upi_profiles(id)
) ENGINE=InnoDB;
