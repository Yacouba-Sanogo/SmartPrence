package com.smartpresence.controller;

import com.smartpresence.dto.request.SalleRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.SalleResponse;
import com.smartpresence.service.SalleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/salles")
@RequiredArgsConstructor
@Tag(name = "Salles", description = "Gestion des salles physiques")
public class SalleController {

    private final SalleService salleService;

    @Operation(summary = "Lister les salles", description = "Récupère la liste de toutes les salles")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<List<SalleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(salleService.findAll(), "Salles récupérées"));
    }

    @Operation(summary = "Trouver une salle par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<SalleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(salleService.findById(id)));
    }

    @Operation(summary = "Créer une salle")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<SalleResponse>> create(@Valid @RequestBody SalleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(salleService.create(request), "Salle créée avec succès"));
    }

    @Operation(summary = "Modifier une salle")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<SalleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody SalleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salleService.update(id, request), "Salle mise à jour"));
    }

    @Operation(summary = "Supprimer une salle")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        salleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Salle supprimée avec succès"));
    }
}
