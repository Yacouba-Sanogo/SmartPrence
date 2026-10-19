package com.smartpresence.entity;

import com.smartpresence.constants.TableNames;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Octets d'un support de cours.
 *
 * <p>Stockés en base plutôt que sur disque : l'instance de production est éphémère, un
 * fichier écrit sur son disque disparaîtrait au prochain redémarrage alors que la
 * ligne de métadonnées, elle, resterait.</p>
 */
@Entity
@Table(name = TableNames.SUPPORTS_CONTENU)
@Getter @Setter
public class SupportContenu {
    @Id @Column(length = 36) private UUID id;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id") private SupportCours support;
    @Lob @Column(nullable = false, columnDefinition = "longblob") private byte[] octets;
}
