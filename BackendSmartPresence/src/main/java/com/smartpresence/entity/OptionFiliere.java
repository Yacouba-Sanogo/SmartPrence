package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Option (spécialité) d'une filière, ouverte à partir de la 3e année. */
@Entity
@Table(name = TableNames.OPTIONS_FILIERE,
        uniqueConstraints = @UniqueConstraint(name = "uk_options_filiere_code",
                columnNames = TableNames.COL_CODE))
@Getter
@Setter
@ToString(exclude = "filiere")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class OptionFiliere extends BaseAuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = TableNames.COL_CODE, nullable = false, length = 40)
    private String code;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filiere_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_options_filiere_filiere"))
    private Filiere filiere;
}
