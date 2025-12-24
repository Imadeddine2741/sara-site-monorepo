package com.sararahmani.site.backend.service.impl;

import com.sararahmani.site.backend.dto.PatientDto;
import com.sararahmani.site.backend.entity.Patient;
import com.sararahmani.site.backend.repository.PatientRepository;
import com.sararahmani.site.backend.service.PatientService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository repository;

    @Override
    public PatientDto create(PatientDto dto) {
        Patient patient = Patient.builder()
                .nom(dto.nom())
                .prenom(dto.prenom())
                .email(dto.email())
                .telephone(dto.telephone())
                .build();

        Patient saved = repository.save(patient);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDto findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Patient introuvable: " + id));
    }

    private PatientDto toDto(Patient patient) {
        return new PatientDto(
                patient.getId(),
                patient.getNom(),
                patient.getPrenom(),
                patient.getEmail(),
                patient.getTelephone()
        );
    }
}

