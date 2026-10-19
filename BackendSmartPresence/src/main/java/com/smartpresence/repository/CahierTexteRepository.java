package com.smartpresence.repository;

import com.smartpresence.entity.CahierTexte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CahierTexteRepository extends JpaRepository<CahierTexte, UUID> {

    @Query("""
            SELECT p FROM CahierTexte p
            JOIN FETCH p.seance s
            JOIN FETCH s.classe
            JOIN FETCH s.matiere
            LEFT JOIN FETCH p.chapitre
            WHERE s.id = :seanceId
            """)
    Optional<CahierTexte> findParSeance(@Param("seanceId") UUID seanceId);

    /** Pages d'un enseignant pour une classe, de la plus récente à la plus ancienne. */
    @Query("""
            SELECT p FROM CahierTexte p
            JOIN FETCH p.seance s
            JOIN FETCH s.classe
            JOIN FETCH s.matiere
            LEFT JOIN FETCH p.chapitre
            WHERE s.classe.id = :classeId
              AND s.enseignant.id = :enseignantId
              AND (:matiereId IS NULL OR s.matiere.id = :matiereId)
            ORDER BY s.debut DESC
            """)
    List<CahierTexte> findParClasse(@Param("classeId") Long classeId,
                                    @Param("enseignantId") UUID enseignantId,
                                    @Param("matiereId") Long matiereId);
}
