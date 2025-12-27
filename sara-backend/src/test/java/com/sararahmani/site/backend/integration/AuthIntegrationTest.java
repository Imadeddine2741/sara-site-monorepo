package com.sararahmani.site.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sararahmani.site.backend.dto.AuthRequest;
import com.sararahmani.site.backend.dto.RegisterRequest;
import com.sararahmani.site.backend.entity.Role;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.repository.EmailConfirmationRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import com.sararahmani.site.backend.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailConfirmationRepository emailConfirmationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private MailService mailService;

    @BeforeEach
    void setUp() {
        emailConfirmationRepository.deleteAll();
        userRepository.deleteAll();
        doNothing().when(mailService).sendConfirmationEmail(anyString(), anyString());
    }

    @Nested
    @DisplayName("Tests d'integration - Inscription")
    class RegisterIntegrationTests {

        @Test
        @DisplayName("POST /api/auth/register - inscription reussie")
        void register_Success() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "nouveau@example.com",
                    "password123",
                    "Dupont",
                    "Marie"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("nouveau@example.com"))
                    .andExpect(jsonPath("$.nom").value("Dupont"))
                    .andExpect(jsonPath("$.prenom").value("Marie"))
                    .andExpect(jsonPath("$.role").value("PATIENT"))
                    .andExpect(jsonPath("$.token").isNotEmpty());

            // Verifier que l'utilisateur est bien cree en base
            assertThat(userRepository.findByEmail("nouveau@example.com")).isPresent();

            // Verifier que le token de confirmation email est cree
            assertThat(emailConfirmationRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("POST /api/auth/register - email deja utilise")
        void register_EmailAlreadyExists() throws Exception {
            // Creer un utilisateur existant
            User existingUser = User.builder()
                    .email("existant@example.com")
                    .password(passwordEncoder.encode("password"))
                    .nom("Existant")
                    .prenom("User")
                    .role(Role.PATIENT)
                    .enabled(true)
                    .build();
            userRepository.save(existingUser);

            RegisterRequest request = new RegisterRequest(
                    "existant@example.com",
                    "password123",
                    "Nouveau",
                    "User"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/auth/register - l'utilisateur est desactive par defaut")
        void register_UserDisabledByDefault() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "nouveau@example.com",
                    "password123",
                    "Dupont",
                    "Marie"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            User user = userRepository.findByEmail("nouveau@example.com").orElseThrow();
            assertThat(user.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests d'integration - Connexion")
    class LoginIntegrationTests {

        @Test
        @DisplayName("POST /api/auth/login - connexion reussie")
        void login_Success() throws Exception {
            // Creer un utilisateur active
            User user = User.builder()
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .nom("Test")
                    .prenom("User")
                    .role(Role.PATIENT)
                    .enabled(true)
                    .build();
            userRepository.save(user);

            AuthRequest request = new AuthRequest("test@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.nom").value("Test"))
                    .andExpect(jsonPath("$.prenom").value("User"));
        }

        @Test
        @DisplayName("POST /api/auth/login - mauvais mot de passe")
        void login_WrongPassword() throws Exception {
            // Creer un utilisateur
            User user = User.builder()
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .nom("Test")
                    .prenom("User")
                    .role(Role.PATIENT)
                    .enabled(true)
                    .build();
            userRepository.save(user);

            AuthRequest request = new AuthRequest("test@example.com", "wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/login - utilisateur non existant")
        void login_UserNotFound() throws Exception {
            AuthRequest request = new AuthRequest("nonexistent@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/login - compte desactive")
        void login_AccountDisabled() throws Exception {
            // Creer un utilisateur non active
            User user = User.builder()
                    .email("disabled@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .nom("Disabled")
                    .prenom("User")
                    .role(Role.PATIENT)
                    .enabled(false)
                    .build();
            userRepository.save(user);

            AuthRequest request = new AuthRequest("disabled@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/login - retourne un JWT valide")
        void login_ReturnsValidJwt() throws Exception {
            // Creer un utilisateur
            User user = User.builder()
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .nom("Test")
                    .prenom("User")
                    .role(Role.PATIENT)
                    .enabled(true)
                    .build();
            userRepository.save(user);

            AuthRequest request = new AuthRequest("test@example.com", "password123");

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("token");

            // Le JWT a le format: xxxxx.yyyyy.zzzzz
            String token = objectMapper.readTree(responseBody).get("token").asText();
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Tests d'integration - Confirmation email")
    class ConfirmEmailIntegrationTests {

        @Test
        @DisplayName("GET /api/auth/confirm-email - confirmation reussie")
        void confirmEmail_Success() throws Exception {
            // D'abord inscrire un utilisateur
            RegisterRequest request = new RegisterRequest(
                    "confirm@example.com",
                    "password123",
                    "Confirm",
                    "User"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Recuperer le token de confirmation
            String token = emailConfirmationRepository.findAll().get(0).getToken();

            // Confirmer l'email
            mockMvc.perform(get("/api/auth/confirm-email")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Votre compte a été activé avec succès !"));

            // Verifier que l'utilisateur est maintenant active
            User user = userRepository.findByEmail("confirm@example.com").orElseThrow();
            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("GET /api/auth/confirm-email - token invalide")
        void confirmEmail_InvalidToken() throws Exception {
            mockMvc.perform(get("/api/auth/confirm-email")
                            .param("token", "invalid-token"))
                    .andExpect(status().isBadRequest());
        }
    }
}
