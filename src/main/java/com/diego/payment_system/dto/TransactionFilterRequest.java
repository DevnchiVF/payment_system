package com.diego.payment_system.dto;

import com.diego.payment_system.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionFilterRequest(
        LocalDateTime from,
        LocalDateTime to,
        TransactionType type,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {}