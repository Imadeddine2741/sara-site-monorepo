package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.entity.EmailConfirmation;
import com.sararahmani.site.backend.entity.Role;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.repository.EmailConfirmationRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfirmationServiceTest {

    @Mock
    private EmailConfirmationRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailConfirmationService emailConfirmationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .nom("Test")
                .prenom("User")
                .role(Role.PATIENT)
                .enabled(false)
                .build();
    }

    @Test
    @DisplayName("Creer un token pour un utilisateur")
    void createTokenForUser_Success() {
        when(repository.save(any(EmailConfirmation.class))).thenAnswer(i -> i.getArgument(0));

        String token = emailConfirmationService.createTokenForUser(user);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        ArgumentCaptor<EmailConfirmation> captor = ArgumentCaptor.forClass(EmailConfirmation.class);
        verify(repository).save(captor.capture());

        EmailConfirmation saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Confirmer un token valide")
    void confirm_ValidToken() {
        EmailConfirmation confirmation = EmailConfirmation.builder()
                .token("valid-token")
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(repository.findByToken("valid-token")).thenReturn(Optional.of(confirmation));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        emailConfirmationService.confirm("valid-token", userRepository);

        assertThat(user.isEnabled()).isTrue();
        assertThat(confirmation.isUsed()).isTrue();

        verify(repository).save(confirmation);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Confirmer un token invalide")
    void confirm_InvalidToken() {
        when(repository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailConfirmationService.confirm("invalid-token", userRepository))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token invalide");
    }

    @Test
    @DisplayName("Confirmer un token deja utilise")
    void confirm_UsedToken() {
        EmailConfirmation confirmation = EmailConfirmation.builder()
                .token("used-token")
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(true)
                .build();

        when(repository.findByToken("used-token")).thenReturn(Optional.of(confirmation));

        assertThatThrownBy(() -> emailConfirmationService.confirm("used-token", userRepository))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token expiré ou déjà utilisé");
    }

    @Test
    @DisplayName("Confirmer un token expire")
    void confirm_ExpiredToken() {
        EmailConfirmation confirmation = EmailConfirmation.builder()
                .token("expired-token")
                .email("test@example.com")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();

        when(repository.findByToken("expired-token")).thenReturn(Optional.of(confirmation));

        assertThatThrownBy(() -> emailConfirmationService.confirm("expired-token", userRepository))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token expiré ou déjà utilisé");
    }

    @Test
    @DisplayName("Confirmer - utilisateur non trouve")
    void confirm_UserNotFound() {
        EmailConfirmation confirmation = EmailConfirmation.builder()
                .token("valid-token")
                .email("unknown@example.com")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(repository.findByToken("valid-token")).thenReturn(Optional.of(confirmation));
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailConfirmationService.confirm("valid-token", userRepository))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Utilisateur introuvable");
    }
}
