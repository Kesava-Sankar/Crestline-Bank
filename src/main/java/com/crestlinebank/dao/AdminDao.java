package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import com.crestlinebank.model.Admin;
import com.crestlinebank.util.PasswordUtil;
import java.sql.*;
import java.util.Optional;

public class AdminDao {
    public boolean hasAdmins() throws SQLException {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM admins")) { r.next(); return r.getInt(1) > 0; }
    }
    public void createFirstAdmin(String name, String email, String password) throws SQLException {
        try (Connection c = Database.getConnection()) {
            boolean lockAcquired = false;
            try (PreparedStatement lock = c.prepareStatement("SELECT GET_LOCK('crestline_first_admin', 10)")) {
                try (ResultSet r = lock.executeQuery()) { lockAcquired = r.next() && r.getInt(1) == 1; }
            }
            if (!lockAcquired) throw new SQLException("Could not acquire administrator setup lock. Try again.");
            try {
                if (hasAdmins()) throw new SQLException("An administrator is already configured.");
                try (PreparedStatement p = c.prepareStatement("INSERT INTO admins(full_name,email,password_hash) VALUES(?,?,?)")) {
                    p.setString(1, name.trim());
                    p.setString(2, email.trim().toLowerCase());
                    p.setString(3, PasswordUtil.hash(password));
                    p.executeUpdate();
                }
            } finally {
                try (Statement release = c.createStatement()) { release.executeQuery("SELECT RELEASE_LOCK('crestline_first_admin')").close(); }
            }
        }
    }
    public Optional<Admin> authenticate(String email, String password) throws SQLException {
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement("SELECT id,full_name,email,password_hash FROM admins WHERE email=? AND status='ACTIVE'")) {
            p.setString(1,email.trim().toLowerCase()); try(ResultSet r=p.executeQuery()) {
                if(!r.next() || !PasswordUtil.matches(password,r.getString("password_hash"))) return Optional.empty();
                return Optional.of(new Admin(r.getInt("id"),r.getString("full_name"),r.getString("email")));
            }
        }
    }
}
