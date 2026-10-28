package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Octets d'un document de bibliothèque, en base pour la même raison que les supports de cours. */
@Entity
@Table(name = TableNames.BIBLIOTHEQUE_CONTENU)
@Getter @Setter
public class DocumentContenu {
    @Id @Column(length = 36) private UUID id;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id") private DocumentBibliotheque document;
    @Lob @Column(nullable = false, columnDefinition = "longblob") private byte[] octets;
}
