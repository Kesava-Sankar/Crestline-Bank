package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import com.crestlinebank.model.Loan;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class AdminManagementDao {
    public List<Object[]> customers() throws SQLException {
        String q="SELECT c.id,c.full_name,c.email,a.account_number,a.balance,a.status,c.created_at FROM customers c LEFT JOIN accounts a ON a.customer_id=c.id ORDER BY c.id DESC";
        List<Object[]> out=new ArrayList<>(); try(Connection c=Database.getConnection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(q)) {
            while(r.next()) out.add(new Object[]{r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getString(6),r.getTimestamp(7)});
        } return out;
    }
    public List<Object[]> transactions() throws SQLException {
        String q="SELECT t.id,c.full_name,a.account_number,t.type,t.amount,t.description,CASE WHEN t.amount >= 100000 THEN 'REVIEW' ELSE 'NORMAL' END AS risk_flag,t.created_at FROM transactions t JOIN accounts a ON a.id=t.account_id JOIN customers c ON c.id=a.customer_id ORDER BY t.created_at DESC,t.id DESC LIMIT 500";
        List<Object[]> out=new ArrayList<>(); try(Connection c=Database.getConnection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(q)) { while(r.next()) out.add(new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getString(6),r.getString(7),r.getTimestamp(8)}); } return out;
    }
    public void setAccountStatus(String accountNumber,String status,int adminId,String reason) throws SQLException {
        if(!Set.of("ACTIVE","FROZEN","CLOSED").contains(status)) throw new IllegalArgumentException("Invalid account status.");
        if(reason==null || reason.trim().length()<5) throw new IllegalArgumentException("Enter a reason (at least 5 characters) for the account action.");
        try(Connection c=Database.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("UPDATE accounts SET status=? WHERE account_number=?")) {
                p.setString(1,status); p.setString(2,accountNumber); if(p.executeUpdate()!=1) throw new SQLException("Account not found.");
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO account_audit_log(account_number,admin_id,action,reason) VALUES(?,?,?,?)")) {
                p.setString(1,accountNumber); p.setInt(2,adminId); p.setString(3,status); p.setString(4,reason.trim()); p.executeUpdate();
            }
            c.commit();
        }
    }
    public List<Object[]> loanInterestDetails() throws SQLException {
        String q="SELECT l.id,c.full_name,l.principal,l.annual_rate,l.tenure_months,l.total_interest,l.interest_paid,(l.total_interest-l.interest_paid),l.next_due_date,l.status FROM loans l JOIN customers c ON c.id=l.customer_id ORDER BY l.id DESC";
        List<Object[]> out=new ArrayList<>(); try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)) {
            while(r.next()) out.add(new Object[]{r.getInt(1),r.getString(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getInt(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getBigDecimal(8),r.getDate(9),r.getString(10)});
        } return out;
    }
    public List<Object[]> accountAudit(String accountNumber) throws SQLException {
        String q="SELECT l.action,l.reason,COALESCE(a.full_name,'Unknown admin'),l.created_at FROM account_audit_log l LEFT JOIN admins a ON a.id=l.admin_id WHERE l.account_number=? ORDER BY l.created_at DESC";
        List<Object[]> out=new ArrayList<>(); try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setString(1,accountNumber);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Object[]{r.getString(1),r.getString(2),r.getString(3),r.getTimestamp(4)});}} return out;
    }
    public void createLoan(int customerId, BigDecimal amount, BigDecimal rate, int months) throws SQLException {
        if(amount==null||amount.signum()<=0||rate==null||rate.signum()<0||months<1||months>360) throw new IllegalArgumentException("Enter valid loan amount, interest rate and tenure.");
        String q="INSERT INTO loans(customer_id,principal,outstanding,annual_rate,tenure_months,status) VALUES(?,?,?,?,?,'PENDING')";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,customerId);p.setBigDecimal(2,amount);p.setBigDecimal(3,amount);p.setBigDecimal(4,rate);p.setInt(5,months);p.executeUpdate();}
    }
    public List<Loan> loans() throws SQLException {
        String q="SELECT l.id,l.customer_id,c.full_name,a.account_number,l.principal,l.outstanding,l.annual_rate,l.tenure_months,l.status,l.created_at FROM loans l JOIN customers c ON c.id=l.customer_id LEFT JOIN accounts a ON a.customer_id=c.id ORDER BY l.created_at DESC";
        List<Loan> out=new ArrayList<>();try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){while(r.next())out.add(new Loan(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getInt(8),r.getString(9),r.getTimestamp(10).toLocalDateTime()));}return out;
    }
    public void decideLoan(int loanId, String decision) throws SQLException {
        if (!Set.of("APPROVED", "REJECTED").contains(decision)) {
            throw new IllegalArgumentException("Invalid decision.");
        }

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                int customerId;
                BigDecimal principal;
                BigDecimal rate;
                int months;

                // Lock the pending loan so it cannot be approved twice concurrently.
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT customer_id, principal, annual_rate, tenure_months " +
                        "FROM loans WHERE id=? AND status='PENDING' FOR UPDATE")) {
                    p.setInt(1, loanId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) {
                            throw new SQLException("Pending loan not found. It may already have been processed.");
                        }
                        customerId = r.getInt("customer_id");
                        principal = r.getBigDecimal("principal");
                        rate = r.getBigDecimal("annual_rate");
                        months = r.getInt("tenure_months");
                    }
                }

                BigDecimal totalInterest = BigDecimal.ZERO;
                if ("APPROVED".equals(decision)) {
                    double monthlyRate = rate.doubleValue() / 1200.0;
                    double power = Math.pow(1.0 + monthlyRate, months);
                    double monthlyPayment = monthlyRate == 0.0
                            ? principal.doubleValue() / months
                            : principal.doubleValue() * monthlyRate * power / (power - 1.0);
                    totalInterest = BigDecimal.valueOf(monthlyPayment * months)
                            .subtract(principal)
                            .max(BigDecimal.ZERO)
                            .setScale(2, java.math.RoundingMode.HALF_UP);

                    int accountId;
                    // Lock the customer's active account before crediting the loan.
                    try (PreparedStatement p = c.prepareStatement(
                            "SELECT id FROM accounts WHERE customer_id=? AND status='ACTIVE' FOR UPDATE")) {
                        p.setInt(1, customerId);
                        try (ResultSet r = p.executeQuery()) {
                            if (!r.next()) {
                                throw new SQLException("Cannot disburse loan: customer has no active account.");
                            }
                            accountId = r.getInt("id");
                        }
                    }

                    // Credit the approved principal to the customer's account.
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE accounts SET balance = balance + ? WHERE id=? AND status='ACTIVE'")) {
                        p.setBigDecimal(1, principal);
                        p.setInt(2, accountId);
                        if (p.executeUpdate() != 1) {
                            throw new SQLException("Loan disbursement failed: account balance was not updated.");
                        }
                    }

                    // Record the credit in the customer's transaction history.
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO transactions(account_id, type, amount, description) " +
                            "VALUES(?, 'DEPOSIT', ?, ?)")) {
                        p.setInt(1, accountId);
                        p.setBigDecimal(2, principal);
                        p.setString(3, "Loan disbursement #" + loanId);
                        p.executeUpdate();
                    }
                }

                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE loans SET status=?, total_interest=?, " +
                        "next_due_date=CASE WHEN ?='APPROVED' " +
                        "THEN DATE_ADD(CURRENT_DATE, INTERVAL 1 MONTH) ELSE NULL END " +
                        "WHERE id=? AND status='PENDING'")) {
                    p.setString(1, decision);
                    p.setBigDecimal(2, totalInterest);
                    p.setString(3, decision);
                    p.setInt(4, loanId);
                    if (p.executeUpdate() != 1) {
                        throw new SQLException("Loan status could not be updated.");
                    }
                }

                c.commit();
            } catch (Exception ex) {
                try {
                    c.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }
                if (ex instanceof SQLException sqlException) throw sqlException;
                if (ex instanceof RuntimeException runtimeException) throw runtimeException;
                throw new SQLException("Loan decision failed.", ex);
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Connection is closed immediately after this method.
                }
            }
        }
    }
    public List<Object[]> repayments() throws SQLException {
        String q="SELECT r.id,r.loan_id,c.full_name,r.amount,r.paid_at FROM loan_repayments r JOIN loans l ON l.id=r.loan_id JOIN customers c ON c.id=l.customer_id ORDER BY r.paid_at DESC LIMIT 500";
        List<Object[]> out=new ArrayList<>();try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){while(r.next())out.add(new Object[]{r.getLong(1),r.getInt(2),r.getString(3),r.getBigDecimal(4),r.getTimestamp(5)});}return out;
    }
    public Object[] metrics() throws SQLException {
        String q="SELECT (SELECT COUNT(*) FROM customers),(SELECT COUNT(*) FROM accounts WHERE status='ACTIVE'),(SELECT COALESCE(SUM(balance),0) FROM accounts WHERE status='ACTIVE'),(SELECT COUNT(*) FROM transactions),(SELECT COUNT(*) FROM loans WHERE status='PENDING')";
        try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){r.next();return new Object[]{r.getInt(1),r.getInt(2),r.getBigDecimal(3),r.getInt(4),r.getInt(5)};}
    }
}
