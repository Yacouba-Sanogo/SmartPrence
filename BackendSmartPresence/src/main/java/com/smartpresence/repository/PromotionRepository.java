package com.smartpresence.repository;

import com.smartpresence.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès aux données des promotions universitaires.
 *
 * @since 0.0.1
 * @see Promotion
 */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /**
     * Recherche une promotion par son code.
     *
     * @param code code métier (ex. {@code L3-INFO-2026})
     * @return la promotion trouvée, ou vide si inexistante
     */
    Optional<Promotion> findByCode(String code);

    /**
     * Indique si une promotion existe pour le code donné.
     *
     * @param code code à vérifier
     * @return {@code true} si déjà utilisé
     */
    boolean existsByCode(String code);

}
