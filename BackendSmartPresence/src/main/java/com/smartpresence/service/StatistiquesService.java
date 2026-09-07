package com.smartpresence.service;

import com.smartpresence.dto.response.StatistiquesClasseResponse;
import com.smartpresence.dto.response.StatistiquesEtudiantResponse;
import com.smartpresence.dto.response.StatistiquesGlobalesResponse;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface StatistiquesService {

    StatistiquesGlobalesResponse getGlobales();

    StatistiquesClasseResponse getByClasse(Long classeId);

    StatistiquesEtudiantResponse getByEtudiant(UUID etudiantId);

    Map<String, Long> getTendanceJournaliere(LocalDate debut, LocalDate fin);
}
