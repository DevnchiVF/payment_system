package com.diego.payment_system.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCompletedEvent(
        Long transactionId,
        Long sourceAccountId,
        BigDecimal amount,
        LocalDateTime timestamp
) {}