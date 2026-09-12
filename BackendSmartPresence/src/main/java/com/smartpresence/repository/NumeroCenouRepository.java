package com.smartpresence.repository;

import com.smartpresence.entity.NumeroCenou;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès au référentiel des numéros CENOU autorisés à s'inscrire.
 */
@Repository
public interface NumeroCenouRepository extends JpaRepository<NumeroCenou, Long> {

    /** Recherche par numéro : c'est le contrôle exercé à chaque inscription. */
    Optional<NumeroCenou> findByNumero(String numero);

    boolean existsByNumero(String numero);

    /** Filtre d'écran : numéros encore disponibles, ou déjà consommés. */
    List<NumeroCenou> findByUtilise(boolean utilise);

    long countByUtilise(boolean utilise);
}
