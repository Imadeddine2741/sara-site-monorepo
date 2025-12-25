package com.sararahmani.site.backend.service.mapper;

import com.sararahmani.site.backend.dto.PatientDto;
import com.sararahmani.site.backend.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    PatientDto toDto(Patient entity);

    @Mapping(target = "dateCreation", ignore = true)
    Patient toEntity(PatientDto dto);
}

