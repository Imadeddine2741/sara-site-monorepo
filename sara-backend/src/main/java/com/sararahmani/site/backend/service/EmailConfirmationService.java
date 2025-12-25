package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.entity.EmailConfirmation;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.repository.EmailConfirmationRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EmailConfirmationService {

    private final EmailConfirmationRepository repository;

    public String createTokenForUser(User user) {
        String token = UUID.randomUUID().toString();

        EmailConfirmation confirmation = new EmailConfirmation();
        confirmation.setToken(token);
        confirmation.setEmail(user.getEmail());
        confirmation.setExpiresAt(LocalDateTime.now().plusDays(1));

        repository.save(confirmation);
        return token;
    }

    public void confirm(String token, UserRepository userRepository) {
        EmailConfirmation conf = repository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (conf.isUsed() || conf.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expiré ou déjà utilisé");
        }

        User user = userRepository.findByEmail(conf.getEmail())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));

        user.setEnabled(true);
        conf.setUsed(true);

        repository.save(conf);
        userRepository.save(user);
    }
}

