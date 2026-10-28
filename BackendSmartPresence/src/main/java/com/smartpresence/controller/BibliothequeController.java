package com.smartpresence.controller;

import com.smartpresence.constants.TypeDocument;
import com.smartpresence.dto.request.BibliothequeRequests;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.BibliothequeResponses;
import com.smartpresence.security.CustomUserDetails;
import com.smartpresence.service.BibliothequeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Bibliothèque : mémoires et anciens sujets numériques, livres physiques et emprunts.
 *
 * <p>Tout le monde consulte ; l'administration (ADMIN, RESPONSABLE_SCOLARITE) tient le fonds,
 * les emprunts et la modération ; l'étudiant dépose, et son dépôt attend la décision.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bibliotheque")
@Tag(name = "Bibliothèque", description = "Mémoires, anciens sujets, livres physiques et emprunts")
public class BibliothequeController {

    private static final String LECTEURS = "hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR', 'ENSEIGNANT', 'ETUDIANT')";
    private static final String GESTION = "hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')";

    private final BibliothequeService service;

    private static boolean gestionnaire(CustomUserDetails u) {
        return u.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_RESPONSABLE_SCOLARITE"));
    }

    // ----- Documents numériques -----------------------------------------

    @GetMapping("/documents")
    @PreAuthorize(LECTEURS)
    @Operation(summary = "Mémoires et anciens sujets publiés",
            description = "Filtrables par type, filière, année et mot-clé.")
    public ResponseEntity<ApiResponse<List<BibliothequeResponses.Document>>> documents(
            @RequestParam(required = false) TypeDocument type, @RequestParam(required = false) Long filiereId,
            @RequestParam(required = false) Integer annee, @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(service.documents(type, filiereId, annee, q)));
    }

    @GetMapping("/documents/a-moderer")
    @PreAuthorize(GESTION)
    @Operation(summary = "Dépôts d'étudiants en attente de décision")
    public ResponseEntity<ApiResponse<List<BibliothequeResponses.Document>>> aModerer() {
        return ResponseEntity.ok(ApiResponse.success(service.documentsAModerer()));
    }

    @GetMapping("/mes-depots")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mes dépôts et leur décision")
    public ResponseEntity<ApiResponse<List<BibliothequeResponses.Document>>> mesDepots(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(service.mesDepots(utilisateur.getId())));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'ETUDIANT')")
    @Operation(summary = "Déposer un mémoire ou un sujet",
            description = "Jusqu'à 25 Mo. Publié d'emblée s'il vient de l'administration, "
                    + "sinon en attente de sa décision.")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Document>> deposer(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @RequestParam TypeDocument type,
            @RequestParam String titre, @RequestParam(required = false) String auteur,
            @RequestParam(required = false) String matiere, @RequestParam(required = false) Long filiereId,
            @RequestParam(required = false) Integer annee, @RequestParam(required = false) String description,
            @RequestParam("fichier") MultipartFile fichier) {
        boolean gestion = gestionnaire(utilisateur);
        BibliothequeResponses.Document doc = service.deposer(utilisateur.getId(), gestion, type, titre, auteur,
                matiere, filiereId, annee, description, fichier);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(doc,
                gestion ? "Document publié" : "Dépôt reçu : il sera publié après validation"));
    }

    @PostMapping("/documents/{id}/decision")
    @PreAuthorize(GESTION)
    @Operation(summary = "Accepter ou refuser un dépôt", description = "Le déposant est prévenu ; le refus exige un motif.")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Document>> decider(
            @PathVariable UUID id, @Valid @RequestBody BibliothequeRequests.Decision decision) {
        return ResponseEntity.ok(ApiResponse.success(service.decider(id, decision), "Décision enregistrée"));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'ETUDIANT')")
    @Operation(summary = "Retirer un document",
            description = "L'administration retire tout ; un étudiant, seulement ses dépôts non publiés.")
    public ResponseEntity<ApiResponse<Void>> retirer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                                     @PathVariable UUID id) {
        service.retirer(utilisateur.getId(), gestionnaire(utilisateur), id);
        return ResponseEntity.ok(ApiResponse.success("Document retiré"));
    }

    @GetMapping("/documents/{id}/fichier")
    @PreAuthorize(LECTEURS)
    @Operation(summary = "Télécharger un document")
    public ResponseEntity<byte[]> telecharger(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                              @PathVariable UUID id) {
        var fichier = service.telecharger(utilisateur.getId(), gestionnaire(utilisateur), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.typeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fichier.nom(), StandardCharsets.UTF_8).build().toString())
                .contentLength(fichier.octets().length)
                .body(fichier.octets());
    }

    // ----- Livres ---------------------------------------------------------

    @GetMapping("/livres")
    @PreAuthorize(LECTEURS)
    @Operation(summary = "Catalogue des livres physiques", description = "Avec le nombre d'exemplaires disponibles.")
    public ResponseEntity<ApiResponse<List<BibliothequeResponses.Livre>>> livres(
            @RequestParam(required = false) Long filiereId, @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(service.livres(filiereId, q)));
    }

    @PostMapping("/livres")
    @PreAuthorize(GESTION)
    @Operation(summary = "Ajouter un livre au fonds")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Livre>> creerLivre(
            @Valid @RequestBody BibliothequeRequests.Livre requete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.creerLivre(requete), "Livre ajouté"));
    }

    @PutMapping("/livres/{id}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Modifier un livre")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Livre>> modifierLivre(
            @PathVariable UUID id, @Valid @RequestBody BibliothequeRequests.Livre requete) {
        return ResponseEntity.ok(ApiResponse.success(service.modifierLivre(id, requete), "Livre modifié"));
    }

    @DeleteMapping("/livres/{id}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Retirer un livre du fonds", description = "Refusé s'il a un historique d'emprunts.")
    public ResponseEntity<ApiResponse<Void>> supprimerLivre(@PathVariable UUID id) {
        service.supprimerLivre(id);
        return ResponseEntity.ok(ApiResponse.success("Livre retiré"));
    }

    // ----- Emprunts -------------------------------------------------------

    @GetMapping("/emprunts")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE', 'SUPERVISEUR')")
    @Operation(summary = "Tous les emprunts", description = "Avec le statut calculé et les jours de retard.")
    public ResponseEntity<ApiResponse<List<BibliothequeResponses.Emprunt>>> emprunts() {
        return ResponseEntity.ok(ApiResponse.success(service.emprunts()));
    }

    @GetMapping("/synthese")
    @PreAuthorize(GESTION)
    @Operation(summary = "Chiffres clés : dépôts en attente, emprunts en cours et en retard")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Synthese>> synthese() {
        return ResponseEntity.ok(ApiResponse.success(service.synthese()));
    }

    @GetMapping("/mes-emprunts")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mes emprunts, avec les dates de retour")
    public ResponseEntity<ApiResponse<List<BibliothequeResponses.Emprunt>>> mesEmprunts(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(service.mesEmprunts(utilisateur.getId())));
    }

    @PostMapping("/emprunts")
    @PreAuthorize(GESTION)
    @Operation(summary = "Enregistrer un emprunt",
            description = "14 jours par défaut ; 3 livres au plus par étudiant, aucun s'il a un retard.")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Emprunt>> emprunter(
            @Valid @RequestBody BibliothequeRequests.Emprunt requete) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.emprunter(requete), "Emprunt enregistré"));
    }

    @PostMapping("/emprunts/{id}/retour")
    @PreAuthorize(GESTION)
    @Operation(summary = "Enregistrer le retour d'un livre")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Emprunt>> retourner(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.retourner(id), "Retour enregistré"));
    }

    @PostMapping("/emprunts/{id}/prolongation")
    @PreAuthorize(GESTION)
    @Operation(summary = "Prolonger un emprunt", description = "Deux fois au plus, et jamais un livre déjà en retard.")
    public ResponseEntity<ApiResponse<BibliothequeResponses.Emprunt>> prolonger(
            @PathVariable UUID id, @Valid @RequestBody BibliothequeRequests.Prolongation prolongation) {
        return ResponseEntity.ok(ApiResponse.success(service.prolonger(id, prolongation.jours()), "Emprunt prolongé"));
    }
}
