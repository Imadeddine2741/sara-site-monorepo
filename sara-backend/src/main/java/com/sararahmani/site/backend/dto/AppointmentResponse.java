package com.sararahmani.site.backend.dto;


import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime dateHeure,

        String motif,

        String patientNom,
        String patientPrenom,

        String status,

        boolean canPatientCancel
) {}

