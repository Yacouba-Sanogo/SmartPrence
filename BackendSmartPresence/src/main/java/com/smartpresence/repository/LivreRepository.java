package com.smartpresence.repository;

import com.smartpresence.entity.Livre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LivreRepository extends JpaRepository<Livre, UUID> {

    @Query("SELECT l FROM Livre l LEFT JOIN FETCH l.filiere ORDER BY l.titre")
    List<Livre> findTous();
}
