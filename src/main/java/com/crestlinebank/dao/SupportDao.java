package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import java.sql.*;
import java.util.*;

/** Customer-to-admin support inbox for the Crestline demo application. */
public class SupportDao {
    public void create(int customerId, String subject, String message) throws SQLException {
        if (subject == null || subject.isBlank() || message == null || message.isBlank())
            throw new IllegalArgumentException("Enter a subject and message.");
        if (subject.length() > 140 || message.length() > 2000)
            throw new IllegalArgumentException("Subject or message is too long.");
        String q = "INSERT INTO support_requests(customer_id,subject,message,status) VALUES(?,?,?,'OPEN')";
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setInt(1, customerId); p.setString(2, subject.trim()); p.setString(3, message.trim()); p.executeUpdate();
        }
    }
    public List<Object[]> forCustomer(int customerId) throws SQLException {
        String q="SELECT id,subject,message,COALESCE(admin_reply,''),status,created_at,updated_at FROM support_requests WHERE customer_id=? ORDER BY created_at DESC";
        List<Object[]> out=new ArrayList<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)) {
            p.setInt(1,customerId); try(ResultSet r=p.executeQuery()) { while(r.next()) out.add(new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getTimestamp(6),r.getTimestamp(7)}); }
        } return out;
    }
    public List<Object[]> all() throws SQLException {
        String q="SELECT s.id,c.full_name,c.email,s.subject,s.message,COALESCE(s.admin_reply,''),s.status,s.created_at FROM support_requests s JOIN customers c ON c.id=s.customer_id ORDER BY CASE s.status WHEN 'OPEN' THEN 0 WHEN 'REPLIED' THEN 1 ELSE 2 END,s.created_at DESC";
        List<Object[]> out=new ArrayList<>();
        try(Connection c=Database.getConnection();Statement p=c.createStatement();ResultSet r=p.executeQuery(q)) { while(r.next()) out.add(new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getTimestamp(8)}); }
        return out;
    }
    public void reply(long id,String reply) throws SQLException {
        if(reply==null||reply.isBlank()) throw new IllegalArgumentException("Enter a reply first.");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE support_requests SET admin_reply=?,status='REPLIED',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status<>'CLOSED'")) {
            p.setString(1,reply.trim());p.setLong(2,id);if(p.executeUpdate()!=1)throw new SQLException("Request not found or already closed.");
        }
    }
    public void close(long id) throws SQLException {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE support_requests SET status='CLOSED',updated_at=CURRENT_TIMESTAMP WHERE id=?")) {p.setLong(1,id);if(p.executeUpdate()!=1)throw new SQLException("Request not found.");}
    }
}
