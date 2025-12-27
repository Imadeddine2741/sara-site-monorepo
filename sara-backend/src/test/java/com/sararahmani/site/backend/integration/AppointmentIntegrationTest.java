package com.sararahmani.site.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sararahmani.site.backend.dto.AppointmentRequest;
import com.sararahmani.site.backend.dto.AuthRequest;
import com.sararahmani.site.backend.entity.Appointment;
import com.sararahmani.site.backend.entity.Role;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.repository.AppointmentRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patient;
    private User admin;
    private String patientToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        appointmentRepository.deleteAll();
        userRepository.deleteAll();

        // Creer un patient
        patient = User.builder()
                .email("patient@example.com")
                .password(passwordEncoder.encode("password123"))
                .nom("Patient")
                .prenom("Test")
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        userRepository.save(patient);

        // Creer un admin
        admin = User.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .nom("Admin")
                .prenom("Test")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);

        // Obtenir les tokens
        patientToken = getToken("patient@example.com", "password123");
        adminToken = getToken("admin@example.com", "admin123");
    }

    private String getToken(String email, String password) throws Exception {
        AuthRequest request = new AuthRequest(email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Nested
    @DisplayName("Tests d'integration - Creation de rendez-vous")
    class CreateAppointmentTests {

        @Test
        @DisplayName("POST /api/appointments - creation reussie")
        void createAppointment_Success() throws Exception {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
            AppointmentRequest request = new AppointmentRequest(futureDate, "Premiere consultation");

            mockMvc.perform(post("/api/appointments")
                            .header("Authorization", "Bearer " + patientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.motif").value("Premiere consultation"))
                    .andExpect(jsonPath("$.patientNom").value("Patient"))
                    .andExpect(jsonPath("$.patientPrenom").value("Test"))
                    .andExpect(jsonPath("$.status").value("PLANNED"));

            // Verifier en base
            assertThat(appointmentRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("POST /api/appointments - sans authentification")
        void createAppointment_Unauthorized() throws Exception {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(7);
            AppointmentRequest request = new AppointmentRequest(futureDate, "Test");

            mockMvc.perform(post("/api/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/appointments - creneau deja pris")
        void createAppointment_SlotTaken() throws Exception {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);

            // Creer un premier rendez-vous
            Appointment existingAppointment = Appointment.builder()
                    .dateHeure(futureDate)
                    .motif("Existant")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            appointmentRepository.save(existingAppointment);

            // Essayer de creer un deuxieme au meme moment
            AppointmentRequest request = new AppointmentRequest(futureDate, "Nouveau");

            mockMvc.perform(post("/api/appointments")
                            .header("Authorization", "Bearer " + patientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("POST /api/appointments - motif vide")
        void createAppointment_EmptyMotif() throws Exception {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(7);
            AppointmentRequest request = new AppointmentRequest(futureDate, "");

            mockMvc.perform(post("/api/appointments")
                            .header("Authorization", "Bearer " + patientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Tests d'integration - Recuperation des rendez-vous")
    class GetAppointmentsTests {

        @Test
        @DisplayName("GET /api/appointments/me - liste des rendez-vous du patient")
        void getMyAppointments_Success() throws Exception {
            // Creer des rendez-vous pour ce patient
            Appointment apt1 = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusDays(7))
                    .motif("RDV 1")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            Appointment apt2 = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusDays(14))
                    .motif("RDV 2")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            appointmentRepository.save(apt1);
            appointmentRepository.save(apt2);

            mockMvc.perform(get("/api/appointments/me")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].motif", containsInAnyOrder("RDV 1", "RDV 2")));
        }

        @Test
        @DisplayName("GET /api/appointments/me - liste vide")
        void getMyAppointments_Empty() throws Exception {
            mockMvc.perform(get("/api/appointments/me")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /api/appointments/me - sans authentification")
        void getMyAppointments_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/appointments/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/appointments - admin voit tous les rendez-vous")
        void getAllAppointments_Admin() throws Exception {
            // Creer un rendez-vous
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusDays(7))
                    .motif("Test")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            appointmentRepository.save(apt);

            mockMvc.perform(get("/api/appointments")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/appointments - patient ne peut pas voir tous les RDV")
        void getAllAppointments_PatientForbidden() throws Exception {
            mockMvc.perform(get("/api/appointments")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Tests d'integration - Annulation de rendez-vous")
    class CancelAppointmentTests {

        @Test
        @DisplayName("DELETE /api/appointments/{id}/cancel - annulation reussie (>48h)")
        void cancelAppointment_Success() throws Exception {
            // Creer un rendez-vous dans plus de 48h
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusHours(72))
                    .motif("A annuler")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            apt = appointmentRepository.save(apt);

            mockMvc.perform(delete("/api/appointments/" + apt.getId() + "/cancel")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("RDV annulé avec succès"));

            // Verifier le statut en base
            Appointment cancelled = appointmentRepository.findById(apt.getId()).orElseThrow();
            assertThat(cancelled.getStatus()).isEqualTo(Appointment.Status.CANCELLED);
            assertThat(cancelled.isAnnuleParPatient()).isTrue();
        }

        @Test
        @DisplayName("DELETE /api/appointments/{id}/cancel - trop tard (<48h)")
        void cancelAppointment_TooLate() throws Exception {
            // Creer un rendez-vous dans moins de 48h
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusHours(24))
                    .motif("Trop tard")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            apt = appointmentRepository.save(apt);

            mockMvc.perform(delete("/api/appointments/" + apt.getId() + "/cancel")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isConflict());

            // Verifier que le statut n'a pas change
            Appointment notCancelled = appointmentRepository.findById(apt.getId()).orElseThrow();
            assertThat(notCancelled.getStatus()).isEqualTo(Appointment.Status.PLANNED);
        }

        @Test
        @DisplayName("DELETE /api/appointments/{id}/cancel - RDV d'un autre patient")
        void cancelAppointment_NotOwner() throws Exception {
            // Creer un autre patient
            User otherPatient = User.builder()
                    .email("other@example.com")
                    .password(passwordEncoder.encode("password"))
                    .nom("Other")
                    .prenom("Patient")
                    .role(Role.PATIENT)
                    .enabled(true)
                    .build();
            userRepository.save(otherPatient);

            // Creer un rendez-vous pour l'autre patient
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusHours(72))
                    .motif("Autre patient")
                    .patient(otherPatient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            apt = appointmentRepository.save(apt);

            // Essayer d'annuler avec le premier patient
            mockMvc.perform(delete("/api/appointments/" + apt.getId() + "/cancel")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/appointments/{id}/cancel - RDV non trouve")
        void cancelAppointment_NotFound() throws Exception {
            mockMvc.perform(delete("/api/appointments/999/cancel")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Tests d'integration - Suppression admin")
    class DeleteAppointmentTests {

        @Test
        @DisplayName("DELETE /api/appointments/{id} - admin peut supprimer")
        void deleteAppointment_AdminSuccess() throws Exception {
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusDays(7))
                    .motif("A supprimer")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            apt = appointmentRepository.save(apt);

            mockMvc.perform(delete("/api/appointments/" + apt.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("RDV supprimé"));

            assertThat(appointmentRepository.findById(apt.getId())).isEmpty();
        }

        @Test
        @DisplayName("DELETE /api/appointments/{id} - patient ne peut pas supprimer")
        void deleteAppointment_PatientForbidden() throws Exception {
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusDays(7))
                    .motif("Test")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            apt = appointmentRepository.save(apt);

            mockMvc.perform(delete("/api/appointments/" + apt.getId())
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Tests du flag canPatientCancel")
    class CanPatientCancelTests {

        @Test
        @DisplayName("canPatientCancel est true quand >48h et PLANNED")
        void canPatientCancel_True() throws Exception {
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusHours(72))
                    .motif("Peut annuler")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            appointmentRepository.save(apt);

            mockMvc.perform(get("/api/appointments/me")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].canPatientCancel").value(true));
        }

        @Test
        @DisplayName("canPatientCancel est false quand <48h")
        void canPatientCancel_FalseTooLate() throws Exception {
            Appointment apt = Appointment.builder()
                    .dateHeure(LocalDateTime.now().plusHours(24))
                    .motif("Ne peut plus annuler")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .dureeMinutes(50)
                    .annuleParPatient(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            appointmentRepository.save(apt);

            mockMvc.perform(get("/api/appointments/me")
                            .header("Authorization", "Bearer " + patientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].canPatientCancel").value(false));
        }
    }
}
