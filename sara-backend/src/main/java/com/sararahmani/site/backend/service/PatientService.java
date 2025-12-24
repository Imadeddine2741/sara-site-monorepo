package com.sararahmani.site.backend.service;


import com.sararahmani.site.backend.dto.PatientDto;

import java.util.List;

public interface PatientService {

    PatientDto create(PatientDto dto);

    List<PatientDto> findAll();

    PatientDto findById(Long id);
}
