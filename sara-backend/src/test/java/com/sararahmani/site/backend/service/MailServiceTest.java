package com.sararahmani.site.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender);
        ReflectionTestUtils.setField(mailService, "frontendUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("Envoyer email de confirmation")
    void sendConfirmationEmail_Success() {
        String email = "test@example.com";
        String token = "confirmation-token-123";

        mailService.sendConfirmationEmail(email, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(email);
        assertThat(sentMessage.getSubject()).isEqualTo("Confirmation de votre compte");
        assertThat(sentMessage.getText()).contains("http://localhost:4200/confirm-email?token=" + token);
        assertThat(sentMessage.getText()).contains("Ce lien expire dans 24 heures");
    }

    @Test
    @DisplayName("Envoyer email de reinitialisation mot de passe")
    void sendPasswordResetEmail_Success() {
        String email = "test@example.com";
        String token = "reset-token-456";

        mailService.sendPasswordResetEmail(email, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(email);
        assertThat(sentMessage.getSubject()).isEqualTo("Réinitialisation de votre mot de passe");
        assertThat(sentMessage.getText()).contains("http://localhost:4200/reset-password?token=" + token);
        assertThat(sentMessage.getText()).contains("Ce lien expire dans 1 heure");
    }

    @Test
    @DisplayName("Email de confirmation contient le bon lien")
    void sendConfirmationEmail_ContainsCorrectLink() {
        String token = "my-token";

        mailService.sendConfirmationEmail("user@test.com", token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        String body = messageCaptor.getValue().getText();
        assertThat(body).contains("http://localhost:4200/confirm-email?token=my-token");
    }

    @Test
    @DisplayName("Email de reset contient le bon lien")
    void sendPasswordResetEmail_ContainsCorrectLink() {
        String token = "reset-token";

        mailService.sendPasswordResetEmail("user@test.com", token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        String body = messageCaptor.getValue().getText();
        assertThat(body).contains("http://localhost:4200/reset-password?token=reset-token");
    }
}
