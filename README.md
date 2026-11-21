# Crestline Bank

A desktop banking management system built with Java Swing, MySQL, JDBC, Maven, and BCrypt.

### Features
- Customer registration and secure sign-in
- Auto-generated account numbers
- Balance overview
- Deposit and withdrawal
- Account-to-account transfers
- Recent transaction history
- Separate administrator access and dashboard
- Customer/account administration with freeze, activate and close controls
- Transaction monitoring with large-value review flags
- Loan requests, approval/rejection and customer repayments
- Admin overview metrics and repayment history
- Database-backed persistence
- Transaction-safe balance updates

### Quick start
See [SETUP.md](SETUP.md) for full instructions.

```bash
mvn clean package
mvn exec:java
```

**Educational demo only:** not intended for real banking or handling real funds.


## Local database credentials

1. Copy `config.example.properties` to a new file named `config.properties` in the project root (beside `pom.xml`).
2. Set `db.password` to the password used by your local MySQL `root` account.
3. Keep `config.properties` private. It is excluded from Git.
4. The application uses the local `crestline_db` database by default.

## UI polish update
- Updated the login hero panel to use a Crestline private-banking building image.
- Increased login and registration input typography and field height for readability.
- Applied Crestline navy/cream/gold defaults to Swing dialogs and common controls.
- Added a subtle animated light sweep to the login hero; the existing splash animation is retained.
- Standardized primary and outline button rendering to keep text contrast readable.

The application remains a Java Swing demo. Animation is intentionally subtle to keep the UI responsive.


## BankPro-inspired Crestline modules

This build includes customer deposits, withdrawals, account-to-account transfers, transaction history, CSV account statements, loan requests, EMI estimation, repayments, admin loan decisions, transaction monitoring, monthly analytics, CSV analytics exports, and a customer-to-admin support inbox with replies. The project remains a Java Swing + MySQL Crestline application; BankPro is used only as a feature reference. Parent/guardian family UPI controls are included as a simulated demo feature.

### Database update

For an existing `crestline_db`, open `database/schema.sql` in MySQL Workbench and run it. The script uses `CREATE TABLE IF NOT EXISTS` and adds the `support_requests` table without deleting existing customers, accounts, transactions, or loans.

## Latest feature upgrade
- Admin dashboard uses a permanent left navigation sidebar.
- Loan desk separates application review from customer-level interest tracking; analytics includes separate loan-status, repayment-progress and interest-collection pie charts.
- Account freeze/reactivation/closure requires an admin reason and writes an audit record. Account closure is a soft close; financial history is retained.
- Loan repayment allocation records principal and interest separately. Existing installations must run `database/upgrade_family_pay_interest_audit.sql` once.
- Customer dashboard includes Crestline Pay family controls: parent-owned secondary child UPI IDs, child mobile contact, per-child daily/per-payment caps, block/reactivate, simulated payments, transaction history and emergency spending requests.
- Loan request and administrator access forms scroll on smaller displays.

Crestline Pay is a local simulation only. It does not connect to NPCI, UPI, a bank or real payment networks. Child mobile numbers are profile details; this build does not provide a separate child-device login or real UPI PIN/QR/collect rail.

### Staff and family controls

This upgrade adds a separate staff sign-in, role-based staff loan approval ceilings, assigned tasks, staff hiring/deactivation, salary configuration and demo payroll records. It also adds a parent UPI identity and per-child merchant blocking to the simulated Crestline Pay family controls. Follow `SETUP.md` and run the additive SQL migrations in the specified order.


## Notification and family-limit upgrade
- Added persistent in-app notification inboxes for administrators, customers and child profiles. Notifications remain available after logout and can be marked read in bulk.
- Database triggers create alerts for new loan applications, loan approvals/rejections, loan repayments, successful child payments, child UPI blocking, merchant blocking, daily-limit changes and support requests/replies.
- Customer, admin and child dashboards now include a Notifications button.
- Child daily spending allowance refreshes automatically by calendar date: the existing daily-spend calculation sums only payments made on `CURRENT_DATE`, so there is no stored allowance to manually reset and unused allowance does not roll over.
- Parent UPI ID and copy action are already present on the main customer dashboard.

### Apply the notification migration
After applying the existing schema and family migrations listed in `SETUP.md`, run `database/upgrade_notifications.sql` in MySQL Workbench. This migration creates a new notifications table and triggers; it does not erase existing records. For a different database name, change the `USE crestline_db;` line before running it.

**Not included in this package:** automatic monthly EMI debit scheduling. It needs a tested, idempotent payment scheduler and a defined policy for insufficient balances; the current build continues to process repayments when the customer initiates them.
