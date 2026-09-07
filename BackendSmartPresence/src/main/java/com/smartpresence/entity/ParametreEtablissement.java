package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Configuration singleton de l'établissement, administrable sans constantes codées en dur.
 *
 * <p>Une seule ligne, d'identifiant fixe {@code 1}. Les règles horaires du pointage du
 * personnel y sont centralisées : elles varient d'un établissement à l'autre et doivent
 * rester modifiables sans redéploiement.</p>
 *
 * @since 0.0.1
 */
@Entity
@Table(name = TableNames.PARAMETRES_ETABLISSEMENT)
@Getter
@Setter
public class ParametreEtablissement extends BaseAuditableEntity {

    /** Identifiant fixe du singleton de configuration. */
    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(length = 80)
    private String sigle;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telephone;

    @Column(length = 120)
    private String fuseauHoraire = "Africa/Bamako";

    /**
     * Tolérance, en minutes, au-delà de laquelle une arrivée est qualifiée de
     * {@code RETARD}. S'applique aux étudiants comme au personnel.
     */
    @Column(nullable = false)
    private int seuilRetardMinutes = 15;

    /**
     * Heure théorique de prise de service du personnel.
     * <p>Un pointage d'entrée postérieur à {@code heureOuverture + seuilRetardMinutes}
     * est marqué en retard.</p>
     */
    @Column(name = "heure_ouverture", nullable = false)
    private LocalTime heureOuverture = LocalTime.of(8, 0);

    /**
     * Heure théorique de fin de service du personnel.
     * <p>Sert de borne de référence pour le calcul du temps de présence journalier.</p>
     */
    @Column(name = "heure_fermeture", nullable = false)
    private LocalTime heureFermeture = LocalTime.of(17, 0);
}
