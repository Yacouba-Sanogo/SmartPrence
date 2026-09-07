package com.smartpresence.entity;
import com.smartpresence.constants.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*; import java.util.UUID;
/** Demande de justification d'absence, traçable et soumise à validation scolarité. */
@Entity @Table(name=TableNames.JUSTIFICATIONS_ABSENCE) @Getter @Setter @EqualsAndHashCode(onlyExplicitlyIncluded=true,callSuper=false)
public class JustificationAbsence extends BaseAuditableEntity {
 @Id @EqualsAndHashCode.Include @Column(length=36) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="etudiant_id",nullable=false) private Etudiant etudiant;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="seance_id") private Seance seance;
 @Column(nullable=false) private LocalDate dateAbsence;
 @Column(nullable=false,length=1000) private String motif;
 @Column(length=500) private String pieceJointeUrl;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StatutJustification statut=StatutJustification.EN_ATTENTE;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="traite_par_id") private Utilisateur traitePar;
 @Column(length=500) private String commentaireTraitement;
 @PrePersist void id(){if(id==null)id=UUID.randomUUID();}
}
