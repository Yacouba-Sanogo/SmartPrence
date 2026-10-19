package com.smartpresence.repository;

import com.smartpresence.entity.AbsenceEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbsenceEvaluationRepository extends JpaRepository<AbsenceEvaluation, UUID> {

    List<AbsenceEvaluation> findByEvaluationId(UUID evaluationId);

    Optional<AbsenceEvaluation> findByEvaluationIdAndEtudiantId(UUID evaluationId, UUID etudiantId);

    long countByEvaluationId(UUID evaluationId);
}
