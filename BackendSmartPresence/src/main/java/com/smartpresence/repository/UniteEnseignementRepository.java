package com.smartpresence.repository;

import com.smartpresence.constants.PeriodeScolaire;
import com.smartpresence.entity.UniteEnseignement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux unités d'enseignement de la maquette pédagogique.
 */
@Repository
public interface UniteEnseignementRepository extends JpaRepository<UniteEnseignement, Long> {

    Optional<UniteEnseignement> findByCode(String code);

    boolean existsByCode(String code);

    List<UniteEnseignement> findByPromotionIdOrderBySemestreAscCodeAsc(Long promotionId);

    /** Maquette d'un semestre donné — la base du relevé de notes. */
    List<UniteEnseignement> findByPromotionIdAndSemestreOrderByCodeAsc(
            Long promotionId, PeriodeScolaire semestre);
}
