package com.diego.payment_system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount
) {}
