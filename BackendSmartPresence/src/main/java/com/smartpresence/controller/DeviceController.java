package com.smartpresence.controller;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.dto.request.DeviceRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.DeviceResponse;
import com.smartpresence.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "Appareils", description = "Gestion des appareils ESP32")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Lister les appareils", description = "Récupère la liste de tous les appareils ESP32")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(deviceService.findAll(), "Appareils récupérés"));
    }

    @Operation(summary = "Trouver un appareil par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<DeviceResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.findById(id)));
    }

    @Operation(summary = "Créer un appareil")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeviceResponse>> create(@Valid @RequestBody DeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(deviceService.create(request), "Appareil créé avec succès"));
    }

    @Operation(summary = "Modifier un appareil")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DeviceResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody DeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.update(id, request), "Appareil mis à jour"));
    }

    @Operation(summary = "Supprimer un appareil")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        deviceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Appareil supprimé avec succès"));
    }

    @Operation(summary = "Changer le statut d'un appareil")
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setStatut(
            @PathVariable UUID id, @RequestParam DeviceStatut statut) {
        deviceService.setStatut(id, statut);
        return ResponseEntity.ok(ApiResponse.success("Statut de l'appareil mis à jour"));
    }
}
