package com.smartpresence.repository;

import com.smartpresence.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    @Query("""
            SELECT e FROM Evaluation e
            JOIN FETCH e.classe
            JOIN FETCH e.matiere
            WHERE e.classe.id = :classeId
              AND e.enseignant.id = :enseignantId
              AND (:matiereId IS NULL OR e.matiere.id = :matiereId)
            ORDER BY e.dateEvaluation DESC
            """)
    List<Evaluation> findParClasse(@Param("classeId") Long classeId,
                                   @Param("enseignantId") UUID enseignantId,
                                   @Param("matiereId") Long matiereId);

    @Query("""
            SELECT e FROM Evaluation e
            JOIN FETCH e.classe
            JOIN FETCH e.matiere
            JOIN FETCH e.enseignant
            WHERE e.id = :id
            """)
    Optional<Evaluation> findAvecContexte(@Param("id") UUID id);
}
