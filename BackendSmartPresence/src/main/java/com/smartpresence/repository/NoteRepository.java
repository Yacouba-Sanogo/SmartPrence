package com.smartpresence.repository;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acces aux notes.
 *
 * <p>Toutes les requetes chargent la matiere et l'enseignant en JOIN FETCH : un bulletin
 * affiche les deux pour chaque ligne, et un chargement paresseux produirait ici un
 * probleme N+1 proportionnel au nombre de notes de l'annee.</p>
 *
 * @since 0.0.1
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    /**
     * Notes publiees d'un etudiant, toutes matieres, filtrables par periode.
     *
     * <p>Seules les notes validees : un brouillon ou une note en attente de decision
     * n'appartient pas encore au bulletin, et le montrer ferait varier les moyennes
     * d'un etudiant au gre des saisies de ses enseignants.</p>
     */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.matiere
            JOIN FETCH n.enseignant
            WHERE n.etudiant.id = :etudiantId
              AND n.statut = com.smartpresence.constants.StatutNote.VALIDEE
              AND (:periode IS NULL OR n.periode = :periode)
            ORDER BY n.dateEvaluation DESC
            """)
    List<Note> findParEtudiant(@Param("etudiantId") UUID etudiantId,
                               @Param("periode") PeriodeScolaire periode);

    /** Notes d'une classe pour une matiere donnee — la vue de saisie de l'enseignant. */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.matiere
            JOIN FETCH n.enseignant
            JOIN FETCH n.etudiant e
            WHERE e.classe.id = :classeId
              AND (:matiereId IS NULL OR n.matiere.id = :matiereId)
              AND (:periode IS NULL OR n.periode = :periode)
            ORDER BY e.nom ASC, e.prenom ASC, n.dateEvaluation DESC
            """)
    List<Note> findParClasse(@Param("classeId") Long classeId,
                             @Param("matiereId") Long matiereId,
                             @Param("periode") PeriodeScolaire periode);

    /** Note chargee avec son contexte, pour la modification et la suppression. */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.matiere
            JOIN FETCH n.enseignant
            JOIN FETCH n.etudiant e
            JOIN FETCH e.classe
            WHERE n.id = :id
            """)
    Optional<Note> findAvecContexte(@Param("id") UUID id);

    /** Notes issues d'une evaluation, avec l'etudiant. */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.etudiant
            JOIN FETCH n.matiere
            JOIN FETCH n.enseignant
            WHERE n.evaluation.id = :evaluationId
            """)
    List<Note> findParEvaluation(@Param("evaluationId") UUID evaluationId);

    /** Notes a un statut donne, pour la file de validation de l'administration. */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.matiere
            JOIN FETCH n.enseignant
            JOIN FETCH n.etudiant e
            JOIN FETCH e.classe c
            WHERE n.statut = :statut
              AND (:classeId IS NULL OR c.id = :classeId)
            ORDER BY c.code ASC, n.matiere.libelle ASC, e.nom ASC, e.prenom ASC
            """)
    List<Note> findParStatut(@Param("statut") com.smartpresence.constants.StatutNote statut,
                             @Param("classeId") Long classeId);

    /** Lot de notes avec leur contexte, pour transmettre, valider ou renvoyer. */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.matiere
            JOIN FETCH n.enseignant
            JOIN FETCH n.etudiant e
            WHERE n.id IN :ids
            """)
    List<Note> findAvecContexteParIds(@Param("ids") java.util.Collection<UUID> ids);

    long countByEvaluationId(UUID evaluationId);

    /** Nombre de notes rattachees a une matiere, pour refuser sa suppression. */
    long countByMatiereId(Long matiereId);
}
