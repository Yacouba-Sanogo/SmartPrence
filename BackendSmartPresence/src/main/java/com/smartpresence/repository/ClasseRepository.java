package com.smartpresence.repository;

import com.smartpresence.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux données des classes (groupes d'étudiants).
 *
 * @since 0.0.1
 * @see Classe
 */
@Repository
public interface ClasseRepository extends JpaRepository<Classe, Long> {

    /**
     * Retire la désignation de responsable d'un étudiant partout sauf dans sa nouvelle classe.
     *
     * <p>Appelé quand un étudiant change de classe : sans cela, un retour dans son ancienne
     * classe lui rendrait silencieusement un rôle que l'administrateur ne lui a plus donné.</p>
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query("UPDATE Classe c SET c.responsable = null WHERE c.responsable.id = :etudiantId AND c.id <> :classeId")
    int libererResponsable(@Param("etudiantId") java.util.UUID etudiantId, @Param("classeId") Long classeId);

    /**
     * Classes dans lesquelles un enseignant intervient.
     *
     * <p>La jointure passe par la table {@code classes_enseignants}, dont la cible est
     * desormais {@code personnels}.</p>
     */
    @Query("""
            SELECT c FROM Classe c
            JOIN c.enseignants e
            LEFT JOIN FETCH c.promotion
            WHERE e.id = :enseignantId
            ORDER BY c.code ASC
            """)
    List<Classe> findParEnseignant(@Param("enseignantId") java.util.UUID enseignantId);


    /**
     * Recherche une classe par son code.
     *
     * @param code code métier (ex. {@code L3-INFO-A})
     * @return la classe trouvée, ou vide si inexistante
     */
    Optional<Classe> findByCode(String code);

    /**
     * Indique si une classe existe pour le code donné.
     *
     * @param code code à vérifier
     * @return {@code true} si déjà utilisé
     */
    boolean existsByCode(String code);

    /**
     * Liste les classes appartenant à une promotion donnée.
     *
     * @param promotionId identifiant de la promotion
     * @return liste des classes de la promotion
     */
    List<Classe> findByPromotionId(Long promotionId);

    /** Classes supérieures : celles dont les étudiants viennent de la classe donnée. */
    List<Classe> findByClassePrecedenteId(Long classePrecedenteId);

}
