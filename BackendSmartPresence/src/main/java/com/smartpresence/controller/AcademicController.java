package com.smartpresence.controller;

import com.smartpresence.dto.request.MatiereRequest;
import com.smartpresence.dto.request.SeanceRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.MatiereResponse;
import com.smartpresence.dto.response.SeanceResponse;
import com.smartpresence.service.AcademicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Référentiel des matières et planification des séances.
 *
 * <p>Le référentiel du personnel est servi par {@link PersonnelController} (mêmes URL
 * {@code /personnels}), qui porte en plus l'enrôlement biométrique.</p>
 *
 * @since 0.0.1
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
@Tag(name = "Référentiel académique", description = "Matières et emploi du temps")
public class AcademicController {

    private final AcademicService service;

    // ------------------------------------------------------------------
    // Matières
    // ------------------------------------------------------------------

    @GetMapping("/matieres")
    @Operation(summary = "Lister les matières")
    public ResponseEntity<ApiResponse<List<MatiereResponse>>> matieres() {
        return ResponseEntity.ok(ApiResponse.success(service.matieres()));
    }

    @PostMapping("/matieres")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Créer une matière")
    public ResponseEntity<ApiResponse<MatiereResponse>> createMatiere(
            @Valid @RequestBody MatiereRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createMatiere(request), "Matière créée"));
    }

    @PutMapping("/matieres/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Modifier une matière")
    public ResponseEntity<ApiResponse<MatiereResponse>> updateMatiere(
            @PathVariable Long id, @Valid @RequestBody MatiereRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(service.updateMatiere(id, request), "Matière mise à jour"));
    }

    @DeleteMapping("/matieres/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une matière",
            description = "Refusé si des séances s'y rattachent : les supprimer en cascade "
                    + "effacerait des relevés de présence.")
    public ResponseEntity<ApiResponse<Void>> deleteMatiere(@PathVariable Long id) {
        service.deleteMatiere(id);
        return ResponseEntity.ok(ApiResponse.success("Matière supprimée"));
    }

    // ------------------------------------------------------------------
    // Séances
    // ------------------------------------------------------------------

    @GetMapping("/seances")
    @Operation(summary = "Emploi du temps",
            description = "Séances d'une période, filtrables par classe et par enseignant. "
                    + "Sans période, la journée courante.")
    public ResponseEntity<ApiResponse<List<SeanceResponse>>> seances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long classeId,
            @RequestParam(required = false) UUID enseignantId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.seances(debut, fin, classeId, enseignantId)));
    }

    @PostMapping("/seances")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Planifier une séance",
            description = "Refusé si le créneau est déjà occupé par la classe, l'enseignant "
                    + "ou la salle.")
    public ResponseEntity<ApiResponse<SeanceResponse>> createSeance(
            @Valid @RequestBody SeanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createSeance(request), "Séance planifiée"));
    }

    @PutMapping("/seances/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Modifier une séance",
            description = "Refusé dès qu'une présence y est relevée : déplacer le créneau "
                    + "requalifierait rétroactivement les relevés.")
    public ResponseEntity<ApiResponse<SeanceResponse>> updateSeance(
            @PathVariable UUID id, @Valid @RequestBody SeanceRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(service.updateSeance(id, request), "Séance mise à jour"));
    }

    @DeleteMapping("/seances/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Supprimer une séance",
            description = "Refusé si des présences y sont rattachées ; l'annuler préserve "
                    + "les relevés.")
    public ResponseEntity<ApiResponse<Void>> deleteSeance(@PathVariable UUID id) {
        service.deleteSeance(id);
        return ResponseEntity.ok(ApiResponse.success("Séance supprimée"));
    }

    @PatchMapping("/seances/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Changer le statut d'une séance")
    public ResponseEntity<ApiResponse<Void>> changerStatut(
            @PathVariable UUID id, @RequestParam String value) {
        service.changeStatutSeance(id, value);
        return ResponseEntity.ok(ApiResponse.success("Statut de séance mis à jour"));
    }
}
