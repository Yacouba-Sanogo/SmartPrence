package com.smartpresence.entity;

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
 * Événement de présence — <b>cœur fonctionnel</b> du système SmartPresence.
 *
 * <p>Modélise exactement le payload minimal échangé avec l'ESP32 (cf.
 * {@code SmartPresence_CONTEXT.md} §3.6) : {@code studentId}, {@code deviceId},
 * {@code date}, {@code heure}, {@code statut}, {@code createdAt} (côté ESP32) et
 * {@code synchronizedAt}. Aucune donnée biométrique n'est manipulée ici.</p>
 *
 * <h2>Double horodatage (essentiel pour le mémoire)</h2>
 * <ul>
 *   <li>{@code creeLeDevice} : instant de capture côté ESP32 (RTC DS3231). Champ métier,
 *       <b>indépendant</b> de l'audit serveur {@code createdAt} (hérité de
 *       {@link BaseAuditableEntity}). Ne doit jamais être confondu avec ce dernier.</li>
 *   <li>{@code synchroniseLe} : instant de réception/persistance côté serveur.</li>
 * </ul>
 * <p>La différence {@code synchroniseLe - creeLeDevice} fournit la <b>latence de
 * synchronisation</b>, métrique clé pour l'évaluation scientifique du mémoire.</p>
 *
 * <h2>Règle métier sur Device</h2>
 * <p>L'association à un {@link Device} dépend de {@link SourcePresence} :</p>
 * <ul>
 *   <li>{@link SourcePresence#ESP32} : {@code device} <b>obligatoire</b>.</li>
 *   <li>{@link SourcePresence#MANUEL} : {@code device} <b>optionnel</b> (peut être {@code null}).</li>
 * </ul>
 * <p>Cette règle est contrôlée par la couche service et la validation métier, <b>pas par
 * JPA seul</b> (cf. {@code SmartPresence_CONTEXT.md} §9.3).</p>
 *
 * <h2>Idempotence</h2>
 * <p>Contrainte unique {@code (device_id, etudiant_id, date_presence, heure_presence)}
 * garantissant qu'un événement retransmis (reprise automatique, §8.6) n'est inséré qu'une
 * seule fois. Les présences {@code MANUEL} sans {@code device} restent autorisées
 * (les {@code NULL} multiples sont distincts en MySQL/InnoDB).</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.PRESENCES,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_presences_idempotence",
                columnNames = {
                        TableNames.COL_DEVICE_ID,
                        TableNames.COL_ETUDIANT_ID,
                        TableNames.COL_DATE_PRESENCE,
                        TableNames.COL_HEURE_PRESENCE
                }
        ),
        indexes = {
                @Index(name = "idx_presences_etudiant_date",
                        columnList = TableNames.COL_ETUDIANT_ID + "," + TableNames.COL_DATE_PRESENCE),
                @Index(name = "idx_presences_device_date",
                        columnList = TableNames.COL_DEVICE_ID + "," + TableNames.COL_DATE_PRESENCE)
        }
)
@Getter
@Setter
@ToString(exclude = {"etudiant", "device"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Presence extends BaseAuditableEntity {

    /**
     * Identifiant technique de l'événement, généré côté application.
     */
    @EqualsAndHashCode.Include
    @Id
    @Column(name = "id", length = 36)
    private UUID id;

    /**
     * Étudiant concerné par l'événement de présence.
     *
     * <p><b>N-1 propriétaire, LAZY</b>, non-null. <b>Aucune cascade</b>.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = TableNames.COL_ETUDIANT_ID, nullable = false,
            foreignKey = @ForeignKey(name = "fk_presences_etudiant"))
    private Etudiant etudiant;

    /**
     * Appareil à l'origine de l'événement.
     *
     * <p><b>N-1 propriétaire, LAZY, NULLABLE</b>. <b>Obligatoire</b> si
     * {@link SourcePresence#ESP32}, <b>optionnel</b> si {@link SourcePresence#MANUEL}
     * (règle contrôlée par la couche service). <b>Aucune cascade</b>.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = TableNames.COL_DEVICE_ID,
            foreignKey = @ForeignKey(name = "fk_presences_device"))
    private Device device;

    /** Séance académique concernée. Nullable pour les pointages ESP32 hors créneau identifié. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seance_id", foreignKey = @ForeignKey(name = "fk_presences_seance"))
    private Seance seance;

    /**
     * Date de la présence (provenant de l'ESP32 pour la source {@code ESP32}).
     */
    @Column(name = TableNames.COL_DATE_PRESENCE, nullable = false)
    private LocalDate datePresence;

    /**
     * Heure précise de la présence (RTC DS3231 côté ESP32).
     */
    @Column(name = TableNames.COL_HEURE_PRESENCE, nullable = false)
    private LocalTime heurePresence;

    /**
     * Statut de présence ({@code PRESENT}, {@code RETARD}, {@code ABSENT}, {@code JUSTIFIE}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutPresence statut;

    /**
     * Origine de l'événement.
     * <p>{@link SourcePresence#ESP32} (automatique) ou {@link SourcePresence#MANUEL}
     * (saisie/correction administrative).</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SourcePresence source = SourcePresence.ESP32;

    /**
     * Instant de création côté ESP32 (RTC DS3231).
     * <p>Champ métier <b>indépendant</b> de l'audit serveur {@code createdAt} (hérité).
     * Correspond au {@code createdAt} du payload ESP32.</p>
     */
    @Column(name = "cree_le_device", nullable = false)
    private Instant creeLeDevice;

    /**
     * Instant de synchronisation réussie avec le backend (réception serveur).
     * <p>Correspond au {@code synchronizedAt} du modèle.</p>
     */
    @Column(name = "synchronise_le", nullable = false)
    private Instant synchroniseLe;

    /**
     * Génère l'identifiant {@link UUID} avant l'insertion en base si nécessaire.
     */
    @PrePersist
    void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
