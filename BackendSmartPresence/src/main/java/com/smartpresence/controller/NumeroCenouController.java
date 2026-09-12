package com.smartpresence.controller;

import com.smartpresence.dto.request.NumeroCenouRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.ImportCenouResponse;
import com.smartpresence.dto.response.NumeroCenouResponse;
import com.smartpresence.service.NumeroCenouService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Référentiel des numéros CENOU : la liste blanche des inscriptions autorisées.
 */
@RestController
@RequestMapping("/cenou")
@RequiredArgsConstructor
@Tag(name = "Numéros CENOU",
        description = "Liste blanche des numéros autorisés à s'inscrire")
public class NumeroCenouController {

    private final NumeroCenouService numeroCenouService;

    @Operation(summary = "Lister les numéros CENOU",
            description = "Le paramètre « utilise » filtre les numéros déjà consommés "
                    + "par une inscription, ou au contraire ceux encore disponibles.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<List<NumeroCenouResponse>>> findAll(
            @RequestParam(required = false) Boolean utilise) {
        return ResponseEntity.ok(ApiResponse.success(
                numeroCenouService.findAll(utilise), "Référentiel CENOU récupéré"));
    }

    @Operation(summary = "Ajouter un numéro CENOU",
            description = "Saisie unitaire, pour un étudiant arrivé hors de la liste initiale.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<NumeroCenouResponse>> create(
            @Valid @RequestBody NumeroCenouRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                numeroCenouService.create(request), "Numéro CENOU ajouté au référentiel"));
    }

    @Operation(summary = "Importer une liste de numéros CENOU",
            description = "Accepte un classeur .xlsx (ou .xls) et les fichiers .csv. "
                    + "Colonnes attendues dans cet ordre : numéro, nom, prénom — seule la "
                    + "première est obligatoire, et une ligne d'en-tête est reconnue. "
                    + "Le paramètre « classeId » affecte d'emblée toute la liste à une classe.")
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<ImportCenouResponse>> importer(
            @RequestPart("fichier") MultipartFile fichier,
            @RequestParam(required = false) Long classeId) {
        ImportCenouResponse compteRendu = numeroCenouService.importer(fichier, classeId);
        return ResponseEntity.ok(ApiResponse.success(compteRendu,
                compteRendu.getAjoutes() + " numéro(s) ajouté(s) au référentiel"));
    }

    @Operation(summary = "Retirer un numéro CENOU",
            description = "Refusé si le numéro a déjà servi à une inscription.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        numeroCenouService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Numéro CENOU retiré du référentiel"));
    }
}
