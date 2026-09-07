package com.smartpresence.repository;

import com.smartpresence.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données des comptes utilisateurs (authentification JWT).
 *
 * @since 0.0.1
 * @see Utilisateur
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    /**
     * Recherche un utilisateur par son email (identifiant de connexion).
     * <p>Les rôles ({@code Set<Role>}) ne sont pas chargés ici (LAZY) ; ils seront
     * récupérés explicitement lors de l'autorisation Spring Security.</p>
     *
     * @param email email de connexion
     * @return l'utilisateur trouvé, ou vide si inexistant
     */
    Optional<Utilisateur> findByEmail(String email);

    /**
     * Indique si un compte existe pour l'email donné.
     *
     * @param email email à vérifier
     * @return {@code true} si l'email est déjà utilisé
     */
    boolean existsByEmail(String email);

}
