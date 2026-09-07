package com.smartpresence.mapper;

import com.smartpresence.dto.request.PromotionRequest;
import com.smartpresence.dto.response.PromotionResponse;
import com.smartpresence.entity.Promotion;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Promotion}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PromotionMapper {

    @Mapping(target = "anneeAcademique", expression = "java(parseAnneeAcademique(entity.getAnneeUniversitaire()))")
    PromotionResponse toResponse(Promotion entity);

    List<PromotionResponse> toResponseList(List<Promotion> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "filiere", source = "libelle")
    @Mapping(target = "niveau", constant = "Non spécifié")
    @Mapping(target = "anneeUniversitaire", expression = "java(String.valueOf(request.getAnneeAcademique()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Promotion toEntity(PromotionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "filiere", ignore = true)
    @Mapping(target = "niveau", ignore = true)
    @Mapping(target = "anneeUniversitaire", expression = "java(request.getAnneeAcademique() != null ? String.valueOf(request.getAnneeAcademique()) : entity.getAnneeUniversitaire())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(PromotionRequest request, @MappingTarget Promotion entity);

    default Integer parseAnneeAcademique(String anneeUniversitaire) {
        if (anneeUniversitaire == null || anneeUniversitaire.isBlank()) {
            return null;
        }
        try {
            if (anneeUniversitaire.contains("-")) {
                return Integer.parseInt(anneeUniversitaire.substring(0, anneeUniversitaire.indexOf('-')));
            }
            return Integer.parseInt(anneeUniversitaire.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
