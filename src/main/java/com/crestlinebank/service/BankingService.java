package com.crestlinebank.service;

import com.crestlinebank.config.Database;
import com.crestlinebank.dao.AccountDao;
import com.crestlinebank.dao.CustomerDao;
import com.crestlinebank.dao.TransactionDao;
import com.crestlinebank.model.BankTransaction;
import com.crestlinebank.model.Customer;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

public class BankingService {
    private final CustomerDao customerDao = new CustomerDao();
    private final AccountDao accountDao = new AccountDao();
    private final TransactionDao transactionDao = new TransactionDao();

    public Optional<Customer> login(String email, String password) throws SQLException {
        return customerDao.authenticate(email, password);
    }

    public Customer register(String name, String email, String password) throws SQLException {
        if (name == null || name.trim().length() < 2) throw new IllegalArgumentException("Enter your full name.");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            throw new IllegalArgumentException("Enter a valid email address.");
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        return customerDao.register(name, email, password);
    }

    public BigDecimal balance(int customerId) throws SQLException {
        return accountDao.getBalance(customerId);
    }

    public String accountOwner(String accountNumber) throws SQLException {
        return accountDao.findAccountOwner(accountNumber);
    }

    public List<BankTransaction> history(int customerId) throws SQLException {
        return transactionDao.findRecent(customerId);
    }

    public void deposit(int customerId, BigDecimal amount) throws SQLException {
        validateAmount(amount);
        executeSingleAccountTransaction(customerId, amount, "DEPOSIT", "Cash deposit");
    }

    public void withdraw(int customerId, BigDecimal amount) throws SQLException {
        validateAmount(amount);
        executeSingleAccountTransaction(customerId, amount.negate(), "WITHDRAWAL", "Cash withdrawal");
    }

    private void executeSingleAccountTransaction(int customerId, BigDecimal delta,
                                                 String type, String description) throws SQLException {
        String lock = "SELECT id,balance FROM accounts WHERE customer_id=? AND status='ACTIVE' FOR UPDATE";
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int accountId;
                BigDecimal current;
                try (PreparedStatement ps = con.prepareStatement(lock)) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Active account not found.");
                        accountId = rs.getInt("id");
                        current = rs.getBigDecimal("balance");
                    }
                }
                BigDecimal updated = current.add(delta);
                if (updated.signum() < 0) throw new IllegalArgumentException("Insufficient funds.");
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance=? WHERE id=?")) {
                    ps.setBigDecimal(1, updated);
                    ps.setInt(2, accountId);
                    ps.executeUpdate();
                }
                insertTransaction(con, accountId, type, delta.abs(), description);
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                if (ex instanceof SQLException sqlEx) throw sqlEx;
                if (ex instanceof IllegalArgumentException iae) throw iae;
                throw new SQLException("Transaction failed.", ex);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public void transfer(int senderCustomerId, String receiverAccount, BigDecimal amount) throws SQLException {
        validateAmount(amount);
        String lockAccount = "SELECT id,customer_id,balance FROM accounts WHERE account_number=? AND status='ACTIVE' FOR UPDATE";
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int senderId;
                int receiverId;
                int receiverCustomerId;
                BigDecimal senderBalance;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT id,balance FROM accounts WHERE customer_id=? AND status='ACTIVE' FOR UPDATE")) {
                    ps.setInt(1, senderCustomerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Your active account was not found.");
                        senderId = rs.getInt("id");
                        senderBalance = rs.getBigDecimal("balance");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(lockAccount)) {
                    ps.setString(1, receiverAccount.trim().toUpperCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Recipient account was not found.");
                        receiverId = rs.getInt("id");
                        receiverCustomerId = rs.getInt("customer_id");
                    }
                }
                if (receiverCustomerId == senderCustomerId)
                    throw new IllegalArgumentException("You cannot transfer money to your own account.");
                if (senderBalance.compareTo(amount) < 0)
                    throw new IllegalArgumentException("Insufficient funds.");
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance=balance-? WHERE id=?")) {
                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, senderId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance=balance+? WHERE id=?")) {
                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, receiverId);
                    ps.executeUpdate();
                }
                insertTransaction(con, senderId, "TRANSFER_OUT", amount, "Transfer to " + receiverAccount.toUpperCase());
                insertTransaction(con, receiverId, "TRANSFER_IN", amount, "Transfer from Crestline customer");
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                if (ex instanceof SQLException sqlEx) throw sqlEx;
                if (ex instanceof IllegalArgumentException iae) throw iae;
                throw new SQLException("Transfer failed.", ex);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private void insertTransaction(Connection con, int accountId, String type,
                                   BigDecimal amount, String description) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO transactions(account_id,type,amount,description) VALUES(?,?,?,?)")) {
            ps.setInt(1, accountId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setString(4, description);
            ps.executeUpdate();
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero.");
        if (amount.scale() > 2) throw new IllegalArgumentException("Amount can have at most two decimal places.");
        if (amount.compareTo(new BigDecimal("10000000.00")) > 0)
            throw new IllegalArgumentException("Amount exceeds the demo transaction limit.");
    }
}
