package com.sararahmani.site.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String message = "Email ou mot de passe incorrect.";

        // Si c'est un compte désactivé, on récupère le message custom
        if (authException instanceof org.springframework.security.authentication.DisabledException
                || authException.getCause() instanceof org.springframework.security.authentication.DisabledException) {
            message = authException.getCause() != null ? authException.getCause().getMessage() : authException.getMessage();
        }

        Map<String, Object> error = Map.of(
                "success", false,
                "message", message
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), error);
    }
}

