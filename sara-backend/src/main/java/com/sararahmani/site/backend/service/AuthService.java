package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.dto.AuthRequest;
import com.sararahmani.site.backend.dto.AuthResponse;
import com.sararahmani.site.backend.dto.RegisterRequest;
import com.sararahmani.site.backend.entity.EmailConfirmation;
import com.sararahmani.site.backend.entity.PasswordResetToken;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.exception.EmailAlreadyExistsException;
import com.sararahmani.site.backend.repository.EmailConfirmationRepository;
import com.sararahmani.site.backend.repository.PasswordResetTokenRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import com.sararahmani.site.backend.service.mapper.UserMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final MailService mailService;
    private final EmailConfirmationRepository emailConfirmationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;


    public AuthResponse register(RegisterRequest request) {

        // 1. Vérifier si email existe déjà
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Cette adresse email est déjà utilisée.");
        }

        User user = userMapper.fromRegister(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(false);

        userRepository.save(user);

        // Générer et sauvegarder le token de confirmation
        String confirmationToken = UUID.randomUUID().toString();
        EmailConfirmation emailConfirmation = EmailConfirmation.builder()
                .token(confirmationToken)
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        emailConfirmationRepository.save(emailConfirmation);

        // Envoyer l'email de confirmation
        mailService.sendConfirmationEmail(user.getEmail(), confirmationToken);

        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of("role", user.getRole().name())
        );

        return userMapper.toAuthResponse(user, token);
    }

    public String confirmEmail(String token) {
        EmailConfirmation confirmation = emailConfirmationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (confirmation.isUsed()) {
            return "Ce lien a déjà été utilisé.";
        }

        if (confirmation.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "Ce lien a expiré.";
        }

        User user = userRepository.findByEmail(confirmation.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        user.setEnabled(true);
        userRepository.save(user);

        confirmation.setUsed(true);
        emailConfirmationRepository.save(confirmation);

        return "Votre compte a été activé avec succès !";
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

        return userMapper.toAuthResponse(user, token);
    }

    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        // Pour des raisons de sécurité, on retourne toujours le même message
        // même si l'email n'existe pas (évite l'énumération des comptes)
        if (user == null) {
            return "Si cette adresse email existe, un lien de réinitialisation vous a été envoyé.";
        }

        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(resetToken)
                .email(email)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.save(passwordResetToken);

        mailService.sendPasswordResetEmail(email, resetToken);

        return "Si cette adresse email existe, un lien de réinitialisation vous a été envoyé.";
    }

    public String resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Ce lien a déjà été utilisé.");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ce lien a expiré.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return "Votre mot de passe a été réinitialisé avec succès !";
    }
}
