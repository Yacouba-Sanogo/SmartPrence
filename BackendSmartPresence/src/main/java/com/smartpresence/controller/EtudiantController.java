package com.smartpresence.controller;

import com.smartpresence.dto.request.EnrolementBiometriqueRequest;
import com.smartpresence.dto.request.EtudiantRequest;
import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.BulletinResponse;
import com.smartpresence.dto.response.ReleveSemestreResponse;
import com.smartpresence.dto.response.CompteEtudiantResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.dto.request.ParcoursRequests;
import com.smartpresence.dto.response.ChoixOptionResponse;
import com.smartpresence.service.EtudiantService;
import com.smartpresence.service.ParcoursService;
import com.smartpresence.service.NoteService;
import com.smartpresence.service.ReleveService;
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
    private final ParcoursService parcoursService;
    private final NoteService noteService;
    private final ReleveService releveService;
    private final com.smartpresence.service.impl.ImportEtudiantsService importService;

    @Operation(summary = "Inscrire une liste d'étudiants dans une classe",
            description = "Lignes issues d'un fichier CSV, Excel ou d'une photo. Chaque ligne est traitée à part ; "
                    + "un matricule déjà inscrit est ignoré. `simulation` vérifie sans rien enregistrer.")
    @PostMapping("/import") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<com.smartpresence.dto.response.ImportEtudiantsResponse>> importer(
            @Valid @RequestBody com.smartpresence.dto.request.ImportEtudiantsRequest requete) {
        var bilan = importService.importer(requete);
        return ResponseEntity.ok(ApiResponse.success(bilan, requete.simulation()
                ? "Vérification terminée"
                : bilan.creees() + " étudiant(s) inscrit(s)"));
    }
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
    @Operation(summary = "Changer un étudiant de classe",
            description = "Transfert ou correction d'affectation. L'option choisie est retirée "
                    + "si la nouvelle classe relève d'une autre filière.")
    @PatchMapping("/{id}/classe") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<EtudiantResponse>> changerClasse(
            @PathVariable UUID id, @Valid @RequestBody ParcoursRequests.ChangementClasse request) {
        return ResponseEntity.ok(ApiResponse.success(
                parcoursService.changerClasse(id, request.classeId()), "Classe modifiée"));
    }

    @Operation(summary = "Options proposées à un étudiant et option retenue")
    @GetMapping("/{id}/option") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<ChoixOptionResponse>> options(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(parcoursService.options(id)));
    }

    @Operation(summary = "Fixer l'option d'un étudiant",
            description = "Elle décide de la classe qu'il rejoint quand sa filière se divise.")
    @PutMapping("/{id}/option") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<ChoixOptionResponse>> choisirOption(
            @PathVariable UUID id, @Valid @RequestBody ParcoursRequests.ChoixOption request) {
        return ResponseEntity.ok(ApiResponse.success(
                parcoursService.choisirOption(id, request.optionId()), "Option enregistrée"));
    }

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

    @Operation(summary = "Relevé de notes d'un étudiant",
            description = "Vue LMD : UE, ECUE, crédits et décision du semestre. "
                    + "Sans paramètre, le premier semestre de la maquette de sa promotion.")
    @GetMapping("/{id}/releve")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<ReleveSemestreResponse>> releve(
            @PathVariable UUID id,
            @RequestParam(required = false) PeriodeScolaire semestre) {
        return ResponseEntity.ok(ApiResponse.success(releveService.releveDe(id, semestre)));
    }

    @Operation(summary = "Bulletin d'un étudiant",
            description = "Vue par matière, indépendante de la maquette LMD : elle montre "
                    + "aussi les matières qu'aucune UE ne regroupe.")
    @GetMapping("/{id}/bulletin")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<BulletinResponse>> bulletin(
            @PathVariable UUID id,
            @RequestParam(required = false) PeriodeScolaire periode) {
        return ResponseEntity.ok(ApiResponse.success(noteService.bulletinDe(id, periode)));
    }

    @PatchMapping("/{id}/active") @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<Void>> setActive(@PathVariable UUID id, @RequestParam boolean value) { etudiantService.setActive(id, value); return ResponseEntity.ok(ApiResponse.success("Statut de l'étudiant mis à jour")); }
}
