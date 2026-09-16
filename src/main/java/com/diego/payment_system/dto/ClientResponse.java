package com.diego.payment_system.dto;

import java.time.LocalDate;

public record ClientResponse(
        Long id,
        String dni,
        String name,
        LocalDate birthDate,
        String phoneNumber,
        String email
) {}
