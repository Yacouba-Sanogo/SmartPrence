package com.smartpresence.controller;

import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.HistoriqueSynchronisationResponse;
import com.smartpresence.service.SynchronisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Consultation du journal des synchronisations des appareils ESP32.
 *
 * @since 0.0.1
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Synchronisations", description = "Journal des échanges entre les lecteurs ESP32 et le backend")
public class SynchronisationController {

    private final SynchronisationService synchronisationService;

    @GetMapping("/synchronisations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    @Operation(summary = "Dernières synchronisations, tous appareils confondus",
            description = "Alimente la supervision du parc : volume transmis, tentatives et issue.")
    public ResponseEntity<ApiResponse<List<HistoriqueSynchronisationResponse>>> recentes(
            @RequestParam(defaultValue = "50") int limite) {
        return ResponseEntity.ok(ApiResponse.success(synchronisationService.recentes(limite)));
    }

    @GetMapping("/devices/{id}/synchronisations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    @Operation(summary = "Journal de synchronisation d'un appareil")
    public ResponseEntity<ApiResponse<List<HistoriqueSynchronisationResponse>>> parAppareil(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(synchronisationService.parAppareil(id)));
    }
}
