package com.crestlinebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Loan(int id, int customerId, String customerName, String accountNumber,
                   BigDecimal principal, BigDecimal outstanding, BigDecimal annualRate,
                   int tenureMonths, String status, LocalDateTime createdAt) {}
