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
    ClasseResponse toResponse(Classe entity);

    List<ClasseResponse> toResponseList(List<Classe> entities);

    ClasseDetailResponse toDetailResponse(Classe entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "etudiants", ignore = true)
    @Mapping(target = "enseignants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Classe toEntity(ClasseRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "etudiants", ignore = true)
    @Mapping(target = "enseignants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ClasseRequest request, @MappingTarget Classe entity);
}
