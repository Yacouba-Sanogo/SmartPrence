package com.smartpresence.controller;

import com.smartpresence.constants.StatutJustification;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.JustificationAbsenceResponse;
import com.smartpresence.security.CustomUserDetails;
import com.smartpresence.service.JustificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/justifications")
@RequiredArgsConstructor
@Tag(name = "Justifications", description = "Gestion des justifications d'absence")
public class JustificationAbsenceController {

    private final JustificationService justificationService;

    @Operation(summary = "Lister toutes les justifications")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<List<JustificationAbsenceResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(justificationService.findAll(), "Justifications récupérées"));
    }

    @Operation(summary = "Lister les justifications d'un étudiant")
    @GetMapping("/etudiant/{etudiantId}")
    // Un justificatif d'absence peut contenir un motif medical. « isAuthenticated() »
    // ouvrait ces pieces a tout compte connecte, y compris a un autre etudiant.
    // L'etudiant accede aux siens via GET /moi/justificatifs.
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<List<JustificationAbsenceResponse>>> findByEtudiant(@PathVariable UUID etudiantId) {
        return ResponseEntity.ok(ApiResponse.success(
                justificationService.findByEtudiant(etudiantId), "Justifications de l'étudiant récupérées"));
    }

    @Operation(summary = "Créer une demande de justification")
    @PostMapping
    // « isAuthenticated() » acceptait un etudiantId arbitraire : n'importe quel compte
    // pouvait deposer un justificatif au nom d'un autre etudiant. L'etudiant depose le
    // sien via POST /moi/justificatifs, ou l'identite vient du jeton.
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<JustificationAbsenceResponse>> create(
            @RequestParam UUID etudiantId,
            @RequestParam String motif,
            @RequestParam LocalDate dateAbsence,
            @RequestParam(required = false) UUID seanceId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(justificationService.create(etudiantId, motif, dateAbsence, seanceId),
                        "Justification créée avec succès"));
    }

    @Operation(summary = "Traiter une justification")
    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<Void>> traiter(
            @PathVariable UUID id,
            @RequestParam StatutJustification statut,
            @RequestParam(required = false) String commentaire,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        justificationService.traiter(id, statut, userDetails.getId(), commentaire);
        return ResponseEntity.ok(ApiResponse.success("Justification traitée avec succès"));
    }
}
