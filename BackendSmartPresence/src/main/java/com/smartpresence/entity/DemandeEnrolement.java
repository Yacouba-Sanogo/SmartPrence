package com.smartpresence.entity;

import com.smartpresence.constants.StatutDemandeEnrolement;
import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Rendez-vous entre un étudiant et un lecteur, pour enrôler son empreinte.
 *
 * <h2>Pourquoi une entité, et pas un simple appel</h2>
 * <p>L'enrôlement met en jeu deux machines qui ne se parlent pas : le téléphone de
 * l'étudiant, qui sait <b>qui</b> demande, et le lecteur, qui sait <b>quelle</b>
 * empreinte vient d'être capturée. Ni l'un ni l'autre ne peut à lui seul faire le
 * lien. Cette demande est le point de rendez-vous où le serveur les rapproche.</p>
 *
 * <h2>Le code, et pourquoi il ne s'agit pas d'un mot de passe</h2>
 * <p>Le lecteur ne reçoit jamais d'identifiant interne — ni UUID d'étudiant, ni
 * identifiant de demande : il ne connaît que des <b>références logiques</b>
 * (cf. {@code SmartPresence_CONTEXT.md} §13). Le {@link #code} tient ce rôle. Il
 * s'affiche des deux côtés — dans l'application et sur l'écran du lecteur — et
 * c'est cette concordance, vérifiée par l'étudiant lui-même avant de poser le
 * doigt, qui garantit que le capteur sert bien <i>sa</i> demande et non celle du
 * camarade qui attend derrière lui.</p>
 *
 * <p>Aucune donnée biométrique ne transite ici : {@link #biometricId} est la
 * référence du emplacement occupé dans la mémoire du capteur AS608, jamais un
 * gabarit.</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(
        name = TableNames.DEMANDES_ENROLEMENT,
        indexes = {
                @Index(name = "idx_demandes_enrolement_statut", columnList = "statut"),
                @Index(name = "idx_demandes_enrolement_code", columnList = TableNames.COL_CODE)
        }
)
@Getter
@Setter
@ToString(exclude = {"etudiant", "device"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class DemandeEnrolement extends BaseAuditableEntity {

    /** Identifiant technique, interne au serveur : il ne sort jamais vers un lecteur. */
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Étudiant qui demande à être enrôlé. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = TableNames.COL_ETUDIANT_ID, nullable = false,
            foreignKey = @ForeignKey(name = "fk_demandes_enrolement_etudiant"))
    private Etudiant etudiant;

    /**
     * Code de rendez-vous, affiché à l'étudiant et sur le lecteur.
     *
     * <p>Unique parmi les demandes en attente seulement : il est fait pour être lu
     * à voix basse devant un capteur, pas pour être conservé.</p>
     */
    @Column(name = TableNames.COL_CODE, nullable = false, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutDemandeEnrolement statut = StatutDemandeEnrolement.EN_ATTENTE;

    /** Au-delà de cet instant, la demande ne sera plus servie. */
    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    /**
     * Lecteur qui a pris la demande en charge.
     *
     * <p>Renseigné dès qu'un lecteur l'annonce à son écran, avant même la capture :
     * c'est ce qui empêche un second lecteur d'appeler le même étudiant au même
     * moment, et ce qui permet de dire, après coup, devant quel capteur l'empreinte
     * a été posée.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = TableNames.COL_DEVICE_ID,
            foreignKey = @ForeignKey(name = "fk_demandes_enrolement_device"))
    private Device device;

    /** Instant où un lecteur a pris la demande en charge. */
    @Column(name = "date_reservation")
    private Instant dateReservation;

    /** Référence logique produite par le capteur — jamais une empreinte. */
    @Column(name = TableNames.COL_BIOMETRIC_ID, length = 100)
    private String biometricId;

    /** Instant où la demande a trouvé son issue, quelle qu'elle soit. */
    @Column(name = "date_traitement")
    private Instant dateTraitement;

    /** Vrai tant que la demande peut encore être servie. */
    public boolean estOuverte(Instant maintenant) {
        return statut == StatutDemandeEnrolement.EN_ATTENTE && expireLe.isAfter(maintenant);
    }
}
