package com.smartpresence.service.impl;

import com.smartpresence.constants.DecisionSemestre;
import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.TypeEvaluation;
import com.smartpresence.dto.response.ReleveSemestreResponse;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Matiere;
import com.smartpresence.entity.Note;
import com.smartpresence.entity.Promotion;
import com.smartpresence.entity.UniteEnseignement;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.mapper.NoteMapper;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.MatiereRepository;
import com.smartpresence.repository.NoteRepository;
import com.smartpresence.repository.UniteEnseignementRepository;
import com.smartpresence.service.ReleveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Relevé de notes au format LMD.
 */
@Service
@RequiredArgsConstructor
public class ReleveServiceImpl implements ReleveService {

    private static final int DECIMALES = 2;
    private static final RoundingMode ARRONDI = RoundingMode.HALF_UP;

    private final EtudiantRepository etudiantRepository;
    private final UniteEnseignementRepository uniteRepository;
    private final MatiereRepository matiereRepository;
    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;

    /** Part du contrôle continu dans la moyenne d'un ECUE. */
    @Value("${app.notes.poids-devoir:0.4}")
    private BigDecimal poidsDevoir;

    /** Part de l'examen final. */
    @Value("${app.notes.poids-examen:0.6}")
    private BigDecimal poidsExamen;

    /** Barre d'acquisition d'une UE et de validation d'un semestre. */
    @Value("${app.notes.moyenne-validation:10}")
    private BigDecimal barre;

    /** Une UE sous la barre est-elle rattrapée par la moyenne générale du semestre. */
    @Value("${app.notes.compensation:true}")
    private boolean compensation;

    @Override
    @Transactional(readOnly = true)
    public ReleveSemestreResponse monReleve(UUID utilisateurId, PeriodeScolaire semestre) {
        Etudiant etudiant = etudiantRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Ce compte n'est rattaché à aucune fiche étudiant", HttpStatus.FORBIDDEN));
        return construire(etudiant, semestre);
    }

