package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import java.sql.*;
import java.util.*;

/** Persistent in-app inbox. recipientType: ADMIN, CUSTOMER, CHILD. */
public class NotificationDao {
    public List<Object[]> inbox(String recipientType, long recipientId) throws SQLException {
        String sql = "SELECT id,title,message,created_at,is_read FROM notifications WHERE recipient_type=? AND recipient_id=? ORDER BY created_at DESC LIMIT 200";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,recipientType); p.setLong(2,recipientId);
            try(ResultSet r=p.executeQuery()) { while(r.next()) rows.add(new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getTimestamp(4),r.getBoolean(5)}); }
        }
        return rows;
    }
    public int unreadCount(String type,long id) throws SQLException {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM notifications WHERE recipient_type=? AND recipient_id=? AND is_read=0")){
            p.setString(1,type);p.setLong(2,id);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    public void markRead(long id,String type,long recipientId) throws SQLException {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE notifications SET is_read=1 WHERE id=? AND recipient_type=? AND recipient_id=?")){
            p.setLong(1,id);p.setString(2,type);p.setLong(3,recipientId);p.executeUpdate();
        }
    }
    public void markAllRead(String type,long id) throws SQLException {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE notifications SET is_read=1 WHERE recipient_type=? AND recipient_id=?")){
            p.setString(1,type);p.setLong(2,id);p.executeUpdate();
        }
    }
}
