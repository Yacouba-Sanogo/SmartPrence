package com.smartpresence.repository;

import com.smartpresence.constants.SourcePresence;
import com.smartpresence.constants.StatutPresence;
import com.smartpresence.dto.request.PresenceSearchCriteria;
import com.smartpresence.entity.Presence;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Spécifications JPA pour la recherche filtrée des présences.
 */
public final class PresenceSpecification {

    private PresenceSpecification() {
    }

    public static Specification<Presence> withCriteria(PresenceSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getEtudiantId() != null) {
                predicates.add(cb.equal(root.get("etudiant").get("id"), criteria.getEtudiantId()));
            }
            if (criteria.getClasseId() != null) {
                predicates.add(cb.equal(root.get("etudiant").get("classe").get("id"), criteria.getClasseId()));
            }
            if (criteria.getPromotionId() != null) {
                predicates.add(cb.equal(
                        root.get("etudiant").get("classe").get("promotion").get("id"),
                        criteria.getPromotionId()));
            }
            if (criteria.getDeviceId() != null) {
                predicates.add(cb.equal(root.get("device").get("id"), criteria.getDeviceId()));
            }
            if (criteria.getSalleId() != null) {
                predicates.add(cb.equal(root.get("device").get("salle").get("id"), criteria.getSalleId()));
            }
            if (criteria.getDateDebut() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("datePresence"), criteria.getDateDebut()));
            }
            if (criteria.getDateFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("datePresence"), criteria.getDateFin()));
            }
            if (criteria.getStatut() != null) {
                predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
            }
            if (criteria.getSource() != null) {
                predicates.add(cb.equal(root.get("source"), criteria.getSource()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Presence> withStatut(StatutPresence statut) {
        return (root, query, cb) -> cb.equal(root.get("statut"), statut);
    }

    public static Specification<Presence> withSource(SourcePresence source) {
        return (root, query, cb) -> cb.equal(root.get("source"), source);
    }
}
