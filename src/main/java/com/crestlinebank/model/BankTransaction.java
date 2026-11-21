package com.crestlinebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankTransaction(
        int id, String type, BigDecimal amount, String description, LocalDateTime createdAt) {}
