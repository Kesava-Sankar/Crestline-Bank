package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import java.math.BigDecimal;
import java.sql.*;

public class AccountDao {
    public BigDecimal getBalance(int customerId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE customer_id=? AND status='ACTIVE'";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Active account was not found.");
                return rs.getBigDecimal("balance");
            }
        }
    }

    public String findAccountOwner(String accountNumber) throws SQLException {
        String sql = "SELECT c.full_name FROM accounts a JOIN customers c ON c.id=a.customer_id WHERE a.account_number=? AND a.status='ACTIVE'";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
