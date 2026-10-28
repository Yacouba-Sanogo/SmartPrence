package com.smartpresence.repository;

import com.smartpresence.constants.StatutDocument;
import com.smartpresence.entity.DocumentBibliotheque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentBibliothequeRepository extends JpaRepository<DocumentBibliotheque, UUID> {

    @Query("""
            SELECT d FROM DocumentBibliotheque d LEFT JOIN FETCH d.filiere
            WHERE d.statut = :statut ORDER BY d.createdAt DESC
            """)
    List<DocumentBibliotheque> findParStatut(@Param("statut") StatutDocument statut);

    @Query("""
            SELECT d FROM DocumentBibliotheque d LEFT JOIN FETCH d.filiere
            WHERE d.deposeParId = :compteId ORDER BY d.createdAt DESC
            """)
    List<DocumentBibliotheque> findDuDeposant(@Param("compteId") UUID compteId);

    @Query("SELECT d FROM DocumentBibliotheque d LEFT JOIN FETCH d.filiere WHERE d.id = :id")
    Optional<DocumentBibliotheque> findAvecFiliere(@Param("id") UUID id);

    long countByStatut(StatutDocument statut);
}
