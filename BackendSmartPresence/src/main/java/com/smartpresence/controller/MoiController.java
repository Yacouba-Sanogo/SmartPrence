package com.smartpresence.controller;

import com.smartpresence.dto.request.SignalementRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.dto.request.NoteRequest;
import com.smartpresence.dto.response.BulletinResponse;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.dto.response.DemandeEnrolementResponse;
import com.smartpresence.dto.response.NoteResponse;
import com.smartpresence.dto.response.EffectifEtudiantResponse;
import com.smartpresence.dto.response.FeuilleSeanceResponse;
import com.smartpresence.dto.response.JustificationAbsenceResponse;
import com.smartpresence.dto.response.MatiereResponse;
import com.smartpresence.dto.response.PagedResponse;
import com.smartpresence.dto.response.PresenceResponse;
import com.smartpresence.dto.response.ReleveSemestreResponse;
import com.smartpresence.dto.response.ProfilResponse;
import com.smartpresence.dto.response.SeanceResponse;
import com.smartpresence.dto.response.SignalementResponse;
import com.smartpresence.dto.response.StatistiquesEtudiantResponse;
import com.smartpresence.security.CustomUserDetails;
import com.smartpresence.service.EnrolementService;
import com.smartpresence.service.ReleveService;
import com.smartpresence.service.ProfilService;
import com.smartpresence.service.JustificationService;
import com.smartpresence.service.AcademicService;
import com.smartpresence.service.NoteService;
import com.smartpresence.service.SignalementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Données personnelles de l'utilisateur connecté — socle de l'application mobile.
 *
 * <h2>Pourquoi un espace dédié plutôt que des identifiants dans l'URL</h2>
 * <p>Aucun endpoint d'ici n'accepte d'identifiant de personne : le sujet est déduit du
 * jeton. Un étudiant ne peut donc pas consulter le dossier d'un autre en changeant un
 * chiffre dans l'adresse — la garantie est <b>structurelle</b>, et non portée par un
 * contrôle qu'un développeur pourrait omettre sur un futur endpoint.</p>
 *
 * @since 0.0.1
 */
@RestController
@RequestMapping("/moi")
@RequiredArgsConstructor
@Tag(name = "Mon espace", description = "Données de l'utilisateur connecté (application mobile)")
public class MoiController {

    private static final int TAILLE_PAGE_MAX = 100;

    private final ProfilService profilService;
    private final SignalementService signalementService;
    private final JustificationService justificationService;
    private final NoteService noteService;
    private final AcademicService academicService;
    private final EnrolementService enrolementService;
    private final ReleveService releveService;

