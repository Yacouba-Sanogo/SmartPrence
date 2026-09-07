package com.smartpresence.controller;

import com.smartpresence.dto.request.ClasseRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.ClasseDetailResponse;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.service.ClasseService;
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
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "Gestion des classes")
public class ClasseController {

    private final ClasseService classeService;

    @Operation(summary = "Lister les classes", description = "Récupère la liste de toutes les classes")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<List<ClasseResponse>>> findAll(
            @RequestParam(required = false) Long promotionId) {
        List<ClasseResponse> classes = promotionId != null
                ? classeService.findByPromotionId(promotionId)
                : classeService.findAll();
        return ResponseEntity.ok(ApiResponse.success(classes, "Classes récupérées"));
    }

    @Operation(summary = "Trouver une classe par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<ClasseDetailResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classeService.findById(id)));
    }

    @Operation(summary = "Créer une classe")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<ClasseResponse>> create(@Valid @RequestBody ClasseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(classeService.create(request), "Classe créée avec succès"));
    }

    @Operation(summary = "Modifier une classe")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<ClasseResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ClasseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(classeService.update(id, request), "Classe mise à jour"));
    }

    @Operation(summary = "Supprimer une classe")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        classeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Classe supprimée avec succès"));
    }
}
