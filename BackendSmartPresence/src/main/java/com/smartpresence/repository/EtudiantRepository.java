package com.smartpresence.repository;

import com.smartpresence.entity.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données des étudiants (référentiel central).
 *
 * <p><b>Note de confidentialité</b> : le champ {@code biometricId} est une référence
 * logique de correspondance, jamais une empreinte. Les requêtes portant sur ce champ
 * servent uniquement à retrouver l'étudiant identifié par l'ESP32.</p>
 *
 * @since 0.0.1
 * @see Etudiant
 */
@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, UUID> {

    /**
     * Retrouve l'étudiant rattaché à un compte de connexion.
     *
     * <p>Point d'entrée des endpoints cadrés sur le porteur du jeton : le mobile ne
     * transmet jamais d'identifiant d'étudiant, il est déduit de l'authentification.</p>
     */
    Optional<Etudiant> findByUtilisateurId(UUID utilisateurId);


    /**
     * Recherche un étudiant par son matricule.
     *
     * @param matricule matricule métier
     * @return l'étudiant trouvé, ou vide si inexistant
     */
    Optional<Etudiant> findByMatricule(String matricule);

    /**
     * Recherche un étudiant par sa référence logique de correspondance biométrique.
     * <p>Utilisée pour faire le lien entre l'identification ESP32 (slot AS608) et
     * l'étudiant connu du référentiel central.</p>
     *
     * @param biometricId référence logique (identifiant, jamais une empreinte)
     * @return l'étudiant trouvé, ou vide si inexistant
     */
    Optional<Etudiant> findByBiometricId(String biometricId);

    /**
     * Indique si un matricule est déjà utilisé.
     *
     * @param matricule matricule à vérifier
     * @return {@code true} si déjà utilisé
     */
    boolean existsByMatricule(String matricule);

    /**
     * Indique si une référence logique biométrique est déjà attribuée.
     *
     * @param biometricId référence logique à vérifier
     * @return {@code true} si déjà attribuée
     */
    boolean existsByBiometricId(String biometricId);

    long countByClasseId(Long classeId);

    List<Etudiant> findByClasseId(Long classeId);

}
