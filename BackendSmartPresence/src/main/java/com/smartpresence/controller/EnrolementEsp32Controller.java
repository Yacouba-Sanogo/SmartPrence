package com.smartpresence.controller;

import com.smartpresence.dto.request.ReferenceEnrolementRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.DemandeEnrolementResponse;
import com.smartpresence.dto.response.EnrolementAServirResponse;
import com.smartpresence.entity.Device;
import com.smartpresence.service.EnrolementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Enrôlement biométrique, côté lecteur.
 *
 * <p>Deux adresses, et rien de plus : demander qui est attendu, puis annoncer la
 * référence produite. Le lecteur ne reçoit que de quoi afficher un nom et un code —
 * jamais un identifiant interne — et ne transmet jamais de gabarit d'empreinte
 * (cf. {@code SmartPresence_CONTEXT.md} §13).</p>
 *
 * <p>L'authentification se fait par clé d'API sur l'en-tête {@code X-API-KEY},
 * comme pour l'ingestion des relevés : un lecteur n'est pas un utilisateur.</p>
 *
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/esp32/enrolements")
@RequiredArgsConstructor
@Tag(name = "Enrôlement (lecteur)",
        description = "File d'attente des enrôlements, à l'usage des lecteurs ESP32")
public class EnrolementEsp32Controller {

    private final EnrolementService enrolementService;

    @Operation(summary = "Qui est attendu devant ce lecteur",
            description = "Rend la plus ancienne demande en attente et la réserve à ce "
                    + "lecteur pour quelques minutes, afin que deux capteurs voisins "
                    + "n'appellent pas le même étudiant. Répond 204 si personne n'attend.")
    @GetMapping("/prochain")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<ApiResponse<EnrolementAServirResponse>> prochain(
            Authentication authentication) {
        Device lecteur = (Device) authentication.getPrincipal();
        Optional<EnrolementAServirResponse> demande = enrolementService.prochaine(lecteur.getId());

        // 204 plutôt qu'une enveloppe vide : le firmware interroge cette adresse en
        // boucle, et un corps de réponse à chaque passage pèserait pour rien sur une
        // liaison Wi-Fi partagée.
        return demande
                .map(valeur -> ResponseEntity.ok(
                        ApiResponse.success(valeur, "Demande d'enrôlement à servir")))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Annoncer la référence produite par la capture",
            description = "Associe à l'étudiant l'emplacement occupé dans la mémoire du "
                    + "capteur. Aucune donnée biométrique n'est transmise.")
    @PostMapping
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<ApiResponse<DemandeEnrolementResponse>> enregistrer(
            Authentication authentication,
            @Valid @RequestBody ReferenceEnrolementRequest request) {
        Device lecteur = (Device) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                enrolementService.enregistrer(
                        lecteur.getId(), request.getCode(), request.getReference()),
                "Empreinte enregistrée"));
    }
}
