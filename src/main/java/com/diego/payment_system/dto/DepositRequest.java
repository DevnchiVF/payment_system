package com.diego.payment_system.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull
        BigDecimal amount
) {}
