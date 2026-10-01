package com.diego.payment_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionFeatureResponse(
        Long transactionId,
        BigDecimal amount,
        int hourOfDay,
        long recentTxCount,
        BigDecimal amountToAvgRatio,
        LocalDateTime createdAt
) {}