package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + email));

        if (!user.isEnabled()) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Votre compte n'est pas encore activé. Vérifiez votre email pour confirmer."
            );
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .accountLocked(false)
                .disabled(!user.isEnabled())
                .build();
    }
}


