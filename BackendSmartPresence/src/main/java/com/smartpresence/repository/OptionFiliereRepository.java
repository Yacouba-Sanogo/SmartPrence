package com.smartpresence.repository;

import com.smartpresence.entity.OptionFiliere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionFiliereRepository extends JpaRepository<OptionFiliere, Long> {

    boolean existsByCode(String code);
}
