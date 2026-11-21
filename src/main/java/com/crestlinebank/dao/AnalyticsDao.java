package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import java.sql.*;
import java.util.*;

public class AnalyticsDao {
    public Object[] totals() throws SQLException {
        String q="SELECT (SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='DEPOSIT'),"+
          "(SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='WITHDRAWAL'),"+
          "(SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='TRANSFER_OUT'),"+
          "(SELECT COUNT(*) FROM customers),"+
          "(SELECT COUNT(*) FROM loans WHERE status='APPROVED'),"+
          "(SELECT COALESCE(SUM(outstanding),0) FROM loans WHERE status='APPROVED')";
        try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)) {r.next();return new Object[]{r.getBigDecimal(1),r.getBigDecimal(2),r.getBigDecimal(3),r.getInt(4),r.getInt(5),r.getBigDecimal(6)};}
    }
    public List<Object[]> monthly() throws SQLException {
        String q="SELECT DATE_FORMAT(created_at,'%Y-%m') month_label,"+
          "SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) deposits,"+
          "SUM(CASE WHEN type='WITHDRAWAL' THEN amount ELSE 0 END) withdrawals,"+
          "SUM(CASE WHEN type='TRANSFER_OUT' THEN amount ELSE 0 END) transfers,"+
          "COUNT(*) transaction_count FROM transactions WHERE created_at >= DATE_SUB(CURRENT_DATE,INTERVAL 12 MONTH) GROUP BY DATE_FORMAT(created_at,'%Y-%m') ORDER BY month_label";
        List<Object[]> out=new ArrayList<>();try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){while(r.next())out.add(new Object[]{r.getString(1),r.getBigDecimal(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getInt(5)});}return out;
    }

    public int[] loanStatusCounts() throws SQLException { return counts("SELECT status,COUNT(*) FROM loans GROUP BY status", new String[]{"APPROVED","PENDING","REJECTED"}); }
    public int[] repaymentProgressCounts() throws SQLException {
        String q="SELECT COALESCE(SUM(CASE WHEN status='PAID' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN status='APPROVED' THEN 1 ELSE 0 END),0) FROM loans";
        try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){r.next();return new int[]{r.getInt(1),r.getInt(2)};}
    }
    public java.math.BigDecimal[] interestTotals() throws SQLException {
        String q="SELECT COALESCE(SUM(interest_paid),0),COALESCE(SUM(GREATEST(total_interest-interest_paid,0)),0) FROM loans WHERE status IN ('APPROVED','PAID')";
        try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){r.next();return new java.math.BigDecimal[]{r.getBigDecimal(1),r.getBigDecimal(2)};}
    }
    private int[] counts(String q,String[] labels) throws SQLException {
        int[] out=new int[labels.length]; try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){while(r.next()){String label=r.getString(1);for(int i=0;i<labels.length;i++)if(labels[i].equals(label))out[i]=r.getInt(2);}} return out;
    }
}
