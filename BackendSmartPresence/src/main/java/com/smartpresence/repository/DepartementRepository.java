package com.smartpresence.repository;

import com.smartpresence.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {

    boolean existsByCode(String code);

    Optional<Departement> findByCode(String code);

    List<Departement> findAllByOrderByLibelleAsc();
}
