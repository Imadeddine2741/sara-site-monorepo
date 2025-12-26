package com.sararahmani.site.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull @FutureOrPresent
        LocalDateTime dateHeure,

        @NotBlank @Size(max = 500)
        String motif
) {}

