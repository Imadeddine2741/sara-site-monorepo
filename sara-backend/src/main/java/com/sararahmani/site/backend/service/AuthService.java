package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.dto.AuthRequest;
import com.sararahmani.site.backend.dto.AuthResponse;
import com.sararahmani.site.backend.dto.RegisterRequest;
import com.sararahmani.site.backend.entity.Role;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public AuthResponse register(RegisterRequest request) {
        Role role = Role.valueOf(request.role().toUpperCase());

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nom(request.nom())
                .prenom(request.prenom())
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of("role", user.getRole().name())
        );

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getRole().name()
        );
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow();

        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of("role", user.getRole().name())
        );

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getRole().name()
        );
    }
}

