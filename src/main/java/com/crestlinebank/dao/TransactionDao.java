package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import com.crestlinebank.model.BankTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    public List<BankTransaction> findRecent(int customerId) throws SQLException {
        String sql = """
                SELECT t.id, t.type, t.amount, t.description, t.created_at
                FROM transactions t
                JOIN accounts a ON a.id=t.account_id
                WHERE a.customer_id=?
                ORDER BY t.created_at DESC, t.id DESC
                LIMIT 100
                """;
        List<BankTransaction> result = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    result.add(new BankTransaction(rs.getInt("id"), rs.getString("type"),
                            rs.getBigDecimal("amount"), rs.getString("description"),
                            timestamp.toLocalDateTime()));
                }
            }
        }
        return result;
    }
}
