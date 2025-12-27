package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.dto.AppointmentRequest;
import com.sararahmani.site.backend.dto.AppointmentResponse;
import com.sararahmani.site.backend.entity.Appointment;
import com.sararahmani.site.backend.entity.Role;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.exception.AccessDeniedException;
import com.sararahmani.site.backend.exception.ConflictException;
import com.sararahmani.site.backend.exception.EntityNotFoundException;
import com.sararahmani.site.backend.repository.AppointmentRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;
    private Appointment appointment;
    private AppointmentRequest appointmentRequest;
    private LocalDateTime futureDate;

    @BeforeEach
    void setUp() {
        patient = User.builder()
                .id(1L)
                .email("patient@example.com")
                .password("password")
                .nom("Martin")
                .prenom("Pierre")
                .role(Role.PATIENT)
                .enabled(true)
                .build();

        futureDate = LocalDateTime.now().plusDays(7);

        appointment = Appointment.builder()
                .id(1L)
                .dateHeure(futureDate)
                .motif("Consultation initiale")
                .patient(patient)
                .status(Appointment.Status.PLANNED)
                .dureeMinutes(50)
                .annuleParPatient(false)
                .createdAt(LocalDateTime.now())
                .build();

        appointmentRequest = new AppointmentRequest(futureDate, "Consultation initiale");
    }

    @Nested
    @DisplayName("Tests de creation de rendez-vous")
    class CreateAppointmentTests {

        @Test
        @DisplayName("Creation reussie d'un rendez-vous")
        void createAppointment_Success() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
            when(appointmentRepository.findAvailableSlots(any(), any())).thenReturn(Collections.emptyList());
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

            // When
            AppointmentResponse response = appointmentService.createAppointment(1L, appointmentRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.motif()).isEqualTo("Consultation initiale");
            assertThat(response.patientNom()).isEqualTo("Martin");
            assertThat(response.patientPrenom()).isEqualTo("Pierre");
            assertThat(response.status()).isEqualTo("PLANNED");

            verify(appointmentRepository).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Creation echouee - patient non trouve")
        void createAppointment_PatientNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentService.createAppointment(999L, appointmentRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Patient non trouvé");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Creation echouee - creneau deja pris")
        void createAppointment_SlotAlreadyTaken() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
            when(appointmentRepository.findAvailableSlots(any(), any())).thenReturn(List.of(appointment));

            // When & Then
            assertThatThrownBy(() -> appointmentService.createAppointment(1L, appointmentRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Ce créneau est déjà pris");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Creation - le rendez-vous a le statut PLANNED par defaut")
        void createAppointment_DefaultStatusIsPlanned() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
            when(appointmentRepository.findAvailableSlots(any(), any())).thenReturn(Collections.emptyList());
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            appointmentService.createAppointment(1L, appointmentRequest);

            // Then
            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Appointment.Status.PLANNED);
        }
    }

    @Nested
    @DisplayName("Tests de recuperation des rendez-vous")
    class GetAppointmentsTests {

        @Test
        @DisplayName("Recuperer les rendez-vous d'un patient")
        void getMyAppointments_Success() {
            // Given
            Appointment appointment2 = Appointment.builder()
                    .id(2L)
                    .dateHeure(futureDate.plusDays(7))
                    .motif("Suivi")
                    .patient(patient)
                    .status(Appointment.Status.PLANNED)
                    .build();

            when(appointmentRepository.findByPatientIdOrderByDateHeureDesc(1L))
                    .thenReturn(List.of(appointment, appointment2));

            // When
            List<AppointmentResponse> responses = appointmentService.getMyAppointments(1L);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).motif()).isEqualTo("Consultation initiale");
            assertThat(responses.get(1).motif()).isEqualTo("Suivi");
        }

        @Test
        @DisplayName("Recuperer les rendez-vous - liste vide")
        void getMyAppointments_EmptyList() {
            // Given
            when(appointmentRepository.findByPatientIdOrderByDateHeureDesc(1L))
                    .thenReturn(Collections.emptyList());

            // When
            List<AppointmentResponse> responses = appointmentService.getMyAppointments(1L);

            // Then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("Recuperer tous les rendez-vous (admin)")
        void getAllAppointments_Success() {
            // Given
            when(appointmentRepository.findAllByOrderByDateHeureDesc())
                    .thenReturn(List.of(appointment));

            // When
            List<AppointmentResponse> responses = appointmentService.getAllAppointments();

            // Then
            assertThat(responses).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Tests d'annulation de rendez-vous")
    class CancelAppointmentTests {

        @Test
        @DisplayName("Annulation reussie - plus de 48h avant")
        void cancelAppointment_Success() {
            // Given
            LocalDateTime in72Hours = LocalDateTime.now().plusHours(72);
            appointment.setDateHeure(in72Hours);

            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            // When
            appointmentService.cancelAppointment(1L, 1L);

            // Then
            assertThat(appointment.getStatus()).isEqualTo(Appointment.Status.CANCELLED);
            assertThat(appointment.isAnnuleParPatient()).isTrue();
            verify(appointmentRepository).save(appointment);
        }

        @Test
        @DisplayName("Annulation echouee - rendez-vous non trouve")
        void cancelAppointment_NotFound() {
            // Given
            when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(1L, 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("RDV non trouvé");
        }

        @Test
        @DisplayName("Annulation echouee - pas le proprietaire")
        void cancelAppointment_NotOwner() {
            // Given
            User otherPatient = User.builder().id(2L).build();
            appointment.setPatient(otherPatient);

            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            // When & Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(1L, 1L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Vous ne pouvez annuler que vos RDV");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Annulation echouee - moins de 48h avant")
        void cancelAppointment_TooLate() {
            // Given
            LocalDateTime in24Hours = LocalDateTime.now().plusHours(24);
            appointment.setDateHeure(in24Hours);

            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            // When & Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(1L, 1L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Annulation possible seulement 48h avant le RDV");

            verify(appointmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests de suppression de rendez-vous (admin)")
    class DeleteAppointmentTests {

        @Test
        @DisplayName("Suppression reussie")
        void deleteAppointment_Success() {
            // When
            appointmentService.deleteAppointment(1L);

            // Then
            verify(appointmentRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Tests du flag canPatientCancel")
    class CanPatientCancelTests {

        @Test
        @DisplayName("canPatientCancel est true quand plus de 48h avant et status PLANNED")
        void canPatientCancel_True() {
            // Given
            LocalDateTime in72Hours = LocalDateTime.now().plusHours(72);
            appointment.setDateHeure(in72Hours);
            appointment.setStatus(Appointment.Status.PLANNED);

            when(appointmentRepository.findByPatientIdOrderByDateHeureDesc(1L))
                    .thenReturn(List.of(appointment));

            // When
            List<AppointmentResponse> responses = appointmentService.getMyAppointments(1L);

            // Then
            assertThat(responses.get(0).canPatientCancel()).isTrue();
        }

        @Test
        @DisplayName("canPatientCancel est false quand moins de 48h avant")
        void canPatientCancel_FalseTooLate() {
            // Given
            LocalDateTime in24Hours = LocalDateTime.now().plusHours(24);
            appointment.setDateHeure(in24Hours);
            appointment.setStatus(Appointment.Status.PLANNED);

            when(appointmentRepository.findByPatientIdOrderByDateHeureDesc(1L))
                    .thenReturn(List.of(appointment));

            // When
            List<AppointmentResponse> responses = appointmentService.getMyAppointments(1L);

            // Then
            assertThat(responses.get(0).canPatientCancel()).isFalse();
        }

        @Test
        @DisplayName("canPatientCancel est false quand status n'est pas PLANNED")
        void canPatientCancel_FalseWrongStatus() {
            // Given
            LocalDateTime in72Hours = LocalDateTime.now().plusHours(72);
            appointment.setDateHeure(in72Hours);
            appointment.setStatus(Appointment.Status.CANCELLED);

            when(appointmentRepository.findByPatientIdOrderByDateHeureDesc(1L))
                    .thenReturn(List.of(appointment));

            // When
            List<AppointmentResponse> responses = appointmentService.getMyAppointments(1L);

            // Then
            assertThat(responses.get(0).canPatientCancel()).isFalse();
        }
    }
}
