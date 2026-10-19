package com.smartpresence.controller;

import com.smartpresence.dto.request.ReferentielRequests;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.DepartementResponse;
import com.smartpresence.dto.response.FiliereResponse;
import com.smartpresence.service.ReferentielService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Départements (DER), filières et options. La lecture est publique : un futur candidat
 * consulte les filières avant d'avoir un compte. L'écriture reste à l'administration.
 */
@RestController
@RequestMapping("/referentiel")
@RequiredArgsConstructor
@Tag(name = "Référentiel", description = "DER, filières et options de l'ENETP")
public class ReferentielController {

    private final ReferentielService referentielService;

    @Operation(summary = "Lister les départements (DER)")
    @GetMapping("/departements")
    public ResponseEntity<ApiResponse<List<DepartementResponse>>> departements() {
        return ResponseEntity.ok(ApiResponse.success(referentielService.departements()));
    }

    @Operation(summary = "Lister les filières avec leur DER et leurs options")
    @GetMapping("/filieres")
    public ResponseEntity<ApiResponse<List<FiliereResponse>>> filieres() {
        return ResponseEntity.ok(ApiResponse.success(referentielService.filieres()));
    }

    @Operation(summary = "Créer un département")
    @PostMapping("/departements")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<DepartementResponse>> creerDepartement(
            @Valid @RequestBody ReferentielRequests.Departement requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                referentielService.creerDepartement(requete), "Département créé"));
    }

    @Operation(summary = "Créer une filière")
    @PostMapping("/filieres")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<FiliereResponse>> creerFiliere(
            @Valid @RequestBody ReferentielRequests.Filiere requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                referentielService.creerFiliere(requete), "Filière créée"));
    }

    @Operation(summary = "Ajouter une option à une filière")
    @PostMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<FiliereResponse>> ajouterOption(
            @Valid @RequestBody ReferentielRequests.Option requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                referentielService.ajouterOption(requete), "Option ajoutée"));
    }

    @Operation(summary = "Supprimer une option")
    @DeleteMapping("/options/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<Void>> supprimerOption(@PathVariable Long id) {
        referentielService.supprimerOption(id);
        return ResponseEntity.ok(ApiResponse.success("Option supprimée"));
    }
}
