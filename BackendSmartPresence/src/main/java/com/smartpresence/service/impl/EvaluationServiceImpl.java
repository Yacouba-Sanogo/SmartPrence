package com.smartpresence.service.impl;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.StatutNote;
import com.smartpresence.constants.TypeEvaluation;
import com.smartpresence.dto.request.EnseignementRequests;
import com.smartpresence.dto.response.EnseignementResponses;
import com.smartpresence.dto.response.NoteResponse;
import com.smartpresence.entity.AbsenceEvaluation;
import com.smartpresence.entity.Classe;
import com.smartpresence.entity.Etudiant;
import com.smartpresence.entity.Evaluation;
import com.smartpresence.entity.Matiere;
import com.smartpresence.entity.Note;
import com.smartpresence.entity.Personnel;
import com.smartpresence.exception.BusinessException;
import com.smartpresence.exception.ResourceNotFoundException;
import com.smartpresence.repository.AbsenceEvaluationRepository;
import com.smartpresence.repository.ClasseRepository;
import com.smartpresence.repository.EtudiantRepository;
import com.smartpresence.repository.EvaluationRepository;
import com.smartpresence.repository.MatiereRepository;
import com.smartpresence.repository.NoteRepository;
import com.smartpresence.service.EvaluationService;
import com.smartpresence.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final BigDecimal VINGT = BigDecimal.valueOf(20);

    private final EnseignantDuCompte enseignants;
    private final EvaluationRepository evaluationRepository;
    private final AbsenceEvaluationRepository absenceRepository;
    private final NoteRepository noteRepository;
    private final EtudiantRepository etudiantRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final NoteService noteService;

    @Override
    @Transactional(readOnly = true)
    public List<EnseignementResponses.Evaluation> evaluationsDeClasse(UUID utilisateurId, Long classeId,
                                                                      Long matiereId) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        enseignants.exigerClasse(enseignant, classeId);
        int effectif = effectif(classeId).size();
        return evaluationRepository.findParClasse(classeId, enseignant.getId(), matiereId).stream()
                .map(e -> versReponse(e, effectif))
                .toList();
    }

    @Override
    @Transactional
    public EnseignementResponses.Evaluation planifier(UUID utilisateurId, EnseignementRequests.Evaluation requete) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        if (requete.classeId() == null) {
            throw new BusinessException("La classe est obligatoire");
        }
        enseignants.exigerClasse(enseignant, requete.classeId());
        Classe classe = classeRepository.findById(requete.classeId())
                .orElseThrow(() -> new ResourceNotFoundException("Classe", "id", requete.classeId()));

        Evaluation evaluation = new Evaluation();
        evaluation.setClasse(classe);
        evaluation.setEnseignant(enseignant);
        appliquer(evaluation, requete);
        Evaluation enregistree = evaluationRepository.save(evaluation);
        log.info("Évaluation planifiée — {} ({}), enseignant={}", enregistree.getLibelle(),
                classe.getCode(), enseignant.getMatricule());
        return versReponse(enregistree, effectif(classe.getId()).size());
    }

    @Override
    @Transactional
    public EnseignementResponses.Evaluation modifier(UUID utilisateurId, UUID evaluationId,
                                                     EnseignementRequests.Evaluation requete) {
        Evaluation evaluation = evaluationDe(enseignants.resoudre(utilisateurId), evaluationId);
        int bareme = requete.bareme() == null ? 20 : requete.bareme();
        // Les notes sont stockées sur 20 : changer le barème après coup réinterpréterait
        // des résultats déjà saisis (7 sur 10 deviendrait 7 sur 20).
        if (bareme != evaluation.getBareme() && noteRepository.countByEvaluationId(evaluationId) > 0) {
            throw new BusinessException(
                    "Des notes sont déjà saisies : le barème ne peut plus changer", HttpStatus.CONFLICT);
        }
        appliquer(evaluation, requete);
        return versReponse(evaluationRepository.save(evaluation), effectif(evaluation.getClasse().getId()).size());
    }

    @Override
    @Transactional
    public void supprimer(UUID utilisateurId, UUID evaluationId) {
        Evaluation evaluation = evaluationDe(enseignants.resoudre(utilisateurId), evaluationId);
        if (noteRepository.countByEvaluationId(evaluationId) > 0) {
            throw new BusinessException(
                    "Des notes sont rattachées à cette évaluation : elle ne peut pas être supprimée",
                    HttpStatus.CONFLICT);
        }
        absenceRepository.deleteAll(absenceRepository.findByEvaluationId(evaluationId));
        evaluationRepository.delete(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public EnseignementResponses.ResultatsEvaluation resultats(UUID utilisateurId, UUID evaluationId) {
        Evaluation evaluation = evaluationDe(enseignants.resoudre(utilisateurId), evaluationId);
        return construire(evaluation);
    }

    @Override
    @Transactional
    public EnseignementResponses.ResultatsEvaluation enregistrerResultats(
            UUID utilisateurId, UUID evaluationId, EnseignementRequests.Resultats requete) {
        Personnel enseignant = enseignants.resoudre(utilisateurId);
        Evaluation evaluation = evaluationDe(enseignant, evaluationId);

        Map<UUID, Etudiant> effectif = effectif(evaluation.getClasse().getId()).stream()
                .collect(Collectors.toMap(Etudiant::getId, Function.identity()));
        Map<UUID, Note> notes = noteRepository.findParEvaluation(evaluationId).stream()
                .collect(Collectors.toMap(n -> n.getEtudiant().getId(), Function.identity()));
        Map<UUID, AbsenceEvaluation> absences = absenceRepository.findByEvaluationId(evaluationId).stream()
                .collect(Collectors.toMap(a -> a.getEtudiant().getId(), Function.identity()));

        long distincts = requete.resultats().stream()
                .map(EnseignementRequests.Resultat::etudiantId).distinct().count();
        if (distincts != requete.resultats().size()) {
            throw new BusinessException("Un étudiant figure plusieurs fois dans la saisie");
        }

        BigDecimal bareme = BigDecimal.valueOf(evaluation.getBareme());
        for (EnseignementRequests.Resultat saisie : requete.resultats()) {
            Etudiant etudiant = effectif.get(saisie.etudiantId());
            if (etudiant == null) {
                throw new BusinessException("Un des étudiants n'appartient pas à cette classe");
            }
            Note existante = notes.get(etudiant.getId());
            if (existante != null && existante.getStatut() != StatutNote.BROUILLON) {
                throw new BusinessException("La note de " + etudiant.getPrenom() + " " + etudiant.getNom()
                        + " est déjà transmise : elle n'est plus modifiable", HttpStatus.CONFLICT);
            }

            if (saisie.absent()) {
                if (existante != null) {
                    noteRepository.delete(existante);
                }
                AbsenceEvaluation absence = absences.get(etudiant.getId());
                if (absence == null) {
                    absence = new AbsenceEvaluation();
                    absence.setEvaluation(evaluation);
                    absence.setEtudiant(etudiant);
                }
                absence.setMotif(saisie.motif() == null || saisie.motif().isBlank() ? null : saisie.motif().trim());
                absenceRepository.save(absence);
                continue;
            }

            if (saisie.valeur() == null) {
                throw new BusinessException("Saisissez une note ou marquez l'étudiant absent ("
                        + etudiant.getPrenom() + " " + etudiant.getNom() + ")");
            }
            if (saisie.valeur().compareTo(bareme) > 0) {
                throw new BusinessException("La note de " + etudiant.getPrenom() + " " + etudiant.getNom()
                        + " dépasse le barème (" + evaluation.getBareme() + ")");
            }
            AbsenceEvaluation absence = absences.get(etudiant.getId());
            if (absence != null) {
                absenceRepository.delete(absence);
            }

            Note note = existante != null ? existante : new Note();
            if (existante == null) {
                note.setEtudiant(etudiant);
                note.setEnseignant(enseignant);
                note.setEvaluation(evaluation);
            }
            note.setMatiere(evaluation.getMatiere());
            note.setValeur(saisie.valeur().multiply(VINGT).divide(bareme, 2, RoundingMode.HALF_UP));
            note.setCoefficient(evaluation.getCoefficient());
            note.setType(evaluation.getType());
            note.setPeriode(evaluation.getPeriode());
            note.setLibelle(evaluation.getLibelle());
            note.setDateEvaluation(evaluation.getDateEvaluation());
            note.setAppreciation(saisie.appreciation() == null || saisie.appreciation().isBlank()
                    ? null : saisie.appreciation().trim());
            note.setStatut(StatutNote.BROUILLON);
            note.setMotifRejet(null);
            noteRepository.save(note);
        }
        return construire(evaluation);
    }

    @Override
    @Transactional
    public List<NoteResponse> transmettre(UUID utilisateurId, UUID evaluationId) {
        evaluationDe(enseignants.resoudre(utilisateurId), evaluationId);
        List<UUID> brouillons = noteRepository.findParEvaluation(evaluationId).stream()
                .filter(n -> n.getStatut() == StatutNote.BROUILLON)
                .map(Note::getId)
                .toList();
        if (brouillons.isEmpty()) {
            throw new BusinessException("Aucune note en brouillon à transmettre", HttpStatus.CONFLICT);
        }
        return noteService.transmettre(utilisateurId, brouillons);
    }

    // ------------------------------------------------------------------

    private EnseignementResponses.ResultatsEvaluation construire(Evaluation evaluation) {
        Map<UUID, Note> notes = noteRepository.findParEvaluation(evaluation.getId()).stream()
                .collect(Collectors.toMap(n -> n.getEtudiant().getId(), Function.identity()));
        Map<UUID, AbsenceEvaluation> absences = absenceRepository.findByEvaluationId(evaluation.getId()).stream()
                .collect(Collectors.toMap(a -> a.getEtudiant().getId(), Function.identity()));
        BigDecimal bareme = BigDecimal.valueOf(evaluation.getBareme());

        List<Etudiant> inscrits = effectif(evaluation.getClasse().getId());
        List<EnseignementResponses.Resultat> lignes = inscrits.stream().map(e -> {
            Note note = notes.get(e.getId());
            AbsenceEvaluation absence = absences.get(e.getId());
            BigDecimal surBareme = note == null ? null
                    : note.getValeur().multiply(bareme).divide(VINGT, 2, RoundingMode.HALF_UP);
            return new EnseignementResponses.Resultat(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(),
                    surBareme, note == null ? null : note.getValeur(),
                    note == null ? null : note.getId(), note == null ? null : note.getStatut(),
                    note == null ? null : note.getMotifRejet(),
                    absence != null, absence == null ? null : absence.getMotif());
        }).toList();

        return new EnseignementResponses.ResultatsEvaluation(
                versReponse(evaluation, inscrits.size(), notes, absences.size()), lignes);
    }

    private EnseignementResponses.Evaluation versReponse(Evaluation evaluation, int effectif) {
        Map<UUID, Note> notes = noteRepository.findParEvaluation(evaluation.getId()).stream()
                .collect(Collectors.toMap(n -> n.getEtudiant().getId(), Function.identity()));
        return versReponse(evaluation, effectif, notes, (int) absenceRepository.countByEvaluationId(evaluation.getId()));
    }

    private EnseignementResponses.Evaluation versReponse(Evaluation e, int effectif,
                                                         Map<UUID, Note> notes, int absents) {
        Map<StatutNote, Long> parStatut = new HashMap<>();
        notes.values().forEach(n -> parStatut.merge(n.getStatut(), 1L, Long::sum));
        return new EnseignementResponses.Evaluation(e.getId(), e.getClasse().getId(), e.getClasse().getCode(),
                e.getMatiere().getId(), e.getMatiere().getLibelle(), e.getType(), e.getLibelle(),
                e.getDateEvaluation(), e.getBareme(), e.getCoefficient(), e.getPeriode(), e.getSujet(),
                effectif, notes.size(), absents,
                parStatut.getOrDefault(StatutNote.BROUILLON, 0L).intValue(),
                parStatut.getOrDefault(StatutNote.TRANSMISE, 0L).intValue(),
                parStatut.getOrDefault(StatutNote.VALIDEE, 0L).intValue());
    }

    private void appliquer(Evaluation evaluation, EnseignementRequests.Evaluation requete) {
        Matiere matiere = matiereRepository.findById(requete.matiereId())
                .orElseThrow(() -> new ResourceNotFoundException("Matière", "id", requete.matiereId()));
        if (!matiere.isActive()) {
            throw new BusinessException("Cette matière est désactivée");
        }
        evaluation.setMatiere(matiere);
        evaluation.setType(requete.type() == null ? TypeEvaluation.DEVOIR : requete.type());
        evaluation.setLibelle(requete.libelle().trim());
        evaluation.setDateEvaluation(requete.dateEvaluation());
        evaluation.setBareme(requete.bareme() == null ? 20 : requete.bareme());
        evaluation.setCoefficient(requete.coefficient() == null ? 1 : requete.coefficient());
        evaluation.setPeriode(requete.periode() == null ? PeriodeScolaire.SEMESTRE_1 : requete.periode());
        evaluation.setSujet(requete.sujet() == null || requete.sujet().isBlank() ? null : requete.sujet().trim());
    }

    private Evaluation evaluationDe(Personnel enseignant, UUID evaluationId) {
        Evaluation evaluation = evaluationRepository.findAvecContexte(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation", "id", evaluationId));
        if (!evaluation.getEnseignant().getId().equals(enseignant.getId())) {
            throw new BusinessException("Cette évaluation n'est pas la vôtre", HttpStatus.FORBIDDEN);
        }
        return evaluation;
    }

    /** Étudiants actifs d'une classe, dans l'ordre alphabétique d'une feuille d'appel. */
    private List<Etudiant> effectif(Long classeId) {
        return etudiantRepository.findByClasseId(classeId).stream()
                .filter(Etudiant::isActif)
                .sorted(Comparator.comparing(Etudiant::getNom, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Etudiant::getPrenom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
