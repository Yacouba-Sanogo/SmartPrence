package com.smartpresence.controller;

import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.dto.request.EnrolementBiometriqueRequest;
import com.smartpresence.dto.request.PersonnelRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.ComptePersonnelResponse;
import com.smartpresence.dto.response.PersonnelResponse;
import com.smartpresence.service.PersonnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints du référentiel du personnel et de son enrôlement biométrique.
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/personnels")
@RequiredArgsConstructor
@Tag(name = "Personnel", description = "Référentiel des agents et enrôlement biométrique")
public class PersonnelController {

    private final PersonnelService personnelService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    @Operation(summary = "Lister les agents",
            description = "Filtrable par catégorie et par état d'activité.")
    public ResponseEntity<ApiResponse<List<PersonnelResponse>>> findAll(
            @RequestParam(required = false) TypePersonnel type,
            @RequestParam(defaultValue = "false") boolean actifsSeul) {
        return ResponseEntity.ok(ApiResponse.success(personnelService.findAll(type, actifsSeul)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    @Operation(summary = "Consulter un agent")
    public ResponseEntity<ApiResponse<PersonnelResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(personnelService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Créer un agent")
    public ResponseEntity<ApiResponse<PersonnelResponse>> create(
            @Valid @RequestBody PersonnelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(personnelService.create(request), "Agent créé avec succès"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Mettre à jour un agent")
    public ResponseEntity<ApiResponse<PersonnelResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PersonnelRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(personnelService.update(id, request), "Agent mis à jour"));
    }

    @PatchMapping("/{id}/actif")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Activer ou désactiver un agent",
            description = "Un agent inactif ne peut plus pointer.")
    public ResponseEntity<ApiResponse<PersonnelResponse>> setActif(
            @PathVariable UUID id, @RequestParam boolean value) {
        return ResponseEntity.ok(ApiResponse.success(personnelService.setActif(id, value),
                value ? "Agent activé" : "Agent désactivé"));
    }

    @PostMapping("/{id}/compte")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Ouvrir l'acces applicatif d'un agent",
            description = "Cree son compte et lui attribue le role correspondant a sa "
                    + "categorie. Le mot de passe initial n'est affiche qu'une fois.")
    public ResponseEntity<ApiResponse<ComptePersonnelResponse>> ouvrirCompte(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                personnelService.ouvrirCompte(id),
                "Acces cree - notez le mot de passe, il ne sera plus affiche"));
    }

    @DeleteMapping("/{id}/compte")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Fermer l'acces applicatif d'un agent",
            description = "Les pointages deja releves sont conserves.")
    public ResponseEntity<ApiResponse<PersonnelResponse>> fermerCompte(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(personnelService.fermerCompte(id), "Acces ferme"));
    }

    @PostMapping("/{id}/enrolement")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Enrôler l'empreinte d'un agent",
            description = "Associe la référence logique de l'empreinte capturée sur le lecteur. "
                    + "Aucune donnée biométrique n'est transmise ni stockée.")
    public ResponseEntity<ApiResponse<PersonnelResponse>> enroler(
            @PathVariable UUID id, @Valid @RequestBody EnrolementBiometriqueRequest request) {
        return ResponseEntity.ok(ApiResponse.success(personnelService.enroler(id, request),
                "Agent enrôlé — il peut désormais pointer"));
    }

    @DeleteMapping("/{id}/enrolement")
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Révoquer l'enrôlement d'un agent",
            description = "Les pointages déjà enregistrés sont conservés.")
    public ResponseEntity<ApiResponse<PersonnelResponse>> revoquerEnrolement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(personnelService.revoquerEnrolement(id),
                "Enrôlement révoqué"));
    }
}
