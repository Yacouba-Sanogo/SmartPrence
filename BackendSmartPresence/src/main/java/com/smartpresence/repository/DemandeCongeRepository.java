package com.smartpresence.repository;

import com.smartpresence.constants.StatutDemande;
import com.smartpresence.entity.DemandeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, UUID> {

    List<DemandeConge> findByEnseignantIdOrderByDebutDesc(UUID enseignantId);

    @Query("""
            SELECT d FROM DemandeConge d
            JOIN FETCH d.enseignant
            WHERE (:statut IS NULL OR d.statut = :statut)
            ORDER BY d.debut ASC
            """)
    List<DemandeConge> findParStatut(@Param("statut") StatutDemande statut);

    /** Demandes en attente ou acceptées d'un enseignant qui recouvrent la période. */
    @Query("""
            SELECT d FROM DemandeConge d
            WHERE d.enseignant.id = :enseignantId
              AND d.statut IN (com.smartpresence.constants.StatutDemande.EN_ATTENTE,
                               com.smartpresence.constants.StatutDemande.ACCEPTEE)
              AND d.debut <= :fin AND d.fin >= :debut
            """)
    List<DemandeConge> findRecouvrant(@Param("enseignantId") UUID enseignantId,
                                      @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    long countByStatut(StatutDemande statut);
}
