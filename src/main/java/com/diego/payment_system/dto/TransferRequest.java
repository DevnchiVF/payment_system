package com.diego.payment_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank
        String phoneNumber,

        @NotNull
        BigDecimal amount
) {}
