package com.sararahmani.site.backend.dto;

public record PatientDto(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone
) {}

