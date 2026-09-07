package com.smartpresence.controller;

import com.smartpresence.dto.request.PromotionRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.PromotionResponse;
import com.smartpresence.service.PromotionService;
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
@RequestMapping("/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Gestion des promotions académiques")
public class PromotionController {

    private final PromotionService promotionService;

    @Operation(summary = "Lister les promotions", description = "Récupère la liste de toutes les promotions")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(promotionService.findAll(), "Promotions récupérées"));
    }

    @Operation(summary = "Trouver une promotion par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<PromotionResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.findById(id)));
    }

    @Operation(summary = "Créer une promotion", description = "Ajoute une nouvelle promotion universitaire")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<PromotionResponse>> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(promotionService.create(request), "Promotion créée avec succès"));
    }

    @Operation(summary = "Modifier une promotion")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.update(id, request), "Promotion mise à jour"));
    }

    @Operation(summary = "Supprimer une promotion")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Promotion supprimée avec succès"));
    }
}
