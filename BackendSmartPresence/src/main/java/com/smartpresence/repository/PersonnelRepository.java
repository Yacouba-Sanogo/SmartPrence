package com.smartpresence.repository;

import com.smartpresence.constants.TypePersonnel;
import com.smartpresence.entity.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données du référentiel du personnel.
 *
 * @since 0.0.1
 * @see Personnel
 */
@Repository
public interface PersonnelRepository extends JpaRepository<Personnel, UUID> {

    /** Retrouve l'agent rattaché à un compte de connexion. */
    Optional<Personnel> findByUtilisateurId(UUID utilisateurId);


    boolean existsByMatricule(String matricule);

    /**
     * Résout l'agent correspondant à une référence biométrique.
     *
     * <p>Point d'entrée de l'identification : le lecteur ESP32 transmet le
     * {@code biometricId} issu du slot reconnu, le backend retrouve l'agent.</p>
     *
     * @param biometricId référence logique de correspondance
     * @return l'agent enrôlé sous cette référence, s'il existe
     */
    Optional<Personnel> findByBiometricId(String biometricId);

    /** Vérifie qu'une référence biométrique n'est pas déjà attribuée à un autre agent. */
    boolean existsByBiometricId(String biometricId);

    /** Agents actifs, triés pour l'affichage. */
    List<Personnel> findByActifTrueOrderByNomAscPrenomAsc();

    /** Agents d'une catégorie donnée. */
    List<Personnel> findByTypeOrderByNomAscPrenomAsc(TypePersonnel type);

    long countByActifTrue();
}
