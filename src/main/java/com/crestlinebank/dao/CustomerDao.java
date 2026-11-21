package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import com.crestlinebank.model.Customer;
import com.crestlinebank.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class CustomerDao {
    public Optional<Customer> authenticate(String email, String password) throws SQLException {
        String sql = """
                SELECT c.id, c.full_name, c.email, a.account_number, c.password_hash
                FROM customers c
                JOIN accounts a ON a.customer_id = c.id
                WHERE c.email = ? AND a.status = 'ACTIVE'
                """;
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                if (!PasswordUtil.matches(password, rs.getString("password_hash"))) return Optional.empty();
                return Optional.of(new Customer(rs.getInt("id"), rs.getString("full_name"),
                        rs.getString("email"), rs.getString("account_number")));
            }
        }
    }

    public Customer register(String fullName, String email, String password) throws SQLException {
        String accountNumber = "CB" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase();
        String insertCustomer = "INSERT INTO customers(full_name,email,password_hash) VALUES(?,?,?)";
        String insertAccount = "INSERT INTO accounts(customer_id,account_number,balance,status) VALUES(?,?,?, 'ACTIVE')";
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int customerId;
                try (PreparedStatement ps = con.prepareStatement(insertCustomer, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, fullName.trim());
                    ps.setString(2, email.trim().toLowerCase());
                    ps.setString(3, PasswordUtil.hash(password));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create customer record.");
                        customerId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(insertAccount)) {
                    ps.setInt(1, customerId);
                    ps.setString(2, accountNumber);
                    ps.setBigDecimal(3, BigDecimal.ZERO);
                    ps.executeUpdate();
                }
                con.commit();
                return new Customer(customerId, fullName.trim(), email.trim().toLowerCase(), accountNumber);
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
