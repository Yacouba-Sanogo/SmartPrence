package com.smartpresence.repository;

import com.smartpresence.entity.Seance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acces aux seances de cours.
 *
 * @since 0.0.1
 */
@Repository
public interface SeanceRepository extends JpaRepository<Seance, UUID> {

    List<Seance> findByClasseIdAndDebutBetweenOrderByDebutAsc(Long classeId, Instant debut, Instant fin);

    /** Nombre de seances rattachees a une matiere, pour refuser sa suppression. */
    long countByMatiereId(Long matiereId);

    /**
     * Seances assurees par un enseignant sur une periode.
     *
     * <p>{@code JOIN FETCH} sur la classe, la matiere et la salle : l'emploi du temps
     * affiche ces trois informations pour chaque ligne, et un chargement paresseux
     * produirait ici un probleme N+1.</p>
     */
    @Query("""
            SELECT s FROM Seance s
            JOIN FETCH s.classe
            JOIN FETCH s.matiere
            LEFT JOIN FETCH s.salle
            WHERE s.enseignant.id = :enseignantId
              AND s.debut BETWEEN :debut AND :fin
            ORDER BY s.debut ASC
            """)
    List<Seance> findParEnseignant(@Param("enseignantId") UUID enseignantId,
                                   @Param("debut") Instant debut,
                                   @Param("fin") Instant fin);

    /**
     * Seances d'une periode, filtrables par classe et par enseignant.
     *
     * <p>Les parametres nuls desactivent leur filtre : une seule requete sert donc
     * l'emploi du temps complet comme la vue d'une classe.</p>
     */
    @Query("""
            SELECT s FROM Seance s
            JOIN FETCH s.classe
            JOIN FETCH s.matiere
            JOIN FETCH s.enseignant
            LEFT JOIN FETCH s.salle
            WHERE s.debut >= :debut AND s.debut < :fin
              AND (:classeId IS NULL OR s.classe.id = :classeId)
              AND (:enseignantId IS NULL OR s.enseignant.id = :enseignantId)
            ORDER BY s.debut ASC
            """)
    List<Seance> rechercher(@Param("debut") Instant debut,
                            @Param("fin") Instant fin,
                            @Param("classeId") Long classeId,
                            @Param("enseignantId") UUID enseignantId);

    /**
     * Seances qui empietent sur un creneau pour la meme classe, le meme enseignant
     * ou la meme salle.
     *
     * <p>Deux creneaux se chevauchent des lors que l'un commence avant que l'autre ne
     * finisse, dans les deux sens. Comparer seulement les heures de debut laisserait
     * passer une seance entierement contenue dans une autre.</p>
     *
     * @param exclusion seance a ignorer, pour qu'une modification ne se detecte pas
     *                  elle-meme comme conflit ; {@code null} a la creation
     */
    @Query("""
            SELECT s FROM Seance s
            JOIN FETCH s.classe
            JOIN FETCH s.matiere
            JOIN FETCH s.enseignant
            LEFT JOIN FETCH s.salle
            WHERE s.statut <> com.smartpresence.constants.StatutSeance.ANNULEE
              AND s.debut < :fin AND s.fin > :debut
              AND (:exclusion IS NULL OR s.id <> :exclusion)
              AND (s.classe.id = :classeId
                   OR s.enseignant.id = :enseignantId
                   OR (:salleId IS NOT NULL AND s.salle.id = :salleId))
            ORDER BY s.debut ASC
            """)
    List<Seance> chevauchements(@Param("debut") Instant debut,
                                @Param("fin") Instant fin,
                                @Param("classeId") Long classeId,
                                @Param("enseignantId") UUID enseignantId,
                                @Param("salleId") Long salleId,
                                @Param("exclusion") UUID exclusion);

    /**
     * Seances candidates pour rattacher un releve biometrique.
     *
     * <p>Le lecteur ne connait pas l'emploi du temps : il transmet un etudiant et une
     * heure. C'est au serveur de retrouver le cours en cours, faute de quoi le releve
     * reste orphelin et la feuille de presence de l'enseignant demeure vide.</p>
     *
     * <p>La borne basse est elargie par l'appelant : un etudiant badge en arrivant,
     * donc souvent avant l'heure de debut.</p>
     */
    @Query("""
            SELECT s FROM Seance s
            LEFT JOIN FETCH s.salle
            WHERE s.classe.id = :classeId
              AND s.statut <> com.smartpresence.constants.StatutSeance.ANNULEE
              AND s.debut <= :borneHaute AND s.fin > :instant
            ORDER BY s.debut ASC
            """)
    List<Seance> candidatesPourReleve(@Param("classeId") Long classeId,
                                      @Param("instant") Instant instant,
                                      @Param("borneHaute") Instant borneHaute);

    /** Seance chargee avec son contexte, pour la feuille de presence. */
    @Query("""
            SELECT s FROM Seance s
            JOIN FETCH s.classe
            JOIN FETCH s.matiere
            JOIN FETCH s.enseignant
            LEFT JOIN FETCH s.salle
            WHERE s.id = :id
            """)
    Optional<Seance> findAvecContexte(@Param("id") UUID id);
}
