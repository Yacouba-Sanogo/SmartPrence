package com.smartpresence.controller;

import com.smartpresence.constants.StatutSignalement;
import com.smartpresence.dto.request.TraitementSignalementRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.SignalementResponse;
import com.smartpresence.security.CustomUserDetails;
import com.smartpresence.service.SignalementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Arbitrage des signalements d'anomalie, côté scolarité.
 *
 * <p>Le dépôt se fait depuis l'espace enseignant ({@code POST /moi/seances/&#123;id&#125;/signalements}) :
 * celui qui témoigne et celui qui tranche n'empruntent pas le même chemin, et c'est
 * délibéré.</p>
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/signalements")
@RequiredArgsConstructor
@Tag(name = "Signalements", description = "Anomalies de relevé signalées par les enseignants")
public class SignalementController {

    private final SignalementService signalementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    @Operation(summary = "File d'arbitrage",
            description = "Signalements déposés, filtrables par statut.")
    public ResponseEntity<ApiResponse<List<SignalementResponse>>> rechercher(
            @RequestParam(required = false) StatutSignalement statut) {
        return ResponseEntity.ok(ApiResponse.success(signalementService.rechercher(statut)));
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    @Operation(summary = "Arbitrer un signalement",
            description = "Accepter un signalement d'étudiant non reconnu produit un relevé "
                    + "de source MANUEL, rattaché au signalement qui le justifie.")
    public ResponseEntity<ApiResponse<SignalementResponse>> traiter(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails arbitre,
            @Valid @RequestBody TraitementSignalementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                signalementService.traiter(id, arbitre.getId(), request),
                Boolean.TRUE.equals(request.getAccepte())
                        ? "Signalement retenu" : "Signalement écarté"));
    }
}
