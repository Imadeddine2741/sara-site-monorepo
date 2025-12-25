package com.sararahmani.site.backend.repository;

import com.sararahmani.site.backend.entity.EmailConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, Long> {

    Optional<EmailConfirmation> findByToken(String token);
}

