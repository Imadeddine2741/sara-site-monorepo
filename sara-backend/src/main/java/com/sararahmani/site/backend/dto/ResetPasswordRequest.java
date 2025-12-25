package com.sararahmani.site.backend.dto;

public record ResetPasswordRequest(String token, String newPassword) {}