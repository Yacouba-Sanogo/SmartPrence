package com.smartpresence.entity;
import com.smartpresence.constants.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant; import java.util.UUID;
/** Notification interne prête pour les canaux push et email ultérieurs. */
@Entity @Table(name=TableNames.NOTIFICATIONS,indexes=@Index(name="idx_notifications_destinataire_lue",columnList="destinataire_id,lue")) @Getter @Setter @EqualsAndHashCode(onlyExplicitlyIncluded=true,callSuper=false)
public class Notification extends BaseAuditableEntity {
 @Id @EqualsAndHashCode.Include @Column(length=36) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="destinataire_id",nullable=false) private Utilisateur destinataire;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TypeNotification type=TypeNotification.INFORMATION;
 @Column(nullable=false,length=160) private String titre;
 @Column(nullable=false,length=2000) private String message;
 @Column(nullable=false) private boolean lue=false;
 @Column private Instant lueLe;
 @PrePersist void id(){if(id==null)id=UUID.randomUUID();}
}
