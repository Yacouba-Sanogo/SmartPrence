package com.smartpresence.repository;

import com.smartpresence.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès aux données des rôles applicatifs (RBAC).
 *
 * @since 0.0.1
 * @see Role
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Recherche un rôle par son code métier unique.
     *
     * @param code code du rôle (ex. {@code ADMIN})
     * @return le rôle trouvé, ou vide si inexistant
     */
    Optional<Role> findByCode(String code);

    /**
     * Indique si un rôle existe pour le code donné.
     *
     * @param code code du rôle
     * @return {@code true} si un rôle porte ce code
     */
    boolean existsByCode(String code);

}
