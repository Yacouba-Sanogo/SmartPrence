package com.smartpresence.repository;

import com.smartpresence.constants.StatutReclamation;
import com.smartpresence.entity.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, UUID> {

    @Query("""
            SELECT r FROM Reclamation r
            JOIN FETCH r.note n JOIN FETCH n.matiere
            JOIN FETCH r.etudiant JOIN FETCH r.enseignant
            WHERE r.enseignant.id = :enseignantId
            ORDER BY CASE WHEN r.statut = com.smartpresence.constants.StatutReclamation.OUVERTE THEN 0 ELSE 1 END,
                     r.createdAt DESC
            """)
    List<Reclamation> findParEnseignant(@Param("enseignantId") UUID enseignantId);

    @Query("""
            SELECT r FROM Reclamation r
            JOIN FETCH r.note n JOIN FETCH n.matiere
            JOIN FETCH r.etudiant JOIN FETCH r.enseignant
            WHERE r.etudiant.id = :etudiantId
            ORDER BY r.createdAt DESC
            """)
    List<Reclamation> findParEtudiant(@Param("etudiantId") UUID etudiantId);

    @Query("""
            SELECT r FROM Reclamation r
            JOIN FETCH r.note n JOIN FETCH n.matiere
            JOIN FETCH r.etudiant JOIN FETCH r.enseignant
            WHERE r.id = :id
            """)
    Optional<Reclamation> findAvecContexte(@Param("id") UUID id);

    boolean existsByNoteIdAndStatut(UUID noteId, StatutReclamation statut);

    long countByEnseignantIdAndStatut(UUID enseignantId, StatutReclamation statut);
}
