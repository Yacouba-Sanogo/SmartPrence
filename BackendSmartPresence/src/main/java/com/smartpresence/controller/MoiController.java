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
import com.smartpresence.dto.request.ParcoursRequests;
import com.smartpresence.dto.response.ChoixOptionResponse;
import com.smartpresence.service.ParcoursService;
import com.smartpresence.service.EnseignementService;
import com.smartpresence.service.EvaluationService;
import com.smartpresence.dto.request.EnseignementRequests;
import com.smartpresence.dto.response.EnseignementResponses;
import com.smartpresence.dto.request.GestionRequests;
import com.smartpresence.dto.response.GestionResponses;
import com.smartpresence.dto.response.JourneePersonnelResponse;
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
    private final ParcoursService parcoursService;
    private final EnseignementService enseignementService;
    private final EvaluationService evaluationService;
    private final com.smartpresence.service.CongeService congeService;
    private final com.smartpresence.service.ReclamationService reclamationService;
    private final com.smartpresence.service.SupportCoursService supportService;

    @GetMapping("/option")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mon option et celles que ma filière propose")
    public ResponseEntity<ApiResponse<ChoixOptionResponse>> monOption(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(parcoursService.options(
                profilService.etudiantDuCompte(utilisateur.getId()).getId())));
    }

    @PutMapping("/option")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Choisir mon option",
            description = "Elle décide de la classe que je rejoindrai en année supérieure quand "
                    + "ma filière se divise (ex. Génie Civil : BTP ou Hydraulique).")
    public ResponseEntity<ApiResponse<ChoixOptionResponse>> choisirMonOption(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @Valid @RequestBody ParcoursRequests.ChoixOption requete) {
        return ResponseEntity.ok(ApiResponse.success(parcoursService.choisirOption(
                profilService.etudiantDuCompte(utilisateur.getId()).getId(), requete.optionId()),
                "Option enregistrée"));
    }

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

    @PostMapping("/notes/transmettre")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Transmettre des notes à l'administration",
            description = "Les notes passent de brouillon à transmise : elles ne sont plus "
                    + "modifiables et attendent la décision de l'administration, qui seule "
                    + "les publie dans les bulletins.")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> transmettreNotes(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @Valid @RequestBody EnseignementRequests.LotNotes lot) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.transmettre(utilisateur.getId(), lot.noteIds()),
                "Notes transmises à l'administration"));
    }

    // ------------------------------------------------------------------
    // Présence de l'enseignant et séances effectuées
    // ------------------------------------------------------------------

    @GetMapping("/pointages")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes pointages",
            description = "Mes arrivées et départs relevés par le lecteur. Sans paramètre, "
                    + "les trente derniers jours.")
    public ResponseEntity<ApiResponse<List<JourneePersonnelResponse>>> mesPointages(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate dernier = fin == null ? LocalDate.now() : fin;
        LocalDate premier = debut == null ? dernier.minusDays(29) : debut;
        if (dernier.isBefore(premier)) {
            throw new BusinessException("La date de fin précède la date de début");
        }
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.mesPointages(utilisateur.getId(), premier, dernier)));
    }

    @PostMapping("/seances/{seanceId}/declaration")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Déclarer une séance effectuée",
            description = "Refusée si la séance est annulée ou pas encore commencée. "
                    + "Idempotente : la rappeler ne change pas la date de déclaration.")
    public ResponseEntity<ApiResponse<SeanceResponse>> declarerSeance(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID seanceId) {
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.declarerSeance(utilisateur.getId(), seanceId),
                "Séance déclarée effectuée"));
    }

    @GetMapping("/bilan")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mon bilan d'enseignement",
            description = "Séances prévues, effectuées, annulées et restant à déclarer, heures "
                    + "enseignées par matière, et ponctualité. Sans paramètre, le mois en cours.")
    public ResponseEntity<ApiResponse<EnseignementResponses.Bilan>> bilan(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate premier = debut == null ? LocalDate.now().withDayOfMonth(1) : debut;
        LocalDate dernier = fin == null ? LocalDate.now() : fin;
        if (dernier.isBefore(premier)) {
            throw new BusinessException("La date de fin précède la date de début");
        }
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.bilan(utilisateur.getId(), premier, dernier)));
    }

    // ------------------------------------------------------------------
    // Cahier de textes et syllabus
    // ------------------------------------------------------------------

    @GetMapping("/seances/{seanceId}/cahier")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Page du cahier de textes d'une séance",
            description = "Vide (data nul) tant que l'enseignant n'a rien écrit.")
    public ResponseEntity<ApiResponse<EnseignementResponses.PageCahier>> cahierDeSeance(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID seanceId) {
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.cahierDeSeance(utilisateur.getId(), seanceId)));
    }

    @PutMapping("/seances/{seanceId}/cahier")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Écrire ou corriger le cahier de textes d'une séance",
            description = "Peut lier la page à un chapitre et le marquer terminé.")
    public ResponseEntity<ApiResponse<EnseignementResponses.PageCahier>> ecrireCahier(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID seanceId,
            @Valid @RequestBody EnseignementRequests.Cahier requete) {
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.ecrireCahier(utilisateur.getId(), seanceId, requete),
                "Cahier de textes enregistré"));
    }

    @GetMapping("/classes/{classeId}/cahier")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Cahier de textes d'une de mes classes")
    public ResponseEntity<ApiResponse<List<EnseignementResponses.PageCahier>>> cahierDeClasse(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable Long classeId,
            @RequestParam(required = false) Long matiereId) {
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.cahierDeClasse(utilisateur.getId(), classeId, matiereId)));
    }

    @GetMapping("/classes/{classeId}/chapitres")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mon syllabus dans une classe",
            description = "Chapitres par matière, avec l'avancement.")
    public ResponseEntity<ApiResponse<List<EnseignementResponses.Syllabus>>> syllabus(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable Long classeId,
            @RequestParam(required = false) Long matiereId) {
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.syllabus(utilisateur.getId(), classeId, matiereId)));
    }

    @PostMapping("/chapitres")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Ajouter un chapitre à mon programme")
    public ResponseEntity<ApiResponse<EnseignementResponses.Chapitre>> ajouterChapitre(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @Valid @RequestBody EnseignementRequests.NouveauChapitre requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                enseignementService.ajouterChapitre(utilisateur.getId(), requete), "Chapitre ajouté"));
    }

    @PutMapping("/chapitres/{chapitreId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Modifier un chapitre, ou le marquer terminé")
    public ResponseEntity<ApiResponse<EnseignementResponses.Chapitre>> modifierChapitre(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID chapitreId,
            @Valid @RequestBody EnseignementRequests.ModificationChapitre requete) {
        return ResponseEntity.ok(ApiResponse.success(
                enseignementService.modifierChapitre(utilisateur.getId(), chapitreId, requete)));
    }

    @DeleteMapping("/chapitres/{chapitreId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Supprimer un chapitre")
    public ResponseEntity<ApiResponse<Void>> supprimerChapitre(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID chapitreId) {
        enseignementService.supprimerChapitre(utilisateur.getId(), chapitreId);
        return ResponseEntity.ok(ApiResponse.success("Chapitre supprimé"));
    }

    // ------------------------------------------------------------------
    // Évaluations
    // ------------------------------------------------------------------

    @GetMapping("/classes/{classeId}/evaluations")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes évaluations dans une classe")
    public ResponseEntity<ApiResponse<List<EnseignementResponses.Evaluation>>> evaluations(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable Long classeId,
            @RequestParam(required = false) Long matiereId) {
        return ResponseEntity.ok(ApiResponse.success(
                evaluationService.evaluationsDeClasse(utilisateur.getId(), classeId, matiereId)));
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Planifier une évaluation",
            description = "Devoir, interrogation, examen… avec son barème, son coefficient et son sujet.")
    public ResponseEntity<ApiResponse<EnseignementResponses.Evaluation>> planifier(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @Valid @RequestBody EnseignementRequests.Evaluation requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                evaluationService.planifier(utilisateur.getId(), requete), "Évaluation planifiée"));
    }

    @PutMapping("/evaluations/{evaluationId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Modifier une évaluation",
            description = "Le barème ne change plus une fois des notes saisies.")
    public ResponseEntity<ApiResponse<EnseignementResponses.Evaluation>> modifierEvaluation(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID evaluationId,
            @Valid @RequestBody EnseignementRequests.Evaluation requete) {
        return ResponseEntity.ok(ApiResponse.success(
                evaluationService.modifier(utilisateur.getId(), evaluationId, requete)));
    }

    @DeleteMapping("/evaluations/{evaluationId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Supprimer une évaluation sans notes")
    public ResponseEntity<ApiResponse<Void>> supprimerEvaluation(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID evaluationId) {
        evaluationService.supprimer(utilisateur.getId(), evaluationId);
        return ResponseEntity.ok(ApiResponse.success("Évaluation supprimée"));
    }

    @GetMapping("/evaluations/{evaluationId}/resultats")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Résultats d'une évaluation",
            description = "Toute la classe : note sur le barème, état dans le circuit de "
                    + "validation, ou absence.")
    public ResponseEntity<ApiResponse<EnseignementResponses.ResultatsEvaluation>> resultats(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID evaluationId) {
        return ResponseEntity.ok(ApiResponse.success(
                evaluationService.resultats(utilisateur.getId(), evaluationId)));
    }

    @PutMapping("/evaluations/{evaluationId}/resultats")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Saisir les résultats d'une évaluation",
            description = "Notes sur le barème de l'épreuve, converties sur 20 par le serveur, "
                    + "ou absences. Refusé pour une note déjà transmise.")
    public ResponseEntity<ApiResponse<EnseignementResponses.ResultatsEvaluation>> saisirResultats(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID evaluationId,
            @Valid @RequestBody EnseignementRequests.Resultats requete) {
        return ResponseEntity.ok(ApiResponse.success(
                evaluationService.enregistrerResultats(utilisateur.getId(), evaluationId, requete),
                "Résultats enregistrés"));
    }

    @PostMapping("/evaluations/{evaluationId}/transmission")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Transmettre les notes d'une évaluation à l'administration")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> transmettreEvaluation(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @PathVariable UUID evaluationId) {
        return ResponseEntity.ok(ApiResponse.success(
                evaluationService.transmettre(utilisateur.getId(), evaluationId),
                "Notes transmises à l'administration"));
    }

    // ------------------------------------------------------------------
    // Demandes de congé
    // ------------------------------------------------------------------

    @GetMapping("/conges")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes demandes de congé et d'absence")
    public ResponseEntity<ApiResponse<List<GestionResponses.Conge>>> mesConges(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(congeService.mesDemandes(utilisateur.getId())));
    }

    @PostMapping("/conges")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Demander un congé ou déclarer une absence",
            description = "Refusée si elle recouvre une demande en attente ou acceptée. "
                    + "La réponse indique combien de séances sont touchées.")
    public ResponseEntity<ApiResponse<GestionResponses.Conge>> demanderConge(
            @AuthenticationPrincipal CustomUserDetails utilisateur,
            @Valid @RequestBody GestionRequests.Conge requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                congeService.demander(utilisateur.getId(), requete), "Demande transmise à l'administration"));
    }

    @PostMapping("/conges/{demandeId}/annulation")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Retirer une demande en attente")
    public ResponseEntity<ApiResponse<GestionResponses.Conge>> annulerConge(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable UUID demandeId) {
        return ResponseEntity.ok(ApiResponse.success(
                congeService.annuler(utilisateur.getId(), demandeId), "Demande retirée"));
    }

    // ------------------------------------------------------------------
    // Réclamations sur les notes
    // ------------------------------------------------------------------

    @PostMapping("/notes/{noteId}/reclamation")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Contester une de mes notes",
            description = "Réservée aux notes publiées de l'étudiant connecté. Une seule "
                    + "contestation à la fois par note.")
    public ResponseEntity<ApiResponse<GestionResponses.Reclamation>> contester(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable UUID noteId,
            @Valid @RequestBody GestionRequests.Contestation requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                reclamationService.contester(utilisateur.getId(), noteId, requete),
                "Contestation transmise à l'enseignant"));
    }

    @GetMapping("/reclamations")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Mes contestations et leur suite")
    public ResponseEntity<ApiResponse<List<GestionResponses.Reclamation>>> mesContestations(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(reclamationService.mesContestations(utilisateur.getId())));
    }

    @GetMapping("/reclamations/recues")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Les contestations adressées à l'enseignant",
            description = "Les ouvertes d'abord, puis les plus récentes.")
    public ResponseEntity<ApiResponse<List<GestionResponses.Reclamation>>> reclamationsRecues(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(reclamationService.recues(utilisateur.getId())));
    }

    @PostMapping("/reclamations/{reclamationId}/reponse")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Répondre à une contestation",
            description = "L'acceptation peut corriger la note ; l'ancienne valeur est conservée.")
    public ResponseEntity<ApiResponse<GestionResponses.Reclamation>> repondreReclamation(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable UUID reclamationId,
            @Valid @RequestBody GestionRequests.ReponseReclamation reponse) {
        return ResponseEntity.ok(ApiResponse.success(
                reclamationService.repondre(utilisateur.getId(), reclamationId, reponse), "Réponse envoyée"));
    }

    // ------------------------------------------------------------------
    // Supports de cours
    // ------------------------------------------------------------------

    @PostMapping(value = "/classes/{classeId}/supports", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Publier un support de cours",
            description = "PDF, présentation, document, tableur ou archive jusqu'à 10 Mo. Les "
                    + "étudiants de la classe sont prévenus.")
    public ResponseEntity<ApiResponse<GestionResponses.Support>> publierSupport(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long classeId,
            @RequestParam Long matiereId, @RequestParam String titre,
            @RequestParam(required = false) String description,
            @RequestParam("fichier") org.springframework.web.multipart.MultipartFile fichier) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                supportService.publier(utilisateur.getId(), classeId, matiereId, titre, description, fichier),
                "Support publié"));
    }

    @GetMapping("/classes/{classeId}/supports")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Les supports que j'ai publiés dans une classe")
    public ResponseEntity<ApiResponse<List<GestionResponses.Support>>> mesSupports(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long classeId) {
        return ResponseEntity.ok(ApiResponse.success(supportService.mesSupports(utilisateur.getId(), classeId)));
    }

    @GetMapping("/supports")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Operation(summary = "Les supports de cours de ma classe")
    public ResponseEntity<ApiResponse<List<GestionResponses.Support>>> supportsDeMaClasse(
            @AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.ok(ApiResponse.success(supportService.supportsDeMaClasse(utilisateur.getId())));
    }

    @DeleteMapping("/supports/{supportId}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Retirer un de mes supports")
    public ResponseEntity<ApiResponse<Void>> supprimerSupport(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable UUID supportId) {
        supportService.supprimer(utilisateur.getId(), supportId);
        return ResponseEntity.ok(ApiResponse.success("Support retiré"));
    }

    @GetMapping("/supports/{supportId}/fichier")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ETUDIANT')")
    @Operation(summary = "Télécharger un support",
            description = "L'auteur, ou un étudiant de la classe concernée.")
    public ResponseEntity<byte[]> telechargerSupport(
            @AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable UUID supportId) {
        var fichier = supportService.telecharger(utilisateur.getId(), supportId);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(fichier.typeMime()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(fichier.nom(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentLength(fichier.octets().length)
                .body(fichier.octets());
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
