package com.sararahmani.site.backend.dto;

public record RegisterRequest(
        String email,
        String password,
        String nom,
        String prenom,
        String role  // "ADMIN" ou "PATIENT"
) {}
