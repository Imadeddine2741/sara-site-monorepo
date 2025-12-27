package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.entity.Role;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User enabledUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        enabledUser = User.builder()
                .id(1L)
                .email("enabled@example.com")
                .password("password")
                .nom("Test")
                .prenom("User")
                .role(Role.PATIENT)
                .enabled(true)
                .build();

        disabledUser = User.builder()
                .id(2L)
                .email("disabled@example.com")
                .password("password")
                .nom("Disabled")
                .prenom("User")
                .role(Role.PATIENT)
                .enabled(false)
                .build();
    }

    @Test
    @DisplayName("Charger un utilisateur active")
    void loadUserByUsername_EnabledUser() {
        when(userRepository.findByEmail("enabled@example.com")).thenReturn(Optional.of(enabledUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("enabled@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("enabled@example.com");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Utilisateur non trouve")
    void loadUserByUsername_UserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    @Test
    @DisplayName("Utilisateur desactive")
    void loadUserByUsername_DisabledUser() {
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("disabled@example.com"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("compte n'est pas encore activé");
    }
}
