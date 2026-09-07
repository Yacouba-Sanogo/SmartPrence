package com.smartpresence.repository;

import com.smartpresence.constants.StatutSignalement;
import com.smartpresence.entity.SignalementPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux signalements d'anomalie déposés par les enseignants.
 *
 * @since 0.0.1
 */
@Repository
public interface SignalementRepository extends JpaRepository<SignalementPresence, UUID> {

    /**
     * Signalements déposés par un enseignant, du plus récent au plus ancien.
     *
     * <p>{@code JOIN FETCH} sur la séance et son contexte : la liste affiche la matière
     * et la classe de chaque ligne.</p>
     */
    @Query("""
            SELECT s FROM SignalementPresence s
            JOIN FETCH s.seance se
            JOIN FETCH se.matiere
            JOIN FETCH se.classe
            JOIN FETCH s.enseignant
            LEFT JOIN FETCH s.etudiant
            WHERE s.enseignant.id = :enseignantId
            ORDER BY s.createdAt DESC
            """)
    List<SignalementPresence> findParEnseignant(@Param("enseignantId") UUID enseignantId);

    /** File d'arbitrage de la scolarité, filtrable par statut. */
    @Query("""
            SELECT s FROM SignalementPresence s
            JOIN FETCH s.seance se
            JOIN FETCH se.matiere
            JOIN FETCH se.classe
            JOIN FETCH s.enseignant
            LEFT JOIN FETCH s.etudiant
            WHERE (:statut IS NULL OR s.statut = :statut)
            ORDER BY s.createdAt DESC
            """)
    List<SignalementPresence> rechercher(@Param("statut") StatutSignalement statut);

    /** Signalement chargé avec son contexte complet, pour l'arbitrage. */
    @Query("""
            SELECT s FROM SignalementPresence s
            JOIN FETCH s.seance se
            JOIN FETCH se.matiere
            JOIN FETCH se.classe
            JOIN FETCH s.enseignant
            LEFT JOIN FETCH s.etudiant
            WHERE s.id = :id
            """)
    Optional<SignalementPresence> findAvecContexte(@Param("id") UUID id);

    long countByStatut(StatutSignalement statut);
}
