package com.smartpresence.repository;

import com.smartpresence.entity.DocumentContenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentContenuRepository extends JpaRepository<DocumentContenu, UUID> { }
