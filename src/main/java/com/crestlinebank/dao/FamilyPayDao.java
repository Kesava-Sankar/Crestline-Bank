package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Parent-owned family UPI identities for the Crestline educational simulation. */
public class FamilyPayDao {

    public String parentUpi(int parentId) throws SQLException {
        String q="SELECT parent_upi_id FROM family_parent_upi WHERE parent_customer_id=?";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,parentId);try(ResultSet r=p.executeQuery()){if(r.next())return r.getString(1);}}
        String base="parent"+parentId+"@crestline";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO family_parent_upi(parent_customer_id,parent_upi_id) VALUES(?,?)")){p.setInt(1,parentId);p.setString(2,base);p.executeUpdate();}
        return base;
    }
    public void setMerchantBlocked(long profileId,String merchant,boolean blocked) throws SQLException {
        if(merchant==null||merchant.trim().length()<2)throw new IllegalArgumentException("Enter the game, website or merchant name / UPI ID.");
        String key=merchant.trim().toLowerCase(Locale.ROOT);
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO family_blocked_merchants(profile_id,merchant_key,display_name,blocked) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),blocked=VALUES(blocked)")){p.setLong(1,profileId);p.setString(2,key);p.setString(3,merchant.trim());p.setBoolean(4,blocked);p.executeUpdate();}
        audit(profileId,blocked?"MERCHANT_BLOCKED":"MERCHANT_UNBLOCKED",merchant.trim());
    }
    public List<Object[]> blockedMerchants(long profileId) throws SQLException {
        List<Object[]> out=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT display_name,blocked FROM family_blocked_merchants WHERE profile_id=? ORDER BY display_name")){p.setLong(1,profileId);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Object[]{r.getString(1),r.getBoolean(2)});}}return out;
    }

    public List<Object[]> children(int parentId) throws SQLException {
        String q="SELECT id,child_name,child_mobile,child_upi_id,daily_limit,per_txn_limit,status FROM family_upi_profiles WHERE parent_customer_id=? ORDER BY id DESC";
        List<Object[]> out=new ArrayList<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,parentId);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getBigDecimal(6),r.getString(7)});}}
        return out;
    }
    public void addChild(int parentId,String name,String mobile,String upi,String passwordHash,BigDecimal daily,BigDecimal perTxn)throws SQLException{
        if(name==null||name.trim().length()<2)throw new IllegalArgumentException("Enter the child's name.");
        if(passwordHash==null||passwordHash.isBlank())throw new IllegalArgumentException("Set a child login password.");
        if(mobile==null||!mobile.matches("[6-9][0-9]{9}"))throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number for the child's device.");
        if(upi==null||!upi.matches("[A-Za-z0-9._-]{2,50}@[A-Za-z]{2,20}"))throw new IllegalArgumentException("Use a demo UPI ID such as child@crestline.");
        validateLimits(daily,perTxn);
        String q="INSERT INTO family_upi_profiles(parent_customer_id,child_name,child_mobile,child_upi_id,login_password_hash,daily_limit,per_txn_limit) VALUES(?,?,?,?,?,?,?)";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,parentId);p.setString(2,name.trim());p.setString(3,mobile.trim());p.setString(4,upi.trim().toLowerCase(Locale.ROOT));p.setString(5,passwordHash);p.setBigDecimal(6,daily);p.setBigDecimal(7,perTxn);p.executeUpdate();}
    }
    public void setChildPassword(int parent,long id,String passwordHash)throws SQLException {
        if(passwordHash==null||passwordHash.isBlank())throw new IllegalArgumentException("Set a valid child password.");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE family_upi_profiles SET login_password_hash=? WHERE id=? AND parent_customer_id=?")){
            p.setString(1,passwordHash);p.setLong(2,id);p.setInt(3,parent);if(p.executeUpdate()!=1)throw new SQLException("Child profile not found.");
        }
    }
    public void setLimits(int parent,long id,BigDecimal daily,BigDecimal perTxn)throws SQLException{
        validateLimits(daily,perTxn);try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE family_upi_profiles SET daily_limit=?,per_txn_limit=? WHERE id=? AND parent_customer_id=?")){p.setBigDecimal(1,daily);p.setBigDecimal(2,perTxn);p.setLong(3,id);p.setInt(4,parent);if(p.executeUpdate()!=1)throw new SQLException("Child UPI profile not found.");} audit(id,"LIMITS_CHANGED","Daily="+daily+", per-payment="+perTxn);
    }
    public void setStatus(int parent,long id,String status)throws SQLException{
        if(!Set.of("ACTIVE","BLOCKED").contains(status))throw new IllegalArgumentException("Invalid UPI status.");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE family_upi_profiles SET status=? WHERE id=? AND parent_customer_id=?")){p.setString(1,status);p.setLong(2,id);p.setInt(3,parent);if(p.executeUpdate()!=1)throw new SQLException("Child UPI profile not found.");} audit(id,status,"Parent changed child UPI access.");
    }
    public void demoPayment(int parent,long id,String recipient,BigDecimal amount,String note)throws SQLException{
        if(recipient==null||recipient.isBlank()||amount==null||amount.signum()<=0)throw new IllegalArgumentException("Enter a valid recipient and positive amount.");
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            BigDecimal daily,per;String status;
            try(PreparedStatement p=c.prepareStatement("SELECT daily_limit,per_txn_limit,status FROM family_upi_profiles WHERE id=? AND parent_customer_id=? FOR UPDATE")){p.setLong(1,id);p.setInt(2,parent);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Child UPI profile not found.");daily=r.getBigDecimal(1);per=r.getBigDecimal(2);status=r.getString(3);}}
            String result="SUCCESS",reason="";
            try(PreparedStatement b=c.prepareStatement("SELECT display_name FROM family_blocked_merchants WHERE profile_id=? AND blocked=1 AND merchant_key=?")){b.setLong(1,id);b.setString(2,recipient.trim().toLowerCase(Locale.ROOT));try(ResultSet br=b.executeQuery()){if(br.next()){result="BLOCKED";reason="Parent blocked this specific game / website / merchant: "+br.getString(1);}}}
            if(!"SUCCESS".equals(result)) { /* retain merchant-specific block */ }
            else if(!"ACTIVE".equals(status)){result="BLOCKED";reason="Child UPI identity is blocked.";}
            else if(amount.compareTo(per)>0){result="BLOCKED";reason="Per-payment limit exceeded.";}
            else {
                BigDecimal spent;
                try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM family_upi_transactions WHERE profile_id=? AND status='SUCCESS' AND DATE(created_at)=CURRENT_DATE")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){r.next();spent=r.getBigDecimal(1);}}
                if(spent.add(amount).compareTo(daily)>0){result="BLOCKED";reason="Daily spending limit exceeded.";}
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO family_upi_transactions(profile_id,parent_customer_id,recipient,amount,status,description) VALUES(?,?,?,?,?,?)")){p.setLong(1,id);p.setInt(2,parent);p.setString(3,recipient.trim());p.setBigDecimal(4,amount);p.setString(5,result);p.setString(6,(note==null?"":note)+ (reason.isEmpty()?"":" | "+reason));p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO family_upi_audit_log(profile_id,action,details) VALUES(?,?,?)")){p.setLong(1,id);p.setString(2,result);p.setString(3,"₹"+amount+" to "+recipient+". "+reason);p.executeUpdate();}
            c.commit();
            if(!"SUCCESS".equals(result))throw new SQLException(reason);
        }
    }
    public Optional<ChildSession> authenticateChild(String upi, String password) throws SQLException {
        String q="SELECT id,parent_customer_id,child_name,child_upi_id,login_password_hash,daily_limit,per_txn_limit,status FROM family_upi_profiles WHERE LOWER(child_upi_id)=LOWER(?)";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){
            p.setString(1,upi.trim()); try(ResultSet r=p.executeQuery()){
                if(!r.next() || !"ACTIVE".equals(r.getString("status"))) return Optional.empty();
                String hash=r.getString("login_password_hash");
                if(hash==null || !com.crestlinebank.util.PasswordUtil.matches(password,hash)) return Optional.empty();
                return Optional.of(new ChildSession(r.getLong("id"),r.getInt("parent_customer_id"),r.getString("child_name"),r.getString("child_upi_id"),r.getBigDecimal("daily_limit"),r.getBigDecimal("per_txn_limit")));
            }
        }
    }
    public record ChildSession(long profileId,int parentId,String childName,String upi,BigDecimal dailyLimit,BigDecimal perTxnLimit) {}

    public BigDecimal parentBalance(int parentId) throws SQLException {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT balance FROM accounts WHERE customer_id=? AND status='ACTIVE'")){
            p.setInt(1,parentId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Parent account is not active.");return r.getBigDecimal(1);}
        }
    }
    public BigDecimal childSpentToday(long profileId) throws SQLException {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM family_upi_transactions WHERE profile_id=? AND status='SUCCESS' AND DATE(created_at)=CURRENT_DATE")){
            p.setLong(1,profileId);try(ResultSet r=p.executeQuery()){r.next();return r.getBigDecimal(1);}
        }
    }
    public List<Object[]> childTransactions(long profileId) throws SQLException {
        List<Object[]> out=new ArrayList<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT recipient,amount,status,description,created_at FROM family_upi_transactions WHERE profile_id=? ORDER BY created_at DESC LIMIT 200")){
            p.setLong(1,profileId);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Object[]{r.getString(1),r.getBigDecimal(2),r.getString(3),r.getString(4),r.getTimestamp(5)});}
        }return out;
    }
    public void childPayment(ChildSession child,String recipient,BigDecimal amount,String note) throws SQLException {
        if(recipient==null||recipient.isBlank()||amount==null||amount.signum()<=0)throw new IllegalArgumentException("Enter a valid recipient and amount.");
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try {
                BigDecimal daily,per;String status;
                try(PreparedStatement p=c.prepareStatement("SELECT daily_limit,per_txn_limit,status FROM family_upi_profiles WHERE id=? AND parent_customer_id=? FOR UPDATE")){
                    p.setLong(1,child.profileId());p.setInt(2,child.parentId());try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Child profile not found.");daily=r.getBigDecimal(1);per=r.getBigDecimal(2);status=r.getString(3);}
                }
                if(!"ACTIVE".equals(status))throw new SQLException("Parent has blocked this child UPI identity.");
                if(amount.compareTo(per)>0)throw new SQLException("Per-payment limit exceeded.");
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM family_blocked_merchants WHERE profile_id=? AND merchant_key=? AND blocked=1")){
                    p.setLong(1,child.profileId());p.setString(2,recipient.trim().toLowerCase(Locale.ROOT));try(ResultSet r=p.executeQuery()){if(r.next())throw new SQLException("This merchant is blocked by your parent.");}
                }
                BigDecimal spent;
                try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM family_upi_transactions WHERE profile_id=? AND status='SUCCESS' AND DATE(created_at)=CURRENT_DATE")){
                    p.setLong(1,child.profileId());try(ResultSet r=p.executeQuery()){r.next();spent=r.getBigDecimal(1);}
                }
                if(spent.add(amount).compareTo(daily)>0)throw new SQLException("Daily spending limit exceeded. Remaining today: ₹"+daily.subtract(spent));
                int accountId;BigDecimal balance;
                try(PreparedStatement p=c.prepareStatement("SELECT id,balance FROM accounts WHERE customer_id=? AND status='ACTIVE' FOR UPDATE")){
                    p.setInt(1,child.parentId());try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Parent bank account is unavailable.");accountId=r.getInt(1);balance=r.getBigDecimal(2);}
                }
                if(balance.compareTo(amount)<0)throw new SQLException("Insufficient funds in the linked parent account.");
                try(PreparedStatement p=c.prepareStatement("UPDATE accounts SET balance=balance-? WHERE id=?")){p.setBigDecimal(1,amount);p.setInt(2,accountId);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO transactions(account_id,type,amount,description) VALUES(?,'WITHDRAWAL',?,?)")){p.setInt(1,accountId);p.setBigDecimal(2,amount);p.setString(3,"Child UPI "+child.upi()+" to "+recipient+" | "+(note==null?"":note));p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO family_upi_transactions(profile_id,parent_customer_id,recipient,amount,status,description) VALUES(?,?,?,?,'SUCCESS',?)")){p.setLong(1,child.profileId());p.setInt(2,child.parentId());p.setString(3,recipient.trim());p.setBigDecimal(4,amount);p.setString(5,note);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO family_upi_audit_log(profile_id,action,details) VALUES(?,'CHILD_PAYMENT',?)")){p.setLong(1,child.profileId());p.setString(2,"₹"+amount+" to "+recipient+"; debited from parent account.");p.executeUpdate();}
                c.commit();
            } catch(Exception ex){c.rollback();if(ex instanceof SQLException se)throw se;if(ex instanceof RuntimeException re)throw re;throw new SQLException(ex);}
        }
    }

    public List<Object[]> transactions(int parentId) throws SQLException {
        String q="SELECT f.child_name,t.recipient,t.amount,t.status,t.description,t.created_at FROM family_upi_transactions t JOIN family_upi_profiles f ON f.id=t.profile_id WHERE t.parent_customer_id=? ORDER BY t.created_at DESC LIMIT 300";
        List<Object[]> out=new ArrayList<>(); try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,parentId);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Object[]{r.getString(1),r.getString(2),r.getBigDecimal(3),r.getString(4),r.getString(5),r.getTimestamp(6)});}} return out;
    }
    public void requestEmergency(int parent,long id,BigDecimal amount,String reason)throws SQLException{
        if(amount==null||amount.signum()<=0||reason==null||reason.trim().length()<4)throw new IllegalArgumentException("Enter a valid amount and reason.");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO family_spend_requests(profile_id,requested_amount,reason) SELECT id,?,? FROM family_upi_profiles WHERE id=? AND parent_customer_id=?")){p.setBigDecimal(1,amount);p.setString(2,reason.trim());p.setLong(3,id);p.setInt(4,parent);if(p.executeUpdate()!=1)throw new SQLException("Child profile not found.");}
        audit(id,"EMERGENCY_REQUEST","₹"+amount+" requested: "+reason);
    }
    public void approveLatestRequest(int parent,long id)throws SQLException{
        String q="UPDATE family_spend_requests r JOIN family_upi_profiles f ON f.id=r.profile_id SET r.status='APPROVED',r.reviewed_at=CURRENT_TIMESTAMP WHERE f.parent_customer_id=? AND f.id=? AND r.status='PENDING' ORDER BY r.id DESC LIMIT 1";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,parent);p.setLong(2,id);if(p.executeUpdate()!=1)throw new SQLException("No pending emergency request found.");}audit(id,"EMERGENCY_APPROVED","Parent approved a one-time request; normal caps unchanged.");
    }
    private void audit(long id,String action,String details)throws SQLException{try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO family_upi_audit_log(profile_id,action,details) VALUES(?,?,?)")){p.setLong(1,id);p.setString(2,action);p.setString(3,details);p.executeUpdate();}}
    private void validateLimits(BigDecimal daily,BigDecimal per){if(daily==null||per==null||daily.signum()<=0||per.signum()<=0||per.compareTo(daily)>0)throw new IllegalArgumentException("Limits must be positive and the per-payment limit cannot exceed the daily limit.");}
}
