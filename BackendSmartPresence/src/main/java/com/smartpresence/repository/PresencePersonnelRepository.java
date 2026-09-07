package com.smartpresence.repository;

import com.smartpresence.entity.PresencePersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données des pointages d'arrivée/départ du personnel.
 *
 * @since 0.0.1
 * @see PresencePersonnel
 */
@Repository
public interface PresencePersonnelRepository
        extends JpaRepository<PresencePersonnel, UUID>, JpaSpecificationExecutor<PresencePersonnel> {

    /**
     * Vérifie l'existence d'un pointage identique (idempotence).
     *
     * <p>Reflète la contrainte unique {@code (device_id, personnel_id, date_pointage,
     * heure_pointage)}. Appelée avant insertion pour absorber les retransmissions de la
     * reprise automatique ESP32 (cf. {@code SmartPresence_CONTEXT.md} §8.6).</p>
     *
     * @param deviceId      appareil émetteur ({@code null} pour une saisie manuelle)
     * @param personnelId   agent concerné
     * @param datePointage  date du pointage
     * @param heurePointage heure du pointage
     * @return {@code true} si un pointage identique existe déjà
     */
    boolean existsByDeviceIdAndPersonnelIdAndDatePointageAndHeurePointage(
            UUID deviceId, UUID personnelId, LocalDate datePointage, LocalTime heurePointage);

    /**
     * Pointages d'un agent pour une date, du plus ancien au plus récent.
     * <p>Le premier élément porte l'<b>heure d'entrée</b> de la journée.</p>
     */
    List<PresencePersonnel> findByPersonnelIdAndDatePointageOrderByHeurePointageAsc(
            UUID personnelId, LocalDate datePointage);

    /** Dernier pointage connu d'un agent pour une date — sert à déterminer le sens suivant. */
    Optional<PresencePersonnel> findFirstByPersonnelIdAndDatePointageOrderByHeurePointageDesc(
            UUID personnelId, LocalDate datePointage);

    /** Nombre de pointages enregistrés pour un agent à une date donnée. */
    long countByPersonnelIdAndDatePointage(UUID personnelId, LocalDate datePointage);

    /**
     * Tous les pointages d'une journée, agent puis heure croissante.
     *
     * <p>{@code JOIN FETCH} sur l'agent et l'appareil : le tableau de bord du jour affiche
     * ces informations pour chaque ligne, les charger paresseusement provoquerait un
     * problème N+1.</p>
     */
    @Query("""
            SELECT p FROM PresencePersonnel p
            JOIN FETCH p.personnel
            LEFT JOIN FETCH p.device
            WHERE p.datePointage = :date
            ORDER BY p.personnel.nom ASC, p.heurePointage ASC
            """)
    List<PresencePersonnel> findJourneeComplete(@Param("date") LocalDate date);

    /** Pointages d'un agent sur une période, pour le calcul du cumul d'heures. */
    List<PresencePersonnel> findByPersonnelIdAndDatePointageBetweenOrderByDatePointageAscHeurePointageAsc(
            UUID personnelId, LocalDate debut, LocalDate fin);

    /** Nombre d'agents distincts ayant pointé à une date — indicateur du tableau de bord. */
    @Query("SELECT COUNT(DISTINCT p.personnel.id) FROM PresencePersonnel p WHERE p.datePointage = :date")
    long countPersonnelsPresents(@Param("date") LocalDate date);

    /** Nombre de pointages produits par un appareil — garde-fou avant suppression. */
    long countByDeviceId(UUID deviceId);
}
