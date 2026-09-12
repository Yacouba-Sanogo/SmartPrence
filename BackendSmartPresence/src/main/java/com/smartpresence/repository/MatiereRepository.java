package com.smartpresence.repository;

import com.smartpresence.entity.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {

    boolean existsByCode(String code);

    /** ECUE d'un ensemble d'UE — le détail d'un relevé de semestre. */
    List<Matiere> findByUniteEnseignementIdInOrderByCodeAsc(List<Long> uniteIds);

    /** ECUE d'une UE, pour savoir si elle peut être supprimée. */
    List<Matiere> findByUniteEnseignementId(Long uniteId);
}
