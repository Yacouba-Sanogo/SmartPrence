package com.smartpresence.controller;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.request.UniteEnseignementRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.UniteEnseignementResponse;
import com.smartpresence.service.UniteEnseignementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Maquette pédagogique : unités d'enseignement et crédits.
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/unites-enseignement")
@RequiredArgsConstructor
@Tag(name = "Unités d'enseignement",
        description = "Maquette pédagogique — UE, crédits et semestres")
public class UniteEnseignementController {

    private final UniteEnseignementService uniteService;

    @Operation(summary = "Lister les UE",
            description = "Filtrable par promotion et par semestre. Chaque UE porte la "
                    + "liste de ses ECUE et le total de leurs crédits, pour vérifier "
                    + "d'un coup d'œil que la maquette est complète.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR', 'ENSEIGNANT')")
    public ResponseEntity<ApiResponse<List<UniteEnseignementResponse>>> findAll(
            @RequestParam(required = false) Long promotionId,
            @RequestParam(required = false) PeriodeScolaire semestre) {
        return ResponseEntity.ok(ApiResponse.success(
                uniteService.findAll(promotionId, semestre), "Maquette récupérée"));
    }

    @Operation(summary = "Trouver une UE par identifiant")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR', 'ENSEIGNANT')")
    public ResponseEntity<ApiResponse<UniteEnseignementResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(uniteService.findById(id)));
    }

    @Operation(summary = "Créer une UE")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<UniteEnseignementResponse>> create(
            @Valid @RequestBody UniteEnseignementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                uniteService.create(request), "UE ajoutée à la maquette"));
    }

    @Operation(summary = "Modifier une UE")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<UniteEnseignementResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UniteEnseignementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(uniteService.update(id, request), "UE modifiée"));
    }

    @Operation(summary = "Supprimer une UE",
            description = "Refusé tant que des matières lui sont rattachées.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        uniteService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("UE retirée de la maquette"));
    }
}
