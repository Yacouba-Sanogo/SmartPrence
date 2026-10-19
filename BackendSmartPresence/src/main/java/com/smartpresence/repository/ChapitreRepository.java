package com.smartpresence.repository;

import com.smartpresence.entity.Chapitre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapitreRepository extends JpaRepository<Chapitre, UUID> {

    /** Syllabus d'un enseignant pour une classe, filtrable par matière. */
    @Query("""
            SELECT c FROM Chapitre c
            JOIN FETCH c.matiere
            WHERE c.classe.id = :classeId
              AND c.enseignant.id = :enseignantId
              AND (:matiereId IS NULL OR c.matiere.id = :matiereId)
            ORDER BY c.matiere.libelle ASC, c.ordre ASC
            """)
    List<Chapitre> findSyllabus(@Param("classeId") Long classeId,
                                @Param("enseignantId") UUID enseignantId,
                                @Param("matiereId") Long matiereId);

    @Query("""
            SELECT c FROM Chapitre c
            JOIN FETCH c.matiere
            JOIN FETCH c.classe
            JOIN FETCH c.enseignant
            WHERE c.id = :id
            """)
    Optional<Chapitre> findAvecContexte(@Param("id") UUID id);

    @Query("""
            SELECT COALESCE(MAX(c.ordre), 0) FROM Chapitre c
            WHERE c.classe.id = :classeId AND c.matiere.id = :matiereId
              AND c.enseignant.id = :enseignantId
            """)
    int dernierOrdre(@Param("classeId") Long classeId, @Param("matiereId") Long matiereId,
                     @Param("enseignantId") UUID enseignantId);
}
