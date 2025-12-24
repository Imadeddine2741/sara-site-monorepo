package com.sararahmani.site.backend.controller;

import com.sararahmani.site.backend.dto.PatientDto;
import com.sararahmani.site.backend.service.PatientService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "http://localhost:4200")
@AllArgsConstructor
public class ApiController {

    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<PatientDto> create(@RequestBody PatientDto dto) {
        PatientDto created = patientService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PatientDto>> findAll() {
        return ResponseEntity.ok(patientService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.findById(id));
    }
}

