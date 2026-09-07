package com.smartpresence.controller;

import com.smartpresence.dto.request.PresenceESP32Request;
import com.smartpresence.dto.request.PresenceManuelRequest;
import com.smartpresence.dto.request.PresenceSearchCriteria;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.PagedResponse;
import com.smartpresence.dto.response.PresenceResponse;
import com.smartpresence.dto.response.SyncSummaryResponse;
import com.smartpresence.entity.Device;
import com.smartpresence.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Présences", description = "Consultation et enregistrement des présences")
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/presences")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    @Operation(summary = "Rechercher les présences")
    public ResponseEntity<ApiResponse<PagedResponse<PresenceResponse>>> search(PresenceSearchCriteria criteria) {
        return ResponseEntity.ok(ApiResponse.success(presenceService.search(criteria)));
    }

    // ENSEIGNANT retire : l'enseignant temoigne, la scolarite arbitre. Lui laisser
    // creer un releve directement contournait le circuit de signalement et effacait la
    // distinction entre une presence constatee par le capteur et une presence decidee.
    @PostMapping("/presences/manual")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Saisir une présence manuelle")
    public ResponseEntity<ApiResponse<PresenceResponse>> createManual(@Valid @RequestBody PresenceManuelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(presenceService.createManual(request)));
    }

    @PostMapping("/esp32/presences/synchronize")
    @PreAuthorize("hasRole('DEVICE')")
    @Operation(summary = "Synchroniser un lot de présences ESP32")
    public ResponseEntity<ApiResponse<SyncSummaryResponse>> synchronize(
            Authentication authentication, @Valid @RequestBody PresenceESP32Request request) {
        Device device = (Device) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(presenceService.synchronize(device.getId(), request)));
    }
}
