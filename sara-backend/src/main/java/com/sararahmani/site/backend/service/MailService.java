package com.sararahmani.site.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public void sendConfirmationEmail(String email, String token) {
        String link = frontendUrl + "/confirm-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Confirmation de votre compte");
        message.setText(
                "Bonjour,\n\nMerci pour votre inscription.\n" +
                        "Veuillez confirmer votre adresse email en cliquant sur le lien suivant :\n" +
                        link + "\n\n" +
                        "Ce lien expire dans 24 heures."
        );

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String email, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Réinitialisation de votre mot de passe");
        message.setText(
                "Bonjour,\n\n" +
                        "Vous avez demandé la réinitialisation de votre mot de passe.\n" +
                        "Cliquez sur le lien suivant pour définir un nouveau mot de passe :\n" +
                        link + "\n\n" +
                        "Ce lien expire dans 1 heure.\n\n" +
                        "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email."
        );

        mailSender.send(message);
    }
}

