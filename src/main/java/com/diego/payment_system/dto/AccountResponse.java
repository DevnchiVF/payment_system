package com.diego.payment_system.dto;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        ClientResponse clientResponse,
        BigDecimal balance
) {}
