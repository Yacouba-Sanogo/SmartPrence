package com.smartpresence.entity;

import com.smartpresence.constants.SensPointage;
import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Pointage d'arrivée ou de départ d'un membre du personnel.
 *
 * <p>Distinct de {@link Presence}, qui modélise la présence <b>académique</b> des
 * étudiants à une séance. Ici, l'événement relève du <b>contrôle horaire</b> : un agent
 * présente son doigt au lecteur d'entrée, l'ESP32 l'identifie localement et transmet
 * l'événement. Aucune donnée biométrique n'est échangée.</p>
 *
 * <h2>Décomposition date / heure</h2>
 * <p>{@code datePointage} et {@code heurePointage} sont volontairement séparés plutôt que
 * réunis en un unique {@code Instant} : l'exploitation métier est <b>journalière</b>
 * (premier pointage du jour, cumul d'heures d'une journée, retards d'une date donnée),
 * et cette décomposition rend ces requêtes indexables sans conversion de fuseau.</p>
 *
 * <h2>Double horodatage</h2>
 * <ul>
 *   <li>{@code creeLeDevice} : instant de capture côté ESP32 (RTC DS3231).</li>
 *   <li>{@code synchroniseLe} : instant de réception côté serveur.</li>
 * </ul>
 * <p>Leur différence donne la <b>latence de synchronisation</b>, métrique reprise dans
 * l'évaluation expérimentale du mémoire — identique à celle de {@link Presence}, ce qui
 * permet de comparer les deux flux.</p>
 *
 * <h2>Idempotence</h2>
 * <p>Contrainte unique {@code (device_id, personnel_id, date_pointage, heure_pointage)}.
 * Un pointage retransmis après une coupure réseau (reprise automatique,
 * cf. {@code SmartPresence_CONTEXT.md} §8.6) n'est donc inséré qu'une seule fois.</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.PRESENCES_PERSONNEL,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_presences_personnel_idempotence",
                columnNames = {
                        TableNames.COL_DEVICE_ID,
                        TableNames.COL_PERSONNEL_ID,
                        TableNames.COL_DATE_POINTAGE,
                        TableNames.COL_HEURE_POINTAGE
                }
        ),
        indexes = {
                @Index(name = "idx_presences_personnel_date",
                        columnList = TableNames.COL_PERSONNEL_ID + "," + TableNames.COL_DATE_POINTAGE),
                @Index(name = "idx_presences_personnel_device_date",
                        columnList = TableNames.COL_DEVICE_ID + "," + TableNames.COL_DATE_POINTAGE)
        }
)
@Getter
@Setter
@ToString(exclude = {"personnel", "device"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class PresencePersonnel extends BaseAuditableEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id", length = 36)
    private UUID id;

    /** Agent concerné par le pointage. N-1 propriétaire, LAZY, non-null, aucune cascade. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = TableNames.COL_PERSONNEL_ID, nullable = false,
            foreignKey = @ForeignKey(name = "fk_presences_personnel_personnel"))
    private Personnel personnel;

    /**
     * Lecteur ESP32 à l'origine du pointage.
     *
     * <p><b>Obligatoire</b> si {@link SourcePresence#ESP32}, <b>optionnel</b> si
     * {@link SourcePresence#MANUEL} (régularisation administrative). Règle contrôlée par
     * la couche service, pas par JPA seul.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = TableNames.COL_DEVICE_ID,
            foreignKey = @ForeignKey(name = "fk_presences_personnel_device"))
    private Device device;

    /** Date du pointage (horloge RTC de l'appareil pour la source {@code ESP32}). */
    @Column(name = TableNames.COL_DATE_POINTAGE, nullable = false)
    private LocalDate datePointage;

    /** Heure précise du pointage — c'est l'<b>heure d'entrée</b> lorsque {@code sens = ENTREE}. */
    @Column(name = TableNames.COL_HEURE_POINTAGE, nullable = false)
    private LocalTime heurePointage;

    /** Entrée ou sortie. Déterminé par la couche service à l'ingestion. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sens", nullable = false, length = 20)
    private SensPointage sens = SensPointage.ENTREE;

    /**
     * Qualification de l'arrivée : {@link StatutPresence#PRESENT} ou
     * {@link StatutPresence#RETARD} selon l'heure d'ouverture et le seuil de tolérance
     * configurés dans {@link ParametreEtablissement}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutPresence statut = StatutPresence.PRESENT;

    /** Origine de l'événement : lecteur biométrique ou régularisation manuelle. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SourcePresence source = SourcePresence.ESP32;

    /**
     * Justification d'une régularisation administrative.
     *
     * <p>Obligatoire pour la source {@link SourcePresence#MANUEL}, toujours {@code null}
     * pour un pointage biométrique. Un relevé saisi à la main sans motif serait une
     * brèche dans la valeur probante du système : la contrainte est portée par la
     * validation de la requête, et le motif suit l'événement jusque dans les exports.</p>
     */
    @Column(name = "motif", length = 500)
    private String motif;

    /** Instant de capture côté ESP32 (RTC DS3231), indépendant de l'audit serveur. */
    @Column(name = "cree_le_device", nullable = false)
    private Instant creeLeDevice;

    /** Instant de réception et de persistance côté backend. */
    @Column(name = "synchronise_le", nullable = false)
    private Instant synchroniseLe;

    @PrePersist
    void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
