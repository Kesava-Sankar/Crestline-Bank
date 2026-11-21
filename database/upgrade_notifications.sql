-- Crestline persistent in-app notifications. Additive migration; does not delete business data.
USE crestline_db;
CREATE TABLE IF NOT EXISTS notifications (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 recipient_type ENUM('ADMIN','CUSTOMER','CHILD') NOT NULL,
 recipient_id BIGINT NOT NULL,
 title VARCHAR(160) NOT NULL,
 message VARCHAR(700) NOT NULL,
 is_read TINYINT(1) NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_notifications_inbox(recipient_type,recipient_id,is_read,created_at)
) ENGINE=InnoDB;

DELIMITER $$
DROP TRIGGER IF EXISTS trg_notify_loan_created$$
CREATE TRIGGER trg_notify_loan_created AFTER INSERT ON loans FOR EACH ROW
BEGIN
 IF NEW.status='PENDING' THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('ADMIN',0,'New loan application',CONCAT('Customer #',NEW.customer_id,' submitted loan #',NEW.id,' for ₹',FORMAT(NEW.principal,2),'.'));
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_notify_loan_decision$$
CREATE TRIGGER trg_notify_loan_decision AFTER UPDATE ON loans FOR EACH ROW
BEGIN
 IF OLD.status<>NEW.status AND NEW.status IN ('APPROVED','REJECTED') THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CUSTOMER',NEW.customer_id,CONCAT('Loan ',LOWER(NEW.status)),CONCAT('Your loan application #',NEW.id,' was ',LOWER(NEW.status),'.',IF(NEW.status='APPROVED',CONCAT(' Approved amount ₹',FORMAT(NEW.principal,2),' has been credited to your account.'),'')));
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_notify_repayment$$
CREATE TRIGGER trg_notify_repayment AFTER INSERT ON loan_repayments FOR EACH ROW
BEGIN
 DECLARE cid INT;
 SELECT customer_id INTO cid FROM loans WHERE id=NEW.loan_id;
 INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('ADMIN',0,'Loan repayment received',CONCAT('Customer #',cid,' paid ₹',FORMAT(NEW.amount,2),' for loan #',NEW.loan_id,'.'));
END$$
DROP TRIGGER IF EXISTS trg_notify_family_payment$$
CREATE TRIGGER trg_notify_family_payment AFTER INSERT ON family_upi_transactions FOR EACH ROW
BEGIN
 IF NEW.status='SUCCESS' THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CUSTOMER',NEW.parent_customer_id,'Child UPI payment',CONCAT('Child payment of ₹',FORMAT(NEW.amount,2),' to ',NEW.recipient,' was completed.'));
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CHILD',NEW.profile_id,'Payment successful',CONCAT('Your payment of ₹',FORMAT(NEW.amount,2),' to ',NEW.recipient,' was successful.'));
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_notify_child_profile$$
CREATE TRIGGER trg_notify_child_profile AFTER UPDATE ON family_upi_profiles FOR EACH ROW
BEGIN
 IF OLD.status<>NEW.status AND NEW.status='BLOCKED' THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CHILD',NEW.id,'Child UPI blocked','Your parent has blocked your Crestline Pay access. Contact your parent for details.');
 END IF;
 IF OLD.daily_limit<>NEW.daily_limit THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CHILD',NEW.id,'Daily spending limit changed',CONCAT('Your daily spending limit is now ₹',FORMAT(NEW.daily_limit,2),'.'));
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_notify_merchant_block_insert$$
CREATE TRIGGER trg_notify_merchant_block_insert AFTER INSERT ON family_blocked_merchants FOR EACH ROW
BEGIN
 IF NEW.blocked=1 THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CHILD',NEW.profile_id,'Merchant blocked',CONCAT('Your parent blocked ',NEW.display_name,' for your child UPI payments.'));
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_notify_merchant_block$$
CREATE TRIGGER trg_notify_merchant_block AFTER UPDATE ON family_blocked_merchants FOR EACH ROW
BEGIN
 IF OLD.blocked<>NEW.blocked AND NEW.blocked=1 THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CHILD',NEW.profile_id,'Merchant blocked',CONCAT('Your parent blocked ',NEW.display_name,' for your child UPI payments.'));
 END IF;
END$$
DROP TRIGGER IF EXISTS trg_notify_support_new$$
CREATE TRIGGER trg_notify_support_new AFTER INSERT ON support_requests FOR EACH ROW
BEGIN
 INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('ADMIN',0,'New customer support request',CONCAT('Customer #',NEW.customer_id,' submitted: ',NEW.subject));
END$$
DROP TRIGGER IF EXISTS trg_notify_support_reply$$
CREATE TRIGGER trg_notify_support_reply AFTER UPDATE ON support_requests FOR EACH ROW
BEGIN
 IF (OLD.admin_reply IS NULL OR OLD.admin_reply='') AND NEW.admin_reply IS NOT NULL AND NEW.admin_reply<>'' THEN
  INSERT INTO notifications(recipient_type,recipient_id,title,message) VALUES('CUSTOMER',NEW.customer_id,'Admin replied to your request',CONCAT('Your support request "',NEW.subject,'" has a response.'));
 END IF;
END$$
DELIMITER ;
