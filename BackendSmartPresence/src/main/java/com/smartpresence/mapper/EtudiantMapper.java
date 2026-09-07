package com.smartpresence.mapper;

import com.smartpresence.dto.request.EtudiantRequest;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.entity.Etudiant;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Etudiant}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EtudiantMapper {

    @Mapping(target = "classeId", source = "classe.id")
    @Mapping(target = "classeCode", source = "classe.code")
    @Mapping(target = "classeLibelle", source = "classe.libelle")
    @Mapping(target = "promotionLibelle", source = "classe.promotion.libelle")
    @Mapping(target = "utilisateurId", source = "utilisateur.id")
    EtudiantResponse toResponse(Etudiant entity);

    List<EtudiantResponse> toResponseList(List<Etudiant> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classe", ignore = true)
    @Mapping(target = "utilisateur", ignore = true)
    @Mapping(target = "actif", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Etudiant toEntity(EtudiantRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classe", ignore = true)
    @Mapping(target = "utilisateur", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(EtudiantRequest request, @MappingTarget Etudiant entity);
}
