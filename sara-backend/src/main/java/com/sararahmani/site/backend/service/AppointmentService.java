package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.dto.AppointmentRequest;
import com.sararahmani.site.backend.dto.AppointmentResponse;
import com.sararahmani.site.backend.entity.Appointment;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.exception.AccessDeniedException;
import com.sararahmani.site.backend.exception.ConflictException;
import com.sararahmani.site.backend.exception.EntityNotFoundException;
import com.sararahmani.site.backend.repository.AppointmentRepository;
import com.sararahmani.site.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private static final int ANNULATION_MINUTES = 48 * 60;

    // Patient : créer RDV
    public AppointmentResponse createAppointment(Long patientId, AppointmentRequest request) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient non trouvé"));

        if (isSlotTaken(request.dateHeure())) {
            throw new ConflictException("Ce créneau est déjà pris");
        }

        Appointment appointment = Appointment.builder()
                .dateHeure(request.dateHeure())
                .motif(request.motif())
                .patient(patient)
                .build();

        appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }

    // Patient : mes RDV
    public List<AppointmentResponse> getMyAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByDateHeureDesc(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Patient : annuler
    public void cancelAppointment(Long patientId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("RDV non trouvé"));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos RDV");
        }

        long minutesBefore = ChronoUnit.MINUTES.between(
                LocalDateTime.now(),
                appointment.getDateHeure()
        );

        if (minutesBefore < ANNULATION_MINUTES) {
            throw new ConflictException("Annulation possible seulement 48h avant le RDV");
        }

        appointment.setStatus(Appointment.Status.CANCELLED);
        appointment.setAnnuleParPatient(true);
        appointmentRepository.save(appointment);
    }

    // Admin
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAllByOrderByDateHeureDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void deleteAppointment(Long appointmentId) {
        appointmentRepository.deleteById(appointmentId);
    }

    private boolean isSlotTaken(LocalDateTime slot) {
        return appointmentRepository.findAvailableSlots(slot.minusMinutes(55), slot.plusMinutes(55))
                .stream()
                .anyMatch(a -> Math.abs(ChronoUnit.MINUTES.between(a.getDateHeure(), slot)) < 60);
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        boolean canPatientCancel = ChronoUnit.MINUTES.between(
                LocalDateTime.now(), a.getDateHeure()
        ) > ANNULATION_MINUTES && a.getStatus() == Appointment.Status.PLANNED;

        return new AppointmentResponse(
                a.getId(),
                a.getDateHeure(),
                a.getMotif(),
                a.getPatient().getNom(),
                a.getPatient().getPrenom(),
                a.getStatus().name(),
                canPatientCancel
        );
    }
}
