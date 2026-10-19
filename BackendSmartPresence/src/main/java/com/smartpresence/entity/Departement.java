package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Département d'enseignement et de recherche (DER) : l'unité qui porte les
 * enseignements, les emplois du temps et les notes d'un groupe de filières.
 */
@Entity
@Table(name = TableNames.DEPARTEMENTS,
        uniqueConstraints = @UniqueConstraint(name = "uk_departements_code",
                columnNames = TableNames.COL_CODE))
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Departement extends BaseAuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Code court, ex. {@code DER-BTP-GC}. */
    @Column(name = TableNames.COL_CODE, nullable = false, length = 30)
    private String code;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;
}
