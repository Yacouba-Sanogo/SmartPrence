package com.smartpresence.repository;

import com.smartpresence.entity.SupportCours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportCoursRepository extends JpaRepository<SupportCours, UUID> {

    @Query("""
            SELECT s FROM SupportCours s
            JOIN FETCH s.classe JOIN FETCH s.matiere JOIN FETCH s.enseignant
            WHERE s.classe.id = :classeId
              AND (:enseignantId IS NULL OR s.enseignant.id = :enseignantId)
            ORDER BY s.createdAt DESC
            """)
    List<SupportCours> findParClasse(@Param("classeId") Long classeId,
                                     @Param("enseignantId") UUID enseignantId);

    @Query("""
            SELECT s FROM SupportCours s
            JOIN FETCH s.classe JOIN FETCH s.matiere JOIN FETCH s.enseignant
            WHERE s.id = :id
            """)
    Optional<SupportCours> findAvecContexte(@Param("id") UUID id);
}
