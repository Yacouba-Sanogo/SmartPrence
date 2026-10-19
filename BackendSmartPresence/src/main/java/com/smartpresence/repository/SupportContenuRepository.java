package com.smartpresence.repository;

import com.smartpresence.entity.SupportContenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportContenuRepository extends JpaRepository<SupportContenu, UUID> { }
