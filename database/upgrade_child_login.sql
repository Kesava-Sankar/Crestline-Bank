-- Crestline Family Banking: additive migration for independent child sign-in.
USE crestline_db;
DELIMITER $$
CREATE PROCEDURE crestline_add_child_login_column()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='family_upi_profiles' AND column_name='login_password_hash') THEN
    ALTER TABLE family_upi_profiles ADD COLUMN login_password_hash VARCHAR(100) NULL AFTER child_upi_id;
  END IF;
END$$
DELIMITER ;
CALL crestline_add_child_login_column();
DROP PROCEDURE crestline_add_child_login_column;
