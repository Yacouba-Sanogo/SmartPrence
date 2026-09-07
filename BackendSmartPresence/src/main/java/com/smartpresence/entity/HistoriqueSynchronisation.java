package com.smartpresence.entity;

import com.smartpresence.constants.StatutSynchronisation;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Journal d'audit des opérations de synchronisation entre un appareil ESP32 et le backend.
 *
 * <p>Sert à la <b>traçabilité</b> et à l'<b>évaluation scientifique</b> du mémoire :
 * fiabilité réseau, nombre de tentatives, performances de synchronisation
 * (cf. {@code SmartPresence_CONTEXT.md} §4.9).</p>
 *
 * <h2>Relation vers Device</h2>
 * <p><b>N-1 propriétaire, LAZY</b>, non-null. <b>Aucune cascade</b> : un appareil ne doit
 * pas pouvoir être supprimé tant qu'un historique existe (intégrité d'audit).</p>
 *
 * @since 0.0.1
 * @see Device
 */
@Entity
@Table(
        name = TableNames.HISTORIQUES_SYNCHRONISATION,
        indexes = @Index(name = "idx_historiques_device_date",
                columnList = "device_id, date_heure")
)
@Getter
@Setter
@ToString(exclude = {"device"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class HistoriqueSynchronisation extends BaseAuditableEntity {

    /**
     * Identifiant technique de l'entrée d'historique, généré côté application.
     */
    @EqualsAndHashCode.Include
    @Id
    @Column(name = "id", length = 36)
    private UUID id;

    /**
     * Appareil à l'origine de l'opération de synchronisation.
     *
     * <p><b>N-1 propriétaire, LAZY</b>, non-null. <b>Aucune cascade</b>.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_historiques_device"))
    private Device device;

    /**
     * Date/heure de l'opération de synchronisation.
     */
    @Column(name = "date_heure", nullable = false)
    private Instant dateHeure;

    /**
     * Résultat de l'opération ({@link StatutSynchronisation#SUCCES},
     * {@link StatutSynchronisation#ECHEC}, {@link StatutSynchronisation#PARTIEL}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutSynchronisation statut;

    /**
     * Nombre d'événements traités lors de l'opération (optionnel).
     */
    @Column(name = "nb_evenements")
    private Integer nbEvenements;

    /**
     * Nombre de tentatives effectuées avant l'aboutissement (ou l'abandon) de l'opération.
     *
     * <p>Métrique d'<b>évaluation scientifique</b> : fiabilité réseau et performance
     * de synchronisation (cf. {@code SmartPresence_CONTEXT.md} §4.9).</p>
     */
    @Column(name = "nombre_tentatives")
    private Integer nombreTentatives;

    /**
     * Message d'erreur éventuel (uniquement en cas d'échec ou de synchronisation partielle).
     */
    @Column(name = "message_erreur", length = 1000)
    private String messageErreur;

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
