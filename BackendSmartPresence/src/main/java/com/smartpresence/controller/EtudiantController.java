package com.smartpresence.controller;

import com.smartpresence.dto.request.EnrolementBiometriqueRequest;
import com.smartpresence.dto.request.EtudiantRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.CompteEtudiantResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.service.EtudiantService;
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
@RequestMapping("/etudiants")
@RequiredArgsConstructor
@Tag(name = "Étudiants", description = "Référentiel des étudiants sans données biométriques")
public class EtudiantController {
    private final EtudiantService etudiantService;
    // ENSEIGNANT retire : sans classeId, cette route listait tous les etudiants de
    // l'etablissement, reference biometrique comprise. Un enseignant obtient l'effectif
    // de ses seules classes via GET /moi/classes/{classeId}/etudiants.
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<List<EtudiantResponse>>> findAll(@RequestParam(required = false) Long classeId) { return ResponseEntity.ok(ApiResponse.success(etudiantService.findAll(classeId))); }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<EtudiantResponse>> findById(@PathVariable UUID id) { return ResponseEntity.ok(ApiResponse.success(etudiantService.findById(id))); }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<EtudiantResponse>> create(@Valid @RequestBody EtudiantRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(etudiantService.create(request))); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<EtudiantResponse>> update(@PathVariable UUID id, @Valid @RequestBody EtudiantRequest request) { return ResponseEntity.ok(ApiResponse.success(etudiantService.update(id, request))); }
    @Operation(summary = "Enrôler l'empreinte d'un étudiant",
            description = "Associe la référence logique de l'empreinte capturée sur le lecteur. "
                    + "Aucune donnée biométrique n'est transmise ni stockée.")
    @PostMapping("/{id}/enrolement")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<EtudiantResponse>> enroler(
            @PathVariable UUID id, @Valid @RequestBody EnrolementBiometriqueRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                etudiantService.enroler(id, request.getBiometricId()),
                "Étudiant enrôlé — sa présence peut désormais être relevée"));
    }

    @Operation(summary = "Révoquer l'enrôlement d'un étudiant",
            description = "Les présences déjà relevées sont conservées.")
    @DeleteMapping("/{id}/enrolement")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<EtudiantResponse>> revoquerEnrolement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                etudiantService.revoquerEnrolement(id), "Enrôlement révoqué"));
    }

    @Operation(summary = "Ouvrir l'accès mobile d'un étudiant",
            description = "Crée son compte, lui attribue le rôle ETUDIANT et renvoie un mot de "
                    + "passe initial aléatoire — affiché une seule fois.")
    @PostMapping("/{id}/compte")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<CompteEtudiantResponse>> ouvrirCompte(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                etudiantService.ouvrirCompte(id),
                "Accès mobile créé — notez le mot de passe, il ne sera plus affiché"));
    }

    @Operation(summary = "Fermer l'accès mobile d'un étudiant",
            description = "Les présences déjà relevées sont conservées.")
    @DeleteMapping("/{id}/compte")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<EtudiantResponse>> fermerCompte(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(etudiantService.fermerCompte(id), "Accès mobile fermé"));
    }

    @PatchMapping("/{id}/active") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<Void>> setActive(@PathVariable UUID id, @RequestParam boolean value) { etudiantService.setActive(id, value); return ResponseEntity.ok(ApiResponse.success("Statut de l'étudiant mis à jour")); }
}