    @Override
    @Transactional(readOnly = true)
    public ReleveSemestreResponse releveDe(UUID etudiantId, PeriodeScolaire semestre) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", etudiantId));
        return construire(etudiant, semestre);
    }

    // ------------------------------------------------------------------
    // Assemblage
    // ------------------------------------------------------------------

    private ReleveSemestreResponse construire(Etudiant etudiant, PeriodeScolaire demande) {
        Classe classe = etudiant.getClasse();
        Promotion promotion = classe == null ? null : classe.getPromotion();

        if (promotion == null) {
            // Un étudiant sans promotion n'a pas de maquette : le relevé est vide,
            // mais il doit exister — l'application ouvrirait sinon un écran d'erreur
            // là où il n'y a qu'une inscription incomplète.
            return releveVide(etudiant, classe, demande == null ? PeriodeScolaire.SEMESTRE_1 : demande,
                    List.of());
        }

        List<PeriodeScolaire> disponibles = semestresDeLaMaquette(promotion.getId());
        PeriodeScolaire semestre = semestreRetenu(demande, disponibles);

        List<UniteEnseignement> unites =
                uniteRepository.findByPromotionIdAndSemestreOrderByCodeAsc(promotion.getId(), semestre)
                        .stream()
                        .filter(UniteEnseignement::isActive)
                        .toList();

        if (unites.isEmpty()) {
            return releveVide(etudiant, classe, semestre, disponibles);
        }

        List<Long> uniteIds = unites.stream().map(UniteEnseignement::getId).toList();
        Map<Long, List<Matiere>> ecuesParUnite = matiereRepository
                .findByUniteEnseignementIdInOrderByCodeAsc(uniteIds).stream()
                .filter(Matiere::isActive)
                .collect(Collectors.groupingBy(
                        matiere -> matiere.getUniteEnseignement().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<Note>> notesParMatiere = noteRepository
                .findParEtudiant(etudiant.getId(), semestre).stream()
                .collect(Collectors.groupingBy(note -> note.getMatiere().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<ReleveSemestreResponse.UniteResponse> lignes = new ArrayList<>();
        for (UniteEnseignement unite : unites) {
            List<Matiere> ecues = ecuesParUnite.getOrDefault(unite.getId(), List.of());
            lignes.add(ligneUnite(unite, ecues, notesParMatiere));
        }

        BigDecimal generale = moyenneGenerale(lignes);
        boolean semestreALaBarre = generale != null && generale.compareTo(barre) >= 0;
        boolean compensationAppliquee = false;

        for (ReleveSemestreResponse.UniteResponse ligne : lignes) {
            boolean parSaMoyenne = ligne.getMoyenne() != null
                    && ligne.getMoyenne().compareTo(barre) >= 0;
            boolean parCompensation = !parSaMoyenne
                    && compensation
                    && semestreALaBarre
                    && ligne.getMoyenne() != null;

            ligne.setAcquise(parSaMoyenne || parCompensation);
            ligne.setAcquiseParCompensation(parCompensation);
            ligne.setCreditsAcquis(ligne.isAcquise() ? ligne.getCredits() : 0);
            compensationAppliquee |= parCompensation;
        }

        int nombreNotes = notesParMatiere.values().stream().mapToInt(List::size).sum();
        return ReleveSemestreResponse.builder()
                .etudiantNom(etudiant.getNom())
                .etudiantPrenom(etudiant.getPrenom())
                .etudiantMatricule(etudiant.getMatricule())
                .classeCode(classe.getCode())
                .classeLibelle(classe.getLibelle())
                .promotionLibelle(promotion.getLibelle())
                .semestre(semestre)
                .semestreLibelle(semestre.libelleCourt())
                .semestresDisponibles(disponibles)
                .moyenneGenerale(generale)
                .creditsAcquis(lignes.stream()
                        .mapToInt(ReleveSemestreResponse.UniteResponse::getCreditsAcquis).sum())
                .creditsRequis(unites.stream().mapToInt(UniteEnseignement::getCredits).sum())
                .decision(decision(generale, nombreNotes))
                .compensationAppliquee(compensationAppliquee)
                .nombreNotes(nombreNotes)
                .unites(lignes)
                .build();
    }

    private ReleveSemestreResponse.UniteResponse ligneUnite(
            UniteEnseignement unite, List<Matiere> ecues, Map<Long, List<Note>> notesParMatiere) {

        List<ReleveSemestreResponse.EcueResponse> lignes = ecues.stream()
                .map(ecue -> ligneEcue(ecue, notesParMatiere.getOrDefault(ecue.getId(), List.of())))
                .sorted(Comparator.comparing(ReleveSemestreResponse.EcueResponse::getCode))
                .toList();

        return ReleveSemestreResponse.UniteResponse.builder()
                .uniteId(unite.getId())
                .code(unite.getCode())
                .libelle(unite.getLibelle())
                .credits(unite.getCredits())
                .moyenne(moyenneUnite(lignes))
                .ecues(lignes)
                .build();
    }

    private ReleveSemestreResponse.EcueResponse ligneEcue(Matiere ecue, List<Note> notes) {
        BigDecimal devoir = moyennePonderee(notes.stream()
                .filter(note -> note.getType() != TypeEvaluation.EXAMEN)
                .toList());
        BigDecimal examen = moyennePonderee(notes.stream()
                .filter(note -> note.getType() == TypeEvaluation.EXAMEN)
                .toList());

        return ReleveSemestreResponse.EcueResponse.builder()
                .matiereId(ecue.getId())
                .code(ecue.getCode())
                .libelle(ecue.getLibelle())
                .credits(creditsDe(ecue))
                .noteDevoir(devoir)
                .noteExamen(examen)
                .moyenne(combiner(devoir, examen))
                .notes(noteMapper.toResponseList(notes))
                .build();
    }

    /**
     * Devoir et examen combinés selon la pondération du règlement.
     *
     * <p>Lorsqu'une seule des deux composantes existe, elle vaut à elle seule la
     * moyenne. Appliquer malgré tout la pondération reviendrait à afficher 7,2 pour
     * un étudiant qui a 12 de devoir et dont l'examen n'a pas encore eu lieu.</p>
     */
    private BigDecimal combiner(BigDecimal devoir, BigDecimal examen) {
        if (devoir == null && examen == null) {
            return null;
        }
        if (devoir == null) {
            return examen;
        }
        if (examen == null) {
            return devoir;
        }
        return devoir.multiply(poidsDevoir)
                .add(examen.multiply(poidsExamen))
                .setScale(DECIMALES, ARRONDI);
    }

    /** Moyenne d'un groupe de notes, pondérée par leurs coefficients. */
    private BigDecimal moyennePonderee(List<Note> notes) {
        int total = notes.stream().mapToInt(Note::getCoefficient).sum();
        if (total == 0) {
            return null;
        }
        return notes.stream()
                .map(note -> note.getValeur().multiply(BigDecimal.valueOf(note.getCoefficient())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(total), DECIMALES, ARRONDI);
    }

    /**
     * Moyenne d'une UE : celles de ses ECUE, pondérées par leurs crédits.
     *
     * <p>Un ECUE sans note est écarté du calcul plutôt que compté zéro : sinon la
     * moyenne d'une UE s'effondrerait au seul motif qu'un enseignant n'a pas encore
     * rendu ses notes.</p>
     */
    private BigDecimal moyenneUnite(List<ReleveSemestreResponse.EcueResponse> ecues) {
        List<ReleveSemestreResponse.EcueResponse> notes = ecues.stream()
                .filter(ecue -> ecue.getMoyenne() != null)
                .toList();
        int total = notes.stream().mapToInt(ReleveSemestreResponse.EcueResponse::getCredits).sum();
        if (total == 0) {
            return null;
        }
        return notes.stream()
                .map(ecue -> ecue.getMoyenne().multiply(BigDecimal.valueOf(ecue.getCredits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(total), DECIMALES, ARRONDI);
    }

    /** Moyenne du semestre : celles des UE, pondérées par leurs crédits. */
    private BigDecimal moyenneGenerale(List<ReleveSemestreResponse.UniteResponse> unites) {
        List<ReleveSemestreResponse.UniteResponse> notees = unites.stream()
                .filter(unite -> unite.getMoyenne() != null)
                .toList();
        int total = notees.stream()
                .mapToInt(ReleveSemestreResponse.UniteResponse::getCredits).sum();
        if (total == 0) {
            return null;
        }
        return notees.stream()
                .map(unite -> unite.getMoyenne().multiply(BigDecimal.valueOf(unite.getCredits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(total), DECIMALES, ARRONDI);
    }

    private DecisionSemestre decision(BigDecimal generale, int nombreNotes) {
        if (nombreNotes == 0 || generale == null) {
            return DecisionSemestre.EN_ATTENTE;
        }
        return generale.compareTo(barre) >= 0
                ? DecisionSemestre.VALIDE
                : DecisionSemestre.NON_VALIDE;
    }

    /** Crédits d'un ECUE, ou 1 à défaut — pour que la pondération reste définie. */
    private int creditsDe(Matiere ecue) {
        return ecue.getCredits() == null || ecue.getCredits() <= 0 ? 1 : ecue.getCredits();
    }

    private List<PeriodeScolaire> semestresDeLaMaquette(Long promotionId) {
        return uniteRepository.findByPromotionIdOrderBySemestreAscCodeAsc(promotionId).stream()
                .filter(UniteEnseignement::isActive)
                .map(UniteEnseignement::getSemestre)
                .distinct()
                .sorted(Comparator.comparingInt(PeriodeScolaire::rang))
                .toList();
    }

    /**
     * Semestre à afficher : celui demandé, sinon le premier de la maquette.
     *
     * <p>Ouvrir sur un semestre vide alors que la promotion n'en a qu'un serait un
     * écran blanc pour rien.</p>
     */
    private PeriodeScolaire semestreRetenu(PeriodeScolaire demande, List<PeriodeScolaire> disponibles) {
        if (demande != null) {
            return demande;
        }
        return disponibles.isEmpty() ? PeriodeScolaire.SEMESTRE_1 : disponibles.get(0);
    }

    private ReleveSemestreResponse releveVide(Etudiant etudiant, Classe classe,
                                              PeriodeScolaire semestre,
                                              List<PeriodeScolaire> disponibles) {
        Promotion promotion = classe == null ? null : classe.getPromotion();
        return ReleveSemestreResponse.builder()
                .etudiantNom(etudiant.getNom())
                .etudiantPrenom(etudiant.getPrenom())
                .etudiantMatricule(etudiant.getMatricule())
                .classeCode(classe == null ? null : classe.getCode())
                .classeLibelle(classe == null ? null : classe.getLibelle())
                .promotionLibelle(promotion == null ? null : promotion.getLibelle())
                .semestre(semestre)
                .semestreLibelle(semestre.libelleCourt())
                .semestresDisponibles(disponibles)
                .moyenneGenerale(null)
                .creditsAcquis(0)
                .creditsRequis(0)
                .decision(DecisionSemestre.EN_ATTENTE)
                .compensationAppliquee(false)
                .nombreNotes(0)
                .unites(List.of())
                .build();
    }
}
