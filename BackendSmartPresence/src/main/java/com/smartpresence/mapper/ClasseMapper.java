package com.smartpresence.mapper;

import com.smartpresence.dto.request.ClasseRequest;
import com.smartpresence.dto.response.ClasseDetailResponse;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.entity.Classe;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Classe}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {PromotionMapper.class, PersonnelMapper.class, EtudiantMapper.class})
public interface ClasseMapper {

    // Les deux compteurs touchent des collections paresseuses : ils exigent une
    // transaction ouverte, et coûtent une requête par classe. Acceptable sur le
    // volume d'un établissement, à revoir si la liste devait être paginée.
    @Mapping(target = "nombreEtudiants", expression = "java(entity.getEtudiants() != null ? entity.getEtudiants().size() : 0)")
    @Mapping(target = "nombreEnseignants", expression = "java(entity.getEnseignants() != null ? entity.getEnseignants().size() : 0)")
    @Mapping(target = "filiereId", source = "filiere.id")
    @Mapping(target = "filiereLibelle", source = "filiere.libelle")
    @Mapping(target = "optionId", source = "option.id")
    @Mapping(target = "optionLibelle", source = "option.libelle")
    @Mapping(target = "classePrecedenteId", source = "classePrecedente.id")
    @Mapping(target = "classePrecedenteCode", source = "classePrecedente.code")
    @Mapping(target = "responsableId", expression = "java(idResponsable(entity))")
    @Mapping(target = "responsableNom", expression = "java(nomResponsable(entity))")
    ClasseResponse toResponse(Classe entity);

    List<ClasseResponse> toResponseList(List<Classe> entities);

    @Mapping(target = "responsableId", expression = "java(idResponsable(entity))")
    @Mapping(target = "responsableNom", expression = "java(nomResponsable(entity))")
    ClasseDetailResponse toDetailResponse(Classe entity);

    /**
     * Le responsable, seulement s'il est toujours étudiant de la classe : une désignation que
     * l'étudiant a quittée en changeant de classe ne doit jamais s'afficher.
     */
    default com.smartpresence.entity.Etudiant responsableValide(Classe classe) {
        com.smartpresence.entity.Etudiant r = classe.getResponsable();
        return r != null && r.getClasse() != null && r.getClasse().getId().equals(classe.getId()) ? r : null;
    }

    default java.util.UUID idResponsable(Classe classe) {
        com.smartpresence.entity.Etudiant r = responsableValide(classe);
        return r == null ? null : r.getId();
    }

    default String nomResponsable(Classe classe) {
        com.smartpresence.entity.Etudiant r = responsableValide(classe);
        return r == null ? null : (r.getPrenom() + " " + r.getNom()).trim();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "filiere", ignore = true)
    @Mapping(target = "option", ignore = true)
    @Mapping(target = "classePrecedente", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "etudiants", ignore = true)
    @Mapping(target = "enseignants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Classe toEntity(ClasseRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "filiere", ignore = true)
    @Mapping(target = "option", ignore = true)
    @Mapping(target = "classePrecedente", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "etudiants", ignore = true)
    @Mapping(target = "enseignants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ClasseRequest request, @MappingTarget Classe entity);
}
