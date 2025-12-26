package com.sararahmani.site.backend.controller;


import com.sararahmani.site.backend.dto.AppointmentRequest;
import com.sararahmani.site.backend.dto.AppointmentResponse;
import com.sararahmani.site.backend.entity.User;
import com.sararahmani.site.backend.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @AuthenticationPrincipal User patient,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(
                appointmentService.createAppointment(patient.getId(), request)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @AuthenticationPrincipal User patient) {
        return ResponseEntity.ok(
                appointmentService.getMyAppointments(patient.getId())
        );
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @AuthenticationPrincipal User patient,
            @PathVariable Long id) {
        appointmentService.cancelAppointment(patient.getId(), id);
        return ResponseEntity.ok(Map.of("message", "RDV annulé avec succès"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getAll() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok(Map.of("message", "RDV supprimé"));
    }
}

