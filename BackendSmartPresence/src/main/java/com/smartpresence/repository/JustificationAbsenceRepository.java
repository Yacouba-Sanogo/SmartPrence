package com.smartpresence.repository; import com.smartpresence.entity.JustificationAbsence; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface JustificationAbsenceRepository extends JpaRepository<JustificationAbsence,UUID>{List<JustificationAbsence> findByEtudiantId(UUID etudiantId);}
