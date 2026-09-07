package com.smartpresence.mapper;

import com.smartpresence.dto.response.PersonnelResponse;
import com.smartpresence.entity.Personnel;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;

/**
 * Mapper MapStruct pour l'entité {@link Personnel}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PersonnelMapper {

    PersonnelResponse toResponse(Personnel entity);

    List<PersonnelResponse> toResponseList(List<Personnel> entities);

    /** Variante ensembliste, utilisée pour les enseignants d'une classe. */
    Set<PersonnelResponse> toResponseSet(Set<Personnel> entities);
}
