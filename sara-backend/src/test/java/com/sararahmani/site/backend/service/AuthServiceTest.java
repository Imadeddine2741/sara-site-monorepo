package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.dto.AuthRequest;
import com.sararahmani.site.backend.dto.AuthResponse;
import com.sararahmani.site.backend.dto.RegisterRequest;
import com.sararahmani.site.backend.entity.EmailConfirmation;
import com.sararahmani.site.backend.entity.Role;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.exception.EmailAlreadyExistsException;
import com.sararahmani.site.backend.repository.EmailConfirmationRepository;
import com.sararahmani.site.backend.repository.PasswordResetTokenRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import com.sararahmani.site.backend.service.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MailService mailService;

    @Mock
    private EmailConfirmationRepository emailConfirmationRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .nom("Dupont")
                .prenom("Jean")
                .role(Role.PATIENT)
                .enabled(false)
                .build();

        registerRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "Dupont",
                "Jean"
        );

        authRequest = new AuthRequest(
                "test@example.com",
                "password123"
        );
    }

    @Nested
    @DisplayName("Tests d'inscription")
    class RegisterTests {

        @Test
        @DisplayName("Inscription reussie - doit creer un utilisateur et envoyer un email")
        void register_Success() {
            // Given
            when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
            when(userMapper.fromRegister(registerRequest)).thenReturn(testUser);
            when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(eq(testUser.getEmail()), any(Map.class))).thenReturn("jwt-token");
            when(userMapper.toAuthResponse(eq(testUser), anyString()))
                    .thenReturn(new AuthResponse("jwt-token", "test@example.com", "Dupont", "Jean", "PATIENT"));

            // When
            AuthResponse response = authService.register(registerRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.token()).isEqualTo("jwt-token");

            verify(userRepository).save(any(User.class));
            verify(emailConfirmationRepository).save(any(EmailConfirmation.class));
            verify(mailService).sendConfirmationEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("Inscription echouee - email deja existant")
        void register_EmailAlreadyExists() {
            // Given
            when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Cette adresse email est déjà utilisée.");

            verify(userRepository, never()).save(any());
            verify(mailService, never()).sendConfirmationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Inscription - le mot de passe doit etre encode")
        void register_PasswordIsEncoded() {
            // Given
            when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
            when(userMapper.fromRegister(registerRequest)).thenReturn(testUser);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(jwtService.generateToken(anyString(), any(Map.class))).thenReturn("jwt-token");
            when(userMapper.toAuthResponse(any(), anyString()))
                    .thenReturn(new AuthResponse("jwt-token", "test@example.com", "Dupont", "Jean", "PATIENT"));

            // When
            authService.register(registerRequest);

            // Then
            verify(passwordEncoder).encode("password123");
            assertThat(testUser.getPassword()).isEqualTo("encodedPassword");
        }

        @Test
        @DisplayName("Inscription - l'utilisateur doit etre desactive par defaut")
        void register_UserIsDisabledByDefault() {
            // Given
            when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
            when(userMapper.fromRegister(registerRequest)).thenReturn(testUser);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(jwtService.generateToken(anyString(), any(Map.class))).thenReturn("jwt-token");
            when(userMapper.toAuthResponse(any(), anyString()))
                    .thenReturn(new AuthResponse("jwt-token", "test@example.com", "Dupont", "Jean", "PATIENT"));

            // When
            authService.register(registerRequest);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests de connexion")
    class AuthenticateTests {

        @Test
        @DisplayName("Connexion reussie")
        void authenticate_Success() {
            // Given
            testUser.setEnabled(true);
            when(userRepository.findByEmail(authRequest.email())).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(eq(testUser.getEmail()), any(Map.class))).thenReturn("jwt-token");
            when(userMapper.toAuthResponse(eq(testUser), eq("jwt-token")))
                    .thenReturn(new AuthResponse("jwt-token", "test@example.com", "Dupont", "Jean", "PATIENT"));

            // When
            AuthResponse response = authService.authenticate(authRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.token()).isEqualTo("jwt-token");

            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password())
            );
        }

        @Test
        @DisplayName("Connexion echouee - mauvais identifiants")
        void authenticate_BadCredentials() {
            // Given
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(authRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("Tests de confirmation email")
    class ConfirmEmailTests {

        @Test
        @DisplayName("Confirmation email reussie")
        void confirmEmail_Success() {
            // Given
            EmailConfirmation confirmation = EmailConfirmation.builder()
                    .token("valid-token")
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();

            when(emailConfirmationRepository.findByToken("valid-token")).thenReturn(Optional.of(confirmation));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            String result = authService.confirmEmail("valid-token");

            // Then
            assertThat(result).isEqualTo("Votre compte a été activé avec succès !");
            assertThat(testUser.isEnabled()).isTrue();
            assertThat(confirmation.isUsed()).isTrue();

            verify(userRepository).save(testUser);
            verify(emailConfirmationRepository).save(confirmation);
        }

        @Test
        @DisplayName("Confirmation email echouee - token deja utilise")
        void confirmEmail_TokenAlreadyUsed() {
            // Given
            EmailConfirmation confirmation = EmailConfirmation.builder()
                    .token("used-token")
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(true)
                    .build();

            when(emailConfirmationRepository.findByToken("used-token")).thenReturn(Optional.of(confirmation));

            // When
            String result = authService.confirmEmail("used-token");

            // Then
            assertThat(result).isEqualTo("Ce lien a déjà été utilisé.");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Confirmation email echouee - token expire")
        void confirmEmail_TokenExpired() {
            // Given
            EmailConfirmation confirmation = EmailConfirmation.builder()
                    .token("expired-token")
                    .email("test@example.com")
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .used(false)
                    .build();

            when(emailConfirmationRepository.findByToken("expired-token")).thenReturn(Optional.of(confirmation));

            // When
            String result = authService.confirmEmail("expired-token");

            // Then
            assertThat(result).isEqualTo("Ce lien a expiré.");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Confirmation email echouee - token invalide")
        void confirmEmail_InvalidToken() {
            // Given
            when(emailConfirmationRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.confirmEmail("invalid-token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token invalide");
        }
    }
}