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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Filière d'entrée en 1re année. Les {@link OptionFiliere options} (spécialités) ne
 * s'ouvrent qu'à partir de la 3e année du cursus LMD.
 */
@Entity
@Table(name = TableNames.FILIERES,
        uniqueConstraints = @UniqueConstraint(name = "uk_filieres_code",
                columnNames = TableNames.COL_CODE))
@Getter
@Setter
@ToString(exclude = {"options", "departement"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Filiere extends BaseAuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = TableNames.COL_CODE, nullable = false, length = 30)
    private String code;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    /** DER qui porte la filière. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "departement_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_filieres_departement"))
    private Departement departement;

    @OneToMany(mappedBy = "filiere", fetch = FetchType.LAZY)
    @OrderBy("libelle ASC")
    private List<OptionFiliere> options = new ArrayList<>();
}
