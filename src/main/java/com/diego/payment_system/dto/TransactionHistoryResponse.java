package com.diego.payment_system.dto;

import com.diego.payment_system.domain.TransactionStatus;
import com.diego.payment_system.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String direction,      // "INBOUND" o "OUTBOUND"
        LocalDateTime timestamp
) {}
