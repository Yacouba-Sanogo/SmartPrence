package com.smartpresence.controller;

import com.smartpresence.dto.request.PointageESP32Request;
import com.smartpresence.dto.request.PointageManuelRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.JourneePersonnelResponse;
import com.smartpresence.dto.response.PointagePersonnelResponse;
import com.smartpresence.dto.response.SyncSummaryResponse;
import com.smartpresence.entity.Device;
import com.smartpresence.service.PointagePersonnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints du pointage horaire du personnel.
 *
 * <p>Deux publics distincts : les lecteurs ESP32 sous {@code /esp32/**}, authentifiés par
 * clé d'API, et les interfaces d'administration authentifiées par JWT.</p>
 *
 * @since 0.0.1
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Pointage personnel", description = "Arrivées, départs et temps de présence du personnel")
public class PointagePersonnelController {

    private final PointagePersonnelService pointageService;

    // ------------------------------------------------------------------
    // Flux entrant — lecteurs ESP32
    // ------------------------------------------------------------------

    @PostMapping("/esp32/personnels/pointages")
    @PreAuthorize("hasRole('DEVICE')")
    @Operation(summary = "Synchroniser un lot de pointages de personnel",
            description = "Opération idempotente : un lot retransmis après une coupure réseau "
                    + "ne produit aucun doublon. Le sens (entrée/sortie) est déduit côté serveur.")
    public ResponseEntity<ApiResponse<SyncSummaryResponse>> synchronize(
            Authentication authentication, @Valid @RequestBody PointageESP32Request request) {
        Device device = (Device) authentication.getPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success(pointageService.synchronize(device.getId(), request)));
    }

    // ------------------------------------------------------------------
    // Consultation — administration
    // ------------------------------------------------------------------

    @GetMapping("/pointages/journee")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'SUPERVISEUR')")
    @Operation(summary = "Synthèse de la journée pour tous les agents actifs",
            description = "Une ligne par agent : heure d'arrivée, heure de départ, retard "
                    + "et temps de présence. Les agents sans pointage apparaissent en ABSENT.")
    public ResponseEntity<ApiResponse<List<JourneePersonnelResponse>>> journee(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                ApiResponse.success(pointageService.journee(date == null ? LocalDate.now() : date)));
    }

    @GetMapping("/pointages")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'SUPERVISEUR')")
    @Operation(summary = "Flux brut des pointages d'une journée",
            description = "Du plus récent au plus ancien — alimente le tableau des arrivées en direct.")
    public ResponseEntity<ApiResponse<List<PointagePersonnelResponse>>> pointagesDuJour(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                pointageService.pointagesDuJour(date == null ? LocalDate.now() : date)));
    }

    @GetMapping("/pointages/personnel/{personnelId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'SUPERVISEUR')")
    @Operation(summary = "Historique des journées d'un agent sur une période")
    public ResponseEntity<ApiResponse<List<JourneePersonnelResponse>>> historique(
            @PathVariable UUID personnelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(
                ApiResponse.success(pointageService.historique(personnelId, debut, fin)));
    }

    @PostMapping("/pointages/manuel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Régulariser manuellement un pointage",
            description = "Utilisé en cas de panne du lecteur ou de non-reconnaissance répétée. "
                    + "La source enregistrée est MANUEL, ce qui rend la correction traçable.")
    public ResponseEntity<ApiResponse<PointagePersonnelResponse>> createManuel(
            @Valid @RequestBody PointageManuelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(pointageService.createManuel(request), "Pointage régularisé"));
    }
}
