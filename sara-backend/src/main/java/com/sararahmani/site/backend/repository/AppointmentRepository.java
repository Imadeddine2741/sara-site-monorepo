package com.sararahmani.site.backend.repository;

import com.sararahmani.site.backend.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Pour patient (méthodes Spring Data JPA auto)
    List<Appointment> findByPatientIdOrderByDateHeureDesc(Long patientId);
    List<Appointment> findByPatientIdAndStatusOrderByDateHeureDesc(Long patientId, Appointment.Status status);

    // Disponibilités (creneaux libres) ✅ CORRIGÉ
    @Query("SELECT a FROM Appointment a WHERE a.dateHeure >= :from AND a.dateHeure < :to AND a.status = 'PLANNED'")
    List<Appointment> findAvailableSlots(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // Admin : tous les RDV (Spring Data JPA auto)
    List<Appointment> findAllByOrderByDateHeureDesc();

    // RDV dans les 48h pour patient ✅ CORRIGÉ
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.dateHeure > :now AND a.dateHeure < :nowPlus48h")
    List<Appointment> findUpcomingWithin48h(
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now,
            @Param("nowPlus48h") LocalDateTime nowPlus48h
    );
}

