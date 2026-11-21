package com.crestlinebank.model;
import java.math.BigDecimal;
public record Staff(int id,String fullName,String email,String role,BigDecimal salary,BigDecimal maxLoanApproval) {}
