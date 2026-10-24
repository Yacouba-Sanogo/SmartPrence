package com.smartpresence.repository;

import com.smartpresence.entity.Emprunt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpruntRepository extends JpaRepository<Emprunt, UUID> {

    @Query("SELECT e FROM Emprunt e JOIN FETCH e.livre JOIN FETCH e.etudiant ORDER BY e.emprunteLe DESC")
    List<Emprunt> findTous();

    @Query("SELECT e FROM Emprunt e JOIN FETCH e.livre JOIN FETCH e.etudiant "
            + "WHERE e.etudiant.id = :etudiantId ORDER BY e.emprunteLe DESC")
    List<Emprunt> findDeLEtudiant(@Param("etudiantId") UUID etudiantId);

    @Query("SELECT e FROM Emprunt e JOIN FETCH e.livre JOIN FETCH e.etudiant WHERE e.retourneLe IS NULL")
    List<Emprunt> findOuverts();

    @Query("SELECT e FROM Emprunt e JOIN FETCH e.livre JOIN FETCH e.etudiant WHERE e.id = :id")
    Optional<Emprunt> findAvecContexte(@Param("id") UUID id);

    long countByLivreIdAndRetourneLeIsNull(UUID livreId);

    long countByEtudiantIdAndRetourneLeIsNull(UUID etudiantId);

    boolean existsByLivreId(UUID livreId);
}
