package com.sararahmani.site.backend.service.impl;

import com.sararahmani.site.backend.dto.PatientDto;
import com.sararahmani.site.backend.entity.Patient;
import com.sararahmani.site.backend.repository.PatientRepository;
import com.sararahmani.site.backend.service.PatientService;
import com.sararahmani.site.backend.service.mapper.PatientMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository repository;
    private final PatientMapper mapper;

    @Override
    public PatientDto create(PatientDto dto) {
        Patient patient = mapper.toEntity(dto);
        Patient saved = repository.save(patient);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Patient introuvable: " + id));
    }
}

