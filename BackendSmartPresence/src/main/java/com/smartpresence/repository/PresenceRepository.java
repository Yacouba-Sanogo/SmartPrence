package com.smartpresence.repository;

import com.smartpresence.entity.Presence;
import com.smartpresence.constants.StatutPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Accès aux données des événements de présence (cœur fonctionnel).
 *
 * @since 0.0.1
 * @see Presence
 */
@Repository
public interface PresenceRepository extends JpaRepository<Presence, UUID>, JpaSpecificationExecutor<Presence> {

    long countByStatut(StatutPresence statut);

    long countByEtudiantClasseId(Long classeId);

    long countByEtudiantClasseIdAndStatut(Long classeId, StatutPresence statut);

    long countByEtudiantIdAndStatut(UUID etudiantId, StatutPresence statut);

    long countByEtudiantId(UUID etudiantId);

    /**
     * Vérifie l'existence d'un événement identique (idempotence).
     * <p>Reflète la contrainte unique {@code (device_id, etudiant_id, date_presence,
     * heure_presence)}. Utilisée avant insertion pour détecter les retransmissions
     * de la reprise automatique ESP32 (cf. {@code SmartPresence_CONTEXT.md} §8.6).</p>
     *
     * @param deviceId     identifiant de l'appareil (peut être {@code null} pour source MANUEL)
     * @param etudiantId   identifiant de l'étudiant
     * @param datePresence date de la présence
     * @param heurePresence heure de la présence
     * @return {@code true} si un événement identique existe déjà
     */
    boolean existsByDeviceIdAndEtudiantIdAndDatePresenceAndHeurePresence(
            UUID deviceId, UUID etudiantId, LocalDate datePresence,
            LocalTime heurePresence);

    /**
     * Liste les présences d'un étudiant sur une période donnée.
     *
     * @param etudiantId identifiant de l'étudiant
     * @param debut      date de début (inclusive)
     * @param fin        date de fin (inclusive)
     * @return liste des présences de l'étudiant sur la période
     */
    List<Presence> findByEtudiantIdAndDatePresenceBetween(
            UUID etudiantId, LocalDate debut, LocalDate fin);

    /**
     * Liste les présences d'un étudiant pour une date donnée.
     *
     * @param etudiantId identifiant de l'étudiant
     * @param date       date recherchée
     * @return liste des présences de l'étudiant ce jour
     */
    List<Presence> findByEtudiantIdAndDatePresence(UUID etudiantId, LocalDate date);

    /**
     * Liste les présences d'un statut donné pour une date.
     *
     * @param statut statut recherché
     * @param date   date recherchée
     * @return liste des présences correspondantes
     */
    List<Presence> findByStatutAndDatePresence(StatutPresence statut, LocalDate date);

    /** Releves rattaches a une seance, pour composer sa feuille de presence. */
    List<Presence> findBySeanceId(UUID seanceId);

    long countByDeviceId(UUID deviceId);

}
