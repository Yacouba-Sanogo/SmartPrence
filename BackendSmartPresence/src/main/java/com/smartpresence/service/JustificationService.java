package com.smartpresence.service;

import com.smartpresence.constants.StatutJustification;
import com.smartpresence.dto.response.JustificationAbsenceResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JustificationService {

    List<JustificationAbsenceResponse> findAll();

    List<JustificationAbsenceResponse> findByEtudiant(UUID etudiantId);

    JustificationAbsenceResponse create(UUID etudiantId, String motif, LocalDate dateAbsence, UUID seanceId);

    void traiter(UUID id, StatutJustification statut, UUID traiteParId, String commentaire);
}
