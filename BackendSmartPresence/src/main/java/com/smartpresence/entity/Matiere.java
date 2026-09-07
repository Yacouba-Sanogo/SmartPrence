package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** Unité d'enseignement ou matière rattachée aux séances de cours. */
@Entity @Table(name = TableNames.MATIERES, uniqueConstraints = @UniqueConstraint(name = "uk_matieres_code", columnNames = TableNames.COL_CODE))
@Getter @Setter @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Matiere extends BaseAuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @Column(name = TableNames.COL_CODE, nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 150) private String libelle;
    @Column private Integer credits;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private boolean active = true;
}
