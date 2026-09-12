package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * ECUE — l'élément constitutif d'une {@link UniteEnseignement}, et la matière
 * rattachée aux séances de cours.
 *
 * <p>Le rattachement à l'UE est <b>facultatif</b> : le catalogue des matières
 * préexiste à la maquette LMD, et exiger l'UE dès la création aurait rendu
 * inutilisables toutes les matières déjà saisies. Une matière sans UE reste
 * enseignable et notable ; elle n'apparaît simplement pas au relevé de crédits.</p>
 */
@Entity @Table(name = TableNames.MATIERES, uniqueConstraints = @UniqueConstraint(name = "uk_matieres_code", columnNames = TableNames.COL_CODE))
@Getter @Setter @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Matiere extends BaseAuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @Column(name = TableNames.COL_CODE, nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 150) private String libelle;
    @Column private Integer credits;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private boolean active = true;

    /** UE dont cette matière est un élément constitutif. Facultatif. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_enseignement_id",
            foreignKey = @ForeignKey(name = "fk_matieres_unite_enseignement"))
    private UniteEnseignement uniteEnseignement;
}
