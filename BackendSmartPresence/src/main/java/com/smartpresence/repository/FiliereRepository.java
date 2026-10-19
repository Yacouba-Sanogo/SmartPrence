package com.smartpresence.repository;

import com.smartpresence.entity.Filiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FiliereRepository extends JpaRepository<Filiere, Long> {

    boolean existsByCode(String code);

    Optional<Filiere> findByCode(String code);

    List<Filiere> findAllByOrderByLibelleAsc();
}
