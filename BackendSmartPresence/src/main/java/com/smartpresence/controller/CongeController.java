package com.smartpresence.controller;

import com.smartpresence.constants.StatutDemande;
import com.smartpresence.dto.request.GestionRequests;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.GestionResponses;
import com.smartpresence.service.CongeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Décision de l'administration sur les demandes de congé des enseignants. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
@Tag(name = "Congés", description = "Demandes de congé et d'absence des enseignants")
public class CongeController {

    private final CongeService congeService;

    @GetMapping("/conges")
    @Operation(summary = "Demandes de congé",
            description = "Filtrables par statut ; sans filtre, toutes. Triées par date de début.")
    public ResponseEntity<ApiResponse<List<GestionResponses.Conge>>> lister(
            @RequestParam(required = false) StatutDemande statut) {
        return ResponseEntity.ok(ApiResponse.success(congeService.aTraiter(statut)));
    }

    @PostMapping("/conges/{id}/decision")
    @Operation(summary = "Accepter ou refuser une demande",
            description = "Le refus doit être motivé. L'enseignant est prévenu.")
    public ResponseEntity<ApiResponse<GestionResponses.Conge>> decider(
            @PathVariable UUID id, @Valid @RequestBody GestionRequests.DecisionConge decision) {
        return ResponseEntity.ok(ApiResponse.success(congeService.decider(id, decision), "Décision enregistrée"));
    }
}
