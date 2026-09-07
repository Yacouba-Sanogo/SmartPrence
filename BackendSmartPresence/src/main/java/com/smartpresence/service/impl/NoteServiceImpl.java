package com.smartpresence.service.impl;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TypeEvaluation;
import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.dto.request.NoteRequest;
import com.smartpresence.dto.response.BulletinResponse;
import com.smartpresence.dto.response.NoteResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Matiere;
import com.smartpresence.entity.Note;
import com.smartpresence.entity.Personnel;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.MatiereRepository;
import com.smartpresence.repository.NoteRepository;
import com.smartpresence.repository.PersonnelRepository;
import com.smartpresence.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notes et bulletins.
 *
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    /**
     * Deux décimales, arrondi au plus proche.
     *
     * <p>{@code HALF_UP} et non {@code HALF_EVEN} : c'est la règle qu'appliquent les
     * établissements, et un bulletin qui arrondirait autrement qu'à la main serait
     * contesté.</p>
     */
    private static final int DECIMALES = 2;
    private static final RoundingMode ARRONDI = RoundingMode.HALF_UP;

    private final NoteRepository noteRepository;
    private final EtudiantRepository etudiantRepository;
    private final MatiereRepository matiereRepository;
    private final PersonnelRepository personnelRepository;
    private final ClasseRepository classeRepository;

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public BulletinResponse monBulletin(UUID utilisateurId, PeriodeScolaire periode) {
        Etudiant etudiant = etudiantRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche étudiant", HttpStatus.FORBIDDEN));
        return construireBulletin(etudiant, periode);
    }

    @Override
    @Transactional(readOnly = true)
    public BulletinResponse bulletinDe(UUID etudiantId, PeriodeScolaire periode) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", etudiantId));
        return construireBulletin(etudiant, periode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteResponse> notesDeMaClasse(UUID utilisateurId, Long classeId,
                                              Long matiereId, PeriodeScolaire periode) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        assertIntervientDans(enseignant, classeId);
        return noteRepository.findParClasse(classeId, matiereId, periode).stream()
                .map(this::versReponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Saisie
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public NoteResponse saisir(UUID utilisateurId, NoteRequest request) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        Etudiant etudiant = etudiantRepository.findById(request.getEtudiantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Étudiant", "id", request.getEtudiantId()));

        if (etudiant.getClasse() == null) {
            throw new BusinessException(
                    "Cet étudiant n'est affecté à aucune classe : il ne peut pas être noté");
        }
        assertIntervientDans(enseignant, etudiant.getClasse().getId());

        Note note = new Note();
        note.setEtudiant(etudiant);
        note.setEnseignant(enseignant);
        appliquer(note, request);

        Note enregistree = noteRepository.save(note);
        log.info("Note saisie — étudiant={}, matière={}, valeur={}, enseignant={}",
                etudiant.getMatricule(), enregistree.getMatiere().getCode(),
                enregistree.getValeur(), enseignant.getMatricule());
        return versReponse(enregistree);
    }

    @Override
    @Transactional
    public NoteResponse modifier(UUID utilisateurId, UUID noteId, NoteRequest request) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        Note note = noteAvecContexte(noteId);
        assertAuteur(enseignant, note);

        // Changer d'étudiant reviendrait à créer une autre note : on l'interdit plutôt
        // que de laisser une correction déplacer silencieusement un résultat.
        if (!note.getEtudiant().getId().equals(request.getEtudiantId())) {
            throw new BusinessException(
                    "Une note ne peut pas changer d'étudiant. Supprimez-la et saisissez-en "
                            + "une nouvelle.", HttpStatus.CONFLICT);
        }

        appliquer(note, request);
        return versReponse(noteRepository.save(note));
    }

    @Override
    @Transactional
    public void supprimer(UUID utilisateurId, UUID noteId) {
        Personnel enseignant = enseignantDuCompte(utilisateurId);
        Note note = noteAvecContexte(noteId);
        assertAuteur(enseignant, note);
        noteRepository.delete(note);
        log.info("Note supprimée — id={}, enseignant={}", noteId, enseignant.getMatricule());
    }

    // ------------------------------------------------------------------

    /**
     * Assemble le bulletin, matière par matière.
     *
     * <p>Les notes sont regroupées par matière, chaque groupe donne une moyenne
     * pondérée par les coefficients des notes, puis la moyenne générale pondère ces
     * moyennes par le poids de chaque matière.</p>
     */
    private BulletinResponse construireBulletin(Etudiant etudiant, PeriodeScolaire periode) {
        List<Note> notes = noteRepository.findParEtudiant(etudiant.getId(), periode);

        // LinkedHashMap : l'ordre des matières reste celui du tri de la requête,
        // sans quoi le bulletin changerait d'ordre d'un affichage à l'autre.
        Map<Matiere, List<Note>> parMatiere = notes.stream()
                .collect(Collectors.groupingBy(Note::getMatiere, LinkedHashMap::new,
                        Collectors.toList()));

        List<BulletinResponse.LigneMatiereResponse> lignes = parMatiere.entrySet().stream()
                .map(entree -> ligneMatiere(entree.getKey(), entree.getValue()))
                .sorted(Comparator.comparing(BulletinResponse.LigneMatiereResponse::getMatiereCode))
                .toList();

        Classe classe = etudiant.getClasse();
        return BulletinResponse.builder()
                .etudiantId(etudiant.getId())
                .etudiantNom(etudiant.getNom())
                .etudiantPrenom(etudiant.getPrenom())
                .etudiantMatricule(etudiant.getMatricule())
                .classeCode(classe == null ? null : classe.getCode())
                .classeLibelle(classe == null ? null : classe.getLibelle())
                .periode(periode)
                .moyenneGenerale(moyenneGenerale(lignes))
                .nombreNotes(notes.size())
                .matieres(lignes)
                .build();
    }

    private BulletinResponse.LigneMatiereResponse ligneMatiere(Matiere matiere, List<Note> notes) {
        return BulletinResponse.LigneMatiereResponse.builder()
                .matiereId(matiere.getId())
                .matiereCode(matiere.getCode())
                .matiereLibelle(matiere.getLibelle())
                .coefficient(poidsDe(matiere))
                .moyenne(moyennePonderee(notes))
                .notes(notes.stream().map(this::versReponse).toList())
                .build();
    }

    /** Moyenne d'une matière : somme des notes pondérées, divisée par la somme des coefficients. */
    private BigDecimal moyennePonderee(List<Note> notes) {
        int totalCoefficients = notes.stream().mapToInt(Note::getCoefficient).sum();
        if (totalCoefficients == 0) {
            return null;
        }
        BigDecimal cumul = notes.stream()
                .map(Note::contributionPonderee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cumul.divide(BigDecimal.valueOf(totalCoefficients), DECIMALES, ARRONDI);
    }

    /**
     * Moyenne générale : moyennes de matières pondérées par le poids de chaque matière.
     *
     * <p>Une matière sans note est écartée du calcul. L'inclure avec une moyenne nulle
     * ferait chuter la générale d'un étudiant simplement parce qu'un enseignant n'a pas
     * encore saisi ses notes.</p>
     */
    private BigDecimal moyenneGenerale(List<BulletinResponse.LigneMatiereResponse> lignes) {
        List<BulletinResponse.LigneMatiereResponse> notees = lignes.stream()
                .filter(ligne -> ligne.getMoyenne() != null)
                .toList();
        if (notees.isEmpty()) {
            return null;
        }

        int totalPoids = notees.stream()
                .mapToInt(BulletinResponse.LigneMatiereResponse::getCoefficient)
                .sum();
        if (totalPoids == 0) {
            return null;
        }

        BigDecimal cumul = notees.stream()
                .map(ligne -> ligne.getMoyenne()
                        .multiply(BigDecimal.valueOf(ligne.getCoefficient())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cumul.divide(BigDecimal.valueOf(totalPoids), DECIMALES, ARRONDI);
    }

    /** Poids d'une matière : ses crédits, ou 1 quand ils ne sont pas renseignés. */
    private int poidsDe(Matiere matiere) {
        Integer credits = matiere.getCredits();
        return credits == null || credits <= 0 ? 1 : credits;
    }

    private void appliquer(Note note, NoteRequest request) {
        Matiere matiere = matiereRepository.findById(request.getMatiereId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matière", "id", request.getMatiereId()));
        if (!matiere.isActive()) {
            throw new BusinessException("Cette matière est désactivée");
        }

        note.setMatiere(matiere);
        note.setValeur(request.getValeur());
        note.setCoefficient(request.getCoefficient() == null ? 1 : request.getCoefficient());
        note.setType(request.getType() == null ? TypeEvaluation.DEVOIR : request.getType());
        note.setPeriode(request.getPeriode() == null
                ? PeriodeScolaire.SEMESTRE_1 : request.getPeriode());
        note.setLibelle(request.getLibelle().trim());
        note.setDateEvaluation(request.getDateEvaluation());
        note.setAppreciation(request.getAppreciation() == null
                ? null : request.getAppreciation().trim());
    }

    /**
     * Vérifie que l'enseignant intervient dans la classe.
     *
     * <p>Le rattachement enseignant → classe est la seule chose qui autorise à noter.
     * Sans ce contrôle, tout enseignant pourrait modifier les résultats de n'importe
     * quel étudiant de l'établissement.</p>
     */
    private void assertIntervientDans(Personnel enseignant, Long classeId) {
        boolean rattache = classeRepository.findParEnseignant(enseignant.getId()).stream()
                .anyMatch(classe -> classe.getId().equals(classeId));
        if (!rattache) {
            throw new BusinessException(
                    "Vous n'intervenez pas dans cette classe", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Vérifie que l'enseignant est l'auteur de la note.
     *
     * <p>Intervenir dans la classe ne suffit pas : deux enseignants peuvent y enseigner
     * des matières différentes, et l'un n'a pas à corriger les notes de l'autre.</p>
     */
    private void assertAuteur(Personnel enseignant, Note note) {
        if (!note.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException(
                    "Cette note a été saisie par " + note.getEnseignant().getPrenom() + " "
                            + note.getEnseignant().getNom() + " : elle ne vous appartient pas",
                    HttpStatus.FORBIDDEN);
        }
    }

    private Note noteAvecContexte(UUID noteId) {
        return noteRepository.findAvecContexte(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", noteId));
    }

    private Personnel enseignantDuCompte(UUID utilisateurId) {
        Personnel personnel = personnelRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche d'agent", HttpStatus.FORBIDDEN));
        if (personnel.getType() != TypePersonnel.ENSEIGNANT) {
            throw new BusinessException(
                    "Ce compte n'est pas rattaché à un enseignant", HttpStatus.FORBIDDEN);
        }
        return personnel;
    }

    private NoteResponse versReponse(Note note) {
        Etudiant etudiant = note.getEtudiant();
        Personnel enseignant = note.getEnseignant();
        return NoteResponse.builder()
                .id(note.getId())
                .etudiantId(etudiant.getId())
                .etudiantNom(etudiant.getNom())
                .etudiantPrenom(etudiant.getPrenom())
                .etudiantMatricule(etudiant.getMatricule())
                .matiereId(note.getMatiere().getId())
                .matiereCode(note.getMatiere().getCode())
                .matiereLibelle(note.getMatiere().getLibelle())
                .enseignantId(enseignant.getId())
                .enseignantNom(enseignant.getPrenom() + " " + enseignant.getNom())
                .valeur(note.getValeur())
                .coefficient(note.getCoefficient())
                .type(note.getType())
                .periode(note.getPeriode())
                .libelle(note.getLibelle())
                .dateEvaluation(note.getDateEvaluation())
                .appreciation(note.getAppreciation())
                .build();
    }
}
