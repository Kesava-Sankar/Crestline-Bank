package com.crestlinebank.dao;

import com.crestlinebank.config.Database;
import com.crestlinebank.model.Loan;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class CustomerLoanDao {
    public List<Loan> findForCustomer(int customerId) throws SQLException {
        String q="SELECT l.id,l.customer_id,c.full_name,a.account_number,l.principal,l.outstanding,l.annual_rate,l.tenure_months,l.status,l.created_at FROM loans l JOIN customers c ON c.id=l.customer_id LEFT JOIN accounts a ON a.customer_id=c.id WHERE l.customer_id=? ORDER BY l.created_at DESC";
        List<Loan> out=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,customerId);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Loan(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getInt(8),r.getString(9),r.getTimestamp(10).toLocalDateTime()));}}return out;
    }
    public void request(int customerId, BigDecimal amount, BigDecimal rate, int months) throws SQLException {
        if(amount==null||amount.signum()<=0||rate==null||rate.signum()<0||months<1||months>360)
            throw new IllegalArgumentException("Enter a valid loan amount, interest rate and tenure.");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(
                "INSERT INTO loans(customer_id,principal,outstanding,annual_rate,tenure_months,status) VALUES(?,?,?,?,?,'PENDING')")) {
            p.setInt(1,customerId);p.setBigDecimal(2,amount);p.setBigDecimal(3,amount);p.setBigDecimal(4,rate);p.setInt(5,months);p.executeUpdate();
        }
    }
    public void repay(int customerId,int loanId,BigDecimal amount) throws SQLException {
        if(amount==null||amount.signum()<=0||amount.scale()>2)throw new IllegalArgumentException("Enter a valid repayment amount (up to 2 decimals).");
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            int accountId;BigDecimal balance;try(PreparedStatement p=c.prepareStatement("SELECT id,balance FROM accounts WHERE customer_id=? AND status='ACTIVE' FOR UPDATE")){p.setInt(1,customerId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Active account not found.");accountId=r.getInt(1);balance=r.getBigDecimal(2);}}
            BigDecimal principal,rate,interestPaid,totalInterest;try(PreparedStatement p=c.prepareStatement("SELECT outstanding,annual_rate,interest_paid,total_interest FROM loans WHERE id=? AND customer_id=? AND status='APPROVED' FOR UPDATE")){p.setInt(1,loanId);p.setInt(2,customerId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Approved loan not found.");principal=r.getBigDecimal(1);rate=r.getBigDecimal(2);interestPaid=r.getBigDecimal(3);totalInterest=r.getBigDecimal(4);}}
            BigDecimal periodInterest=principal.multiply(rate).divide(new BigDecimal("1200"),2,java.math.RoundingMode.HALF_UP);
            BigDecimal interestDue=totalInterest.subtract(interestPaid).max(BigDecimal.ZERO).min(periodInterest);
            BigDecimal maxPayment=principal.add(interestDue);
            if(amount.compareTo(maxPayment)>0)throw new IllegalArgumentException("Repayment exceeds current principal plus accrued interest ("+maxPayment+").");
            if(balance.compareTo(amount)<0)throw new IllegalArgumentException("Insufficient account balance.");
            BigDecimal interestComponent=amount.min(interestDue); BigDecimal principalComponent=amount.subtract(interestComponent).min(principal);
            try(PreparedStatement p=c.prepareStatement("UPDATE accounts SET balance=balance-? WHERE id=?")){p.setBigDecimal(1,amount);p.setInt(2,accountId);p.executeUpdate();}
            BigDecimal left=principal.subtract(principalComponent).max(BigDecimal.ZERO); BigDecimal paidInterest=interestPaid.add(interestComponent);
            try(PreparedStatement p=c.prepareStatement("UPDATE loans SET outstanding=?,principal_paid=principal_paid+?,interest_paid=?,status=?,next_due_date=CASE WHEN ?=0 THEN NULL ELSE DATE_ADD(CURRENT_DATE,INTERVAL 1 MONTH) END WHERE id=?")){p.setBigDecimal(1,left);p.setBigDecimal(2,principalComponent);p.setBigDecimal(3,paidInterest);p.setString(4,left.signum()==0?"PAID":"APPROVED");p.setInt(5,left.signum());p.setInt(6,loanId);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO loan_repayments(loan_id,amount) VALUES(?,?)")){p.setInt(1,loanId);p.setBigDecimal(2,amount);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO transactions(account_id,type,amount,description) VALUES(?,'WITHDRAWAL',? ,?)")){p.setInt(1,accountId);p.setBigDecimal(2,amount);p.setString(3,"Loan repayment #"+loanId+" (interest "+interestComponent+", principal "+principalComponent+")");p.executeUpdate();}
            c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalArgumentException ia)throw ia;throw new SQLException(e);}finally{c.setAutoCommit(true);}}
    }
}
