package com.smartpresence.dto.response;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.constants.StatutNote;
import com.smartpresence.constants.TypeEvaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Réponses de l'espace enseignant : bilan, cahier de textes, syllabus et évaluations. */
public final class EnseignementResponses {

    private EnseignementResponses() { }

    /**
     * Bilan d'une période. {@code seancesADeclarer} compte les séances passées, ni
     * annulées ni déclarées : c'est ce que l'enseignant doit encore régulariser.
     */
    public record Bilan(
            LocalDate debut, LocalDate fin,
            int seancesPrevues, int seancesEffectuees, int seancesAnnulees, int seancesADeclarer,
            long minutesEnseignees, int joursPointes, int joursEnRetard,
            List<LigneMatiere> parMatiere) { }

    public record LigneMatiere(Long matiereId, String matiereLibelle, int effectuees, long minutes) { }

    public record Chapitre(
            UUID id, Long classeId, Long matiereId, String matiereLibelle,
            int ordre, String titre, String objectifs, boolean termine, LocalDate termineLe) { }

    /** Programme d'une matière dans une classe, avec son avancement. */
    public record Syllabus(Long classeId, Long matiereId, int total, int termines, List<Chapitre> chapitres) { }

    public record PageCahier(
            UUID id, UUID seanceId, Instant debutSeance, Long classeId, String classeCode,
            Long matiereId, String matiereLibelle, String contenu, String travailAFaire,
            UUID chapitreId, String chapitreTitre) { }

    public record Evaluation(
            UUID id, Long classeId, String classeCode, Long matiereId, String matiereLibelle,
            TypeEvaluation type, String libelle, LocalDate dateEvaluation, int bareme, int coefficient,
            PeriodeScolaire periode, String sujet,
            int effectif, int notes, int absents,
            int brouillons, int transmises, int validees) { }

    public record Resultat(
            UUID etudiantId, String matricule, String nom, String prenom,
            BigDecimal valeurSurBareme, BigDecimal valeurSur20,
            UUID noteId, StatutNote statut, String motifRejet,
            boolean absent, String motifAbsence) { }

    public record ResultatsEvaluation(Evaluation evaluation, List<Resultat> lignes) { }
}
