package com.smartpresence.controller;

import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.StatistiquesClasseResponse;
import com.smartpresence.dto.response.StatistiquesEtudiantResponse;
import com.smartpresence.dto.response.StatistiquesGlobalesResponse;
import com.smartpresence.service.StatistiquesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/statistiques")
@RequiredArgsConstructor
@Tag(name = "Statistiques", description = "Indicateurs et statistiques d'assiduité")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    @Operation(summary = "Indicateurs globaux")
    @GetMapping("/globales")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<StatistiquesGlobalesResponse>> getGlobales() {
        return ResponseEntity.ok(ApiResponse.success(statistiquesService.getGlobales(), "Statistiques globales récupérées"));
    }

    @Operation(summary = "Statistiques d'une classe")
    @GetMapping("/classes/{classeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<StatistiquesClasseResponse>> getByClasse(@PathVariable Long classeId) {
        return ResponseEntity.ok(ApiResponse.success(statistiquesService.getByClasse(classeId), "Statistiques de la classe récupérées"));
    }

    @Operation(summary = "Statistiques d'un étudiant")
    @GetMapping("/etudiants/{etudiantId}")
    // « isAuthenticated() » laissait n'importe quel compte lire l'assiduite de n'importe
    // quel etudiant. Un etudiant consulte la sienne via GET /moi/statistiques, ou aucun
    // identifiant ne circule.
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<StatistiquesEtudiantResponse>> getByEtudiant(@PathVariable UUID etudiantId) {
        return ResponseEntity.ok(ApiResponse.success(statistiquesService.getByEtudiant(etudiantId), "Statistiques de l'étudiant récupérées"));
    }

    @Operation(summary = "Tendance journalière des présences")
    @GetMapping("/tendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTendanceJournaliere(
            @RequestParam LocalDate debut, @RequestParam LocalDate fin) {
        return ResponseEntity.ok(ApiResponse.success(statistiquesService.getTendanceJournaliere(debut, fin), "Tendance journalière récupérée"));
    }
}
