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

    /** Notes d'un etudiant, toutes matieres, filtrables par periode. */
    @Query("""
            SELECT n FROM Note n
            JOIN FETCH n.matiere
            JOIN FETCH n.enseignant
            WHERE n.etudiant.id = :etudiantId
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

    /** Nombre de notes rattachees a une matiere, pour refuser sa suppression. */
    long countByMatiereId(Long matiereId);
}
