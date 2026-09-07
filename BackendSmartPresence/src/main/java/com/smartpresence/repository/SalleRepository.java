package com.smartpresence.repository;

import com.smartpresence.entity.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès aux données des salles physiques.
 *
 * @since 0.0.1
 * @see Salle
 */
@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {

    /**
     * Recherche une salle par son code.
     *
     * @param code code métier (ex. {@code B12-101})
     * @return la salle trouvée, ou vide si inexistante
     */
    Optional<Salle> findByCode(String code);

    /**
     * Indique si une salle existe pour le code donné.
     *
     * @param code code à vérifier
     * @return {@code true} si déjà utilisé
     */
    boolean existsByCode(String code);

}
