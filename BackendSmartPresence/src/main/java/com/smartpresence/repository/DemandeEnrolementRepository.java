package com.smartpresence.repository;

import com.smartpresence.constants.StatutDemandeEnrolement;
import com.smartpresence.entity.DemandeEnrolement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux demandes d'enrôlement biométrique.
 */
@Repository
public interface DemandeEnrolementRepository extends JpaRepository<DemandeEnrolement, Long> {

    /** Demande ouverte d'un étudiant donné, s'il en a une. */
    @Query("""
            select d from DemandeEnrolement d
            where d.etudiant.id = :etudiantId
              and d.statut = com.smartpresence.constants.StatutDemandeEnrolement.EN_ATTENTE
              and d.expireLe > :maintenant
            order by d.createdAt desc
            """)
    List<DemandeEnrolement> ouvertesDe(@Param("etudiantId") UUID etudiantId,
                                       @Param("maintenant") Instant maintenant);

    /** Dernière demande d'un étudiant, quel qu'en soit le sort — pour lui en rendre compte. */
    Optional<DemandeEnrolement> findFirstByEtudiantIdOrderByCreatedAtDesc(UUID etudiantId);

    /** Demande ouverte portant ce code, telle que la présente un lecteur. */
    @Query("""
            select d from DemandeEnrolement d
            where d.code = :code
              and d.statut = com.smartpresence.constants.StatutDemandeEnrolement.EN_ATTENTE
              and d.expireLe > :maintenant
            """)
    List<DemandeEnrolement> ouvertesParCode(@Param("code") String code,
                                            @Param("maintenant") Instant maintenant);

    /**
     * File d'attente des demandes à servir, la plus ancienne d'abord.
     *
     * <p>Rendue en liste plutôt qu'en {@code Optional} : le service écarte ensuite
     * celles qu'un autre lecteur vient d'annoncer à son écran, ce qu'une requête
     * ne saurait exprimer sans figer ici la durée de cette réservation.</p>
     */
    @Query("""
            select d from DemandeEnrolement d
            where d.statut = com.smartpresence.constants.StatutDemandeEnrolement.EN_ATTENTE
              and d.expireLe > :maintenant
            order by d.createdAt asc
            """)
    List<DemandeEnrolement> fileDAttente(@Param("maintenant") Instant maintenant);

    boolean existsByCodeAndStatut(String code, StatutDemandeEnrolement statut);

    /** Demandes périmées restées en attente, à clore. */
    @Query("""
            select d from DemandeEnrolement d
            where d.statut = com.smartpresence.constants.StatutDemandeEnrolement.EN_ATTENTE
              and d.expireLe <= :maintenant
            """)
    List<DemandeEnrolement> perimees(@Param("maintenant") Instant maintenant);
}