    @GetMapping("/profil")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mon identité et mon profil métier",
            description = "Résout le rattachement du compte : étudiant, enseignant, agent ou aucun. "
                    + "Appelé au démarrage de l'application mobile pour savoir quels écrans ouvrir.")
    public ResponseEntity<ApiResponse<ProfilResponse>> profil(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(profilService.profil(utilisateur.getId())));
    }

    @GetMapping("/presences")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mes présences",
            description = "Relevés de l'étudiant connecté sur la période demandée.")
    public ResponseEntity<ApiResponse<PagedResponse<PresenceResponse>>> mesPresences(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        return ResponseEntity.ok(ApiResponse.success(profilService.mesPresences(
                utilisateur.getId(), debut, fin, Math.max(0, page),
                Math.min(TAILLE_PAGE_MAX, Math.max(1, taille)))));
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mon assiduité")
    public ResponseEntity<ApiResponse<StatistiquesEtudiantResponse>> mesStatistiques(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(
                ApiResponse.success(profilService.mesStatistiques(utilisateur.getId())));
    }

    @GetMapping("/justificatifs")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mes justificatifs d'absence")
    public ResponseEntity<ApiResponse<List<JustificationAbsenceResponse>>> mesJustificatifs(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(
                ApiResponse.success(profilService.mesJustificatifs(utilisateur.getId())));
    }

    @GetMapping("/bulletin")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mon bulletin",
            description = "Notes groupées par matière, avec les moyennes calculées par le "
                    + "serveur. Sans période, l'année entière.")
    public ResponseEntity<ApiResponse<BulletinResponse>> monBulletin(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam(required = false) PeriodeScolaire periode) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.monBulletin(utilisateur.getId(), periode)));
    }

    @GetMapping("/emploi-du-temps")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mon emploi du temps",
            description = "Séances de ma classe sur la période demandée. Sans paramètre, "
                    + "les sept jours à venir — ce qu'on vient chercher en ouvrant l'écran.")
    public ResponseEntity<ApiResponse<List<SeanceResponse>>> monEmploiDuTemps(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate premier = debut == null ? LocalDate.now() : debut;
        LocalDate dernier = fin == null ? premier.plusDays(6) : fin;
        if (dernier.isBefore(premier)) {
            throw new BusinessException("La date de fin précède la date de début");
        }
        return ResponseEntity.ok(ApiResponse.success(
                profilService.monEmploiDuTemps(utilisateur.getId(), premier, dernier)));
    }

    @GetMapping("/releve")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mon relevé de notes",
            description = "Vue LMD du semestre : UE, ECUE avec note de devoir et note "
                    + "d'examen, crédits acquis et décision. Toutes les moyennes sont "
                    + "calculées par le serveur — les recalculer ici produirait, au moindre "
                    + "écart d'arrondi, un relevé différent de celui de l'administration. "
                    + "Sans paramètre, le premier semestre de la maquette.")
    public ResponseEntity<ApiResponse<ReleveSemestreResponse>> monReleve(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam(required = false) PeriodeScolaire semestre) {
        return ResponseEntity.ok(ApiResponse.success(
                releveService.monReleve(utilisateur.getId(), semestre)));
    }

    @PostMapping("/justificatifs")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Déposer un justificatif d'absence",
            description = "L'étudiant n'est pas transmis : il est déduit du jeton. Un compte "
                    + "ne peut donc pas justifier l'absence d'un autre.")
    public ResponseEntity<ApiResponse<JustificationAbsenceResponse>> deposerJustificatif(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam String motif,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateAbsence,
            @RequestParam(required = false) UUID seanceId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                justificationService.create(
                        profilService.etudiantDuCompte(utilisateur.getId()).getId(),
                        motif, dateAbsence, seanceId),
                "Justificatif transmis à la scolarité"));
    }

    // ------------------------------------------------------------------
    // Enrôlement biométrique en libre-service
    // ------------------------------------------------------------------

    @PostMapping("/enrolement")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Demander l'enrôlement de mon empreinte",
            description = "Ouvre un rendez-vous avec le lecteur et rend un code à six "
                    + "chiffres. L'étudiant retrouve ce code sur l'écran du capteur avant "
                    + "de poser le doigt : c'est ce qui garantit que la capture est bien "
                    + "portée à son dossier. Rappeler cette adresse ne crée pas un second "
                    + "code tant que le premier court.")
    public ResponseEntity<ApiResponse<DemandeEnrolementResponse>> demanderEnrolement(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                enrolementService.demander(utilisateur.getId()),
                "Présentez-vous devant un lecteur et vérifiez le code affiché"));
    }

    @GetMapping("/enrolement")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Où en est ma demande d'enrôlement",
            description = "Interrogée par l'application pendant que l'étudiant attend "
                    + "devant le capteur, pour basculer l'écran dès que l'empreinte est prise.")
    public ResponseEntity<ApiResponse<DemandeEnrolementResponse>> monEnrolement(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(
                enrolementService.maDemande(utilisateur.getId())));
    }

    @DeleteMapping("/enrolement")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Renoncer à ma demande d'enrôlement")
    public ResponseEntity<ApiResponse<DemandeEnrolementResponse>> annulerEnrolement(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(
                enrolementService.annuler(utilisateur.getId()), "Demande annulée"));
    }

    // ------------------------------------------------------------------
    // Espace enseignant
    // ------------------------------------------------------------------

    @GetMapping("/classes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes classes",
            description = "Classes dans lesquelles l'enseignant connecté intervient.")
    public ResponseEntity<ApiResponse<List<ClasseResponse>>> mesClasses(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(profilService.mesClasses(utilisateur.getId())));
    }

    @GetMapping("/classes/{classeId}/etudiants")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Effectif d'une de mes classes",
            description = "Identité et état d'enrôlement des inscrits. Ni email, ni téléphone, "
                    + "ni référence biométrique : faire cours n'en demande pas.")
    public ResponseEntity<ApiResponse<List<EffectifEtudiantResponse>>> mesEtudiants(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable Long classeId) {
        return ResponseEntity.ok(ApiResponse.success(
                profilService.mesEtudiants(utilisateur.getId(), classeId)));
    }

    @GetMapping("/seances")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes séances du jour")
    public ResponseEntity<ApiResponse<List<SeanceResponse>>> mesSeances(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                profilService.mesSeances(utilisateur.getId(), date == null ? LocalDate.now() : date)));
    }

    @GetMapping("/seances/{seanceId}/feuille")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Feuille de présence d'une de mes séances",
            description = "Relevés constatés par le lecteur. Les étudiants non enrôlés sont "
                    + "comptés à part : leur absence de relevé ne dit rien de leur assiduité.")
    public ResponseEntity<ApiResponse<FeuilleSeanceResponse>> feuille(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID seanceId) {
        return ResponseEntity.ok(
                ApiResponse.success(profilService.feuilleDeSeance(utilisateur.getId(), seanceId)));
    }

    @PostMapping("/seances/{seanceId}/signalements")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Signaler une anomalie sur une de mes séances",
            description = "L'enseignant témoigne, la scolarité arbitre. Le relevé n'est jamais "
                    + "modifié directement — c'est ce qui préserve sa valeur probante.")
    public ResponseEntity<ApiResponse<SignalementResponse>> signaler(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID seanceId,
            @Valid @RequestBody SignalementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                signalementService.signaler(utilisateur.getId(), seanceId, request),
                "Signalement transmis à la scolarité"));
    }

    @GetMapping("/matieres")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Matières disponibles pour la saisie de notes",
            description = "Référentiel des matières actives. La liste des enseignements d'un "
                    + "établissement n'est pas confidentielle, et l'enseignant en a besoin "
                    + "pour noter ; c'est le rattachement à la classe qui restreint ce qu'il "
                    + "peut faire, pas la liste des matières.")
    public ResponseEntity<ApiResponse<List<MatiereResponse>>> mesMatieres() {
        return ResponseEntity.ok(ApiResponse.success(
                academicService.matieres().stream().filter(MatiereResponse::isActive).toList()));
    }

    @GetMapping("/classes/{classeId}/notes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Notes d'une de mes classes",
            description = "Filtrables par matière et par période.")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> notesDeMaClasse(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable Long classeId,
            @RequestParam(required = false) Long matiereId,
            @RequestParam(required = false) PeriodeScolaire periode) {
        return ResponseEntity.ok(ApiResponse.success(noteService.notesDeMaClasse(
                utilisateur.getId(), classeId, matiereId, periode)));
    }

    @PostMapping("/notes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Saisir une note",
            description = "Refusé si l'enseignant n'intervient pas dans la classe de "
                    + "l'étudiant.")
    public ResponseEntity<ApiResponse<NoteResponse>> saisirNote(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                noteService.saisir(utilisateur.getId(), request), "Note enregistrée"));
    }

    @PutMapping("/notes/{noteId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Modifier une de mes notes",
            description = "Seul l'enseignant qui a saisi la note peut la corriger.")
    public ResponseEntity<ApiResponse<NoteResponse>> modifierNote(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.modifier(utilisateur.getId(), noteId, request),
                "Note mise à jour"));
    }

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Supprimer une de mes notes")
    public ResponseEntity<ApiResponse<Void>> supprimerNote(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID noteId) {
        noteService.supprimer(utilisateur.getId(), noteId);
        return ResponseEntity.ok(ApiResponse.success("Note supprimée"));
    }

    @GetMapping("/signalements")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes signalements et leur suivi")
    public ResponseEntity<ApiResponse<List<SignalementResponse>>> mesSignalements(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(
                ApiResponse.success(signalementService.mesSignalements(utilisateur.getId())));
    }
}
