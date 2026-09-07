package com.smartpresence.entity;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.constants.TableNames;
import com.smartpresence.constants.UsageDevice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Appareil ESP32 déclaré et autorisé à communiquer avec le backend.
 *
 * <p>Les appareils ESP32 ne possèdent <b>pas de compte utilisateur</b> : ils
 * s'authentifient via une <b>clé d'API dédiée</b> (jamais via JWT). La clé est stockée
 * sous forme <b>hachée</b> dans {@code apiKeyHash} (cf. {@code SmartPresence_CONTEXT.md} §7.2).</p>
 *
 * <h2>Relations</h2>
 * <ul>
 *   <li>{@link Salle} : <b>1-1 propriétaire</b> ({@code salle_id} unique), LAZY. Une salle
 *       est équipée d'au plus un appareil.</li>
 *   <li>{@link Presence} : <b>1-N unidirectionnelle, LAZY</b>. <b>Aucune cascade</b>,
 *       <b>orphanRemoval = false</b> : les présences constituent un historique immuable.</li>
 *   <li>{@link HistoriqueSynchronisation} : <b>1-N unidirectionnelle, LAZY</b>.
 *       <b>Aucune cascade</b>.</li>
 * </ul>
 *
 * <p>Cet UUID correspond au {@code deviceId} échangé avec l'ESP32.</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.DEVICES,
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_devices_api_key_hash", columnNames = TableNames.COL_API_KEY_HASH),
                @UniqueConstraint(name = "uk_devices_adresse_mac", columnNames = TableNames.COL_ADRESSE_MAC)
        },
        indexes = {
                @Index(name = "idx_devices_api_key_hash", columnList = TableNames.COL_API_KEY_HASH),
                @Index(name = "idx_devices_adresse_mac", columnList = TableNames.COL_ADRESSE_MAC)
        }
)
@Getter
@Setter
@ToString(exclude = {"salle", "presences", "historiques"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Device extends BaseAuditableEntity {

    /**
     * Identifiant technique de l'appareil, généré côté application.
     * <p>Cet UUID correspond au {@code deviceId} échangé avec l'ESP32.</p>
     */
    @EqualsAndHashCode.Include
    @Id
    @Column(name = "id", length = 36)
    private UUID id;

    /**
     * Nom lisible de l'appareil (ex. {@code ESP32-Salle-B12-101}).
     */
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    /**
     * <b>Hash</b> de la clé d'API utilisée par l'appareil pour s'authentifier.
     * <p>Jamais stocké en clair. La comparaison à l'authentification se fait sur le hash.</p>
     */
    @Column(name = TableNames.COL_API_KEY_HASH, nullable = false, length = 100)
    private String apiKeyHash;

    /**
     * Adresse MAC physique de l'appareil (identifiant matériel unique).
     * <p>Permet le suivi et l'identification matérielle des équipements IoT.</p>
     */
    @Column(name = TableNames.COL_ADRESSE_MAC, nullable = false, length = 17)
    private String adresseMac;

    /**
     * État opérationnel de l'appareil.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private DeviceStatut statut = DeviceStatut.ACTIF;

    /**
     * Vocation fonctionnelle de l'appareil : lecteur de salle (étudiants), lecteur
     * d'entrée (personnel) ou polyvalent.
     *
     * <p>La couche métier s'appuie sur ce champ pour <b>refuser</b> qu'un lecteur de
     * salle alimente le flux de pointage du personnel, et inversement. Colonne
     * <b>nullable</b> pour ne pas invalider le parc déjà enregistré : une valeur absente
     * est interprétée comme {@link UsageDevice#MIXTE}.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = TableNames.COL_USAGE_DEVICE, length = 20)
    private UsageDevice usage = UsageDevice.MIXTE;

    /**
     * Version du firmware embarqué (traçabilité des déploiements).
     */
    @Column(name = "version_firmware", length = 50)
    private String versionFirmware;

    /**
     * Date/heure de la dernière connexion constatée de l'appareil.
     */
    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    /**
     * Date/heure de la dernière synchronisation réussie avec le backend.
     * <p>Indicateur de suivi IoT (cf. {@code SmartPresence_CONTEXT.md} §4.7).</p>
     */
    @Column(name = "derniere_synchronisation")
    private Instant derniereSynchronisation;

    /**
     * Salle physique dans laquelle l'appareil est installé.
     *
     * <p><b>1-1 propriétaire</b>, LAZY, <b>aucune cascade</b>.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id",
            foreignKey = @ForeignKey(name = "fk_devices_salle"))
    private Salle salle;

    /**
     * Présences produites par cet appareil.
     *
     * <p><b>1-N unidirectionnelle, LAZY</b>. <b>Aucune cascade</b>,
     * <b>orphanRemoval = false</b> : les présences forment un historique immuable.</p>
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private List<Presence> presences = new ArrayList<>();

    /**
     * Historique des opérations de synchronisation de cet appareil.
     *
     * <p><b>1-N unidirectionnelle, LAZY</b>. <b>Aucune cascade</b>.</p>
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private List<HistoriqueSynchronisation> historiques = new ArrayList<>();

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
