package com.smartpresence.repository;
import com.smartpresence.entity.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MatiereRepository extends JpaRepository<Matiere, Long> { boolean existsByCode(String code); }
