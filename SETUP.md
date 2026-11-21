# Crestline Bank — Setup Guide

Crestline Bank is an educational Java Swing + MySQL banking management project. It is a local demo application, not a real banking system.

## Requirements
- JDK 21 or later
- Apache Maven 3.9+
- MySQL Server 8.0+
- VS Code with Extension Pack for Java (or IntelliJ IDEA)

## 1. Open the project
Extract the ZIP file. In VS Code choose **File → Open Folder** and select the `CrestlineBank` folder (the folder containing `pom.xml`).

## 2. Create the database
Open MySQL Workbench, connect to your local MySQL server, open `database/schema.sql`, and execute the complete script.

This creates:
- `crestline_db`
- `customers`, `accounts`, and `transactions`
- `admins` for separate administrator authentication
- `loans` and `loan_repayments` for lending workflows

## 3. Configure MySQL credentials
The default connection is:
- URL: `jdbc:mysql://localhost:3306/crestline_db`
- User: `root`
- Password: empty

If your MySQL root account has a password, set these Windows environment variables before launching:

PowerShell:
```powershell
$env:CRESTLINE_DB_USER="root"
$env:CRESTLINE_DB_PASSWORD="your_mysql_password"
```

Optional URL override:
```powershell
$env:CRESTLINE_DB_URL="jdbc:mysql://localhost:3306/crestline_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

Do not commit real database passwords to GitHub.

## 4. Build and run
Open a terminal in the project root and run:

```bash
mvn clean package
mvn exec:java
```

The first build downloads dependencies, so an internet connection is required.

## 5. Use the application
1. Choose **Create an account** on the sign-in screen.
2. Register with a name, email, and password (minimum 8 characters).
3. Sign in.
4. Try deposits, withdrawals, transfers, and transaction history.
5. Choose **Administrator access** on the login screen. The first administrator can be created once; later access requires that account. Store the admin password safely.
6. In the customer dashboard, open **My loans** to submit a loan request or repay an approved loan.
7. In the admin dashboard, use the left sidebar to review customers/accounts, transaction monitoring, loan requests, repayment history and analytics.
8. In the customer dashboard, open **Crestline Pay** to create child UPI identities, set daily/per-payment caps, block/reactivate identities, record demo payments, review family payment history and create emergency spending requests.

New accounts start with ₹0.00. To test transfers, create two customer accounts and transfer between their account numbers. For an existing installation, run `database/schema.sql` again if needed, then run `database/upgrade_family_pay_interest_audit.sql` once to add loan-interest fields, account audit logging and family UPI tables. The migration is additive and does not drop existing customer, account or transaction records.

## Project layout
```text
src/main/java/com/crestlinebank/
  Main.java
  config/       Database connection
  dao/          SQL data access
  model/        Customer, admin, transaction and loan models
  service/      Validation and banking operations
  ui/           Swing screens, customer dashboard, admin dashboard and theme
  util/         Password hashing
database/
  schema.sql
  upgrade_family_pay_interest_audit.sql
```

## Security and limitations
- Passwords are stored as BCrypt hashes.
- SQL uses prepared statements.
- Deposit, withdrawal, and transfer operations use database transactions and row locks.
- This is a learning/demo project. Crestline Pay is simulated and does not connect to NPCI/UPI or real payment rails. This build does not provide a separate child-device login, real UPI PIN, QR scanning, or real collect requests. It does not implement production KYC, OTP/MFA, regulatory compliance or bank-grade fraud controls.
- Use only test data. Never use real customer credentials or money.


## BankPro-inspired Crestline modules

This build includes customer deposits, withdrawals, account-to-account transfers, transaction history, CSV account statements, loan requests, EMI estimation, repayments, admin loan decisions, transaction monitoring, monthly analytics, CSV analytics exports, and a customer-to-admin support inbox with replies. The project remains a Java Swing + MySQL Crestline application; BankPro is used only as a feature reference. Parent/guardian and minor-account features are intentionally excluded.

### Database update

For an existing `crestline_db`, run `database/schema.sql`, then run `database/upgrade_family_pay_interest_audit.sql` once. The upgrade adds the family UPI and audit tables and loan interest fields without deleting existing customer, account, transaction or loan records.

## Staff, payroll and family UPI controls (added feature upgrade)

After the existing database is set up, run these scripts in MySQL Workbench against `crestline_db`, in this order:

1. `database/upgrade_family_pay_interest_audit.sql` (if it has not already been run for the previous Crestline upgrade).
2. `database/upgrade_staff_family_controls.sql`.

The Main Admin sidebar now includes **Staff & Payroll**. Admin can hire staff, assign a role, set monthly salary and a per-person loan approval limit, change salary/authority, assign tasks, record demo salary payments, and deactivate/reactivate staff. Deactivation requires a reason and retains the employee record and history.

Staff use **Staff sign in** on the main login screen. Staff accounts are created by the Main Admin; passwords are stored as BCrypt hashes. Loan approval/rejection actions are available only to Loan Officers and Branch Managers. An approval is rejected by the service if the requested principal exceeds that employee's current approval limit; higher-value requests must be handled by an Admin. Staff can view and complete assigned tasks.

Crestline Pay now creates a parent demo UPI ID on first opening (`parent<CUSTOMER_ID>@crestline`). The parent can block or unblock an exact game, website, merchant name or UPI ID for an individual child profile. This blocks only matching simulated payments to that merchant; other child payments continue under their normal daily/per-transaction limits.

**Demo limitations:** Staff payroll entries record a paid status only; they do not transfer money. Crestline Pay, parent/child UPI IDs, merchant blocking and payments are local simulations, not NPCI/real UPI integrations. Merchant blocking matches the exact normalized merchant name or UPI ID entered in the demo payment form; it cannot inspect or block purchases made in unrelated third-party apps or websites.


## Family Banking: independent child sign-in upgrade

1. Back up `crestline_db` before applying any schema migration.
2. Run `database/upgrade_child_login.sql` after the existing family UPI tables have been created. This adds a nullable password-hash column to `family_upi_profiles`; existing child profiles remain intact but cannot sign in until a parent sets a password (edit/recreate profile workflow currently supports setting credentials when adding a child).
3. Build and run the application with Java 21 and Maven: `mvn clean package` then `mvn exec:java`.
4. Parent: sign in normally, open Crestline Pay, choose Add child UPI, and set the child's login password and spending limits.
5. Child: return to the main login screen, choose Child UPI sign in, and sign in with the child's UPI ID and password.
6. Successful child payments are checked against the child's active status, per-payment cap, daily cap, blocked merchants, and the parent's available account balance. The amount is debited from the parent's account and recorded in both the bank transaction ledger and family UPI ledger. The parent's Family UPI History and child's My Transactions show the same payment.

This is a local educational simulation. It does not connect to NPCI or move real money. The parent account is the source of funds; the child has no separate deposit account or balance.

### Persistent in-app notifications (latest upgrade)
After the previous family/staff migrations and `upgrade_child_login.sql` have been applied, run `database/upgrade_notifications.sql` in MySQL Workbench. This creates the notification inbox and database triggers. Keep the existing `crestline_db`; do not drop or recreate it. The notification migration expects the loan, support, family UPI and merchant-control tables from the earlier migrations to exist.

The customer, administrator and child dashboards now show a Notifications button with an unread count. Notifications are stored in MySQL and remain available after sign-out. The child daily cap is calculated against payments dated `CURRENT_DATE`, so the available daily allowance naturally resets on the next calendar day (midnight according to the MySQL server's date/time configuration).
