package com.sararahmani.site.backend.service;

import com.sararahmani.site.backend.dto.PatientDto;
import com.sararahmani.site.backend.entity.Patient;
import com.sararahmani.site.backend.repository.PatientRepository;
import com.sararahmani.site.backend.service.impl.PatientServiceImpl;
import com.sararahmani.site.backend.service.mapper.PatientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient patient;
    private PatientDto patientDto;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(1L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie.dupont@example.com")
                .telephone("0612345678")
                .dateCreation(LocalDateTime.now())
                .build();

        patientDto = new PatientDto(1L, "Dupont", "Marie", "marie.dupont@example.com", "0612345678");
    }

    @Test
    @DisplayName("Creer un patient")
    void create_Success() {
        when(patientMapper.toEntity(patientDto)).thenReturn(patient);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        PatientDto result = patientService.create(patientDto);

        assertThat(result).isNotNull();
        assertThat(result.nom()).isEqualTo("Dupont");
        assertThat(result.email()).isEqualTo("marie.dupont@example.com");

        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Trouver tous les patients")
    void findAll_Success() {
        Patient patient2 = Patient.builder()
                .id(2L)
                .nom("Martin")
                .prenom("Jean")
                .email("jean.martin@example.com")
                .build();
        PatientDto dto2 = new PatientDto(2L, "Martin", "Jean", "jean.martin@example.com", null);

        when(patientRepository.findAll()).thenReturn(List.of(patient, patient2));
        when(patientMapper.toDto(patient)).thenReturn(patientDto);
        when(patientMapper.toDto(patient2)).thenReturn(dto2);

        List<PatientDto> result = patientService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nom()).isEqualTo("Dupont");
        assertThat(result.get(1).nom()).isEqualTo("Martin");
    }

    @Test
    @DisplayName("Trouver tous les patients - liste vide")
    void findAll_EmptyList() {
        when(patientRepository.findAll()).thenReturn(List.of());

        List<PatientDto> result = patientService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Trouver un patient par ID")
    void findById_Success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toDto(patient)).thenReturn(patientDto);

        PatientDto result = patientService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nom()).isEqualTo("Dupont");
    }

    @Test
    @DisplayName("Trouver un patient par ID - non trouve")
    void findById_NotFound() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.findById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient introuvable");
    }
}
