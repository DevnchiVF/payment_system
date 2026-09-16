package com.diego.payment_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ClientRequest(
        @NotBlank
        String dni,

        @NotBlank
        String name,

        @NotNull
        LocalDate birthDate,

        @NotBlank
        String phoneNumber,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {}
