USE crestline_db;

-- Safe additive migration: preserves all existing Crestline records.
CREATE TABLE IF NOT EXISTS support_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    subject VARCHAR(140) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    admin_reply VARCHAR(2000) NULL,
    status ENUM('OPEN','REPLIED','CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_support_status_created (status, created_at)
) ENGINE=InnoDB;
