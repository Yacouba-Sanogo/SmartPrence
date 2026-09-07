package com.smartpresence.mapper;

import com.smartpresence.dto.response.ParametreEtablissementResponse;
import com.smartpresence.entity.ParametreEtablissement;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link ParametreEtablissement}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ParametreEtablissementMapper {

    ParametreEtablissementResponse toResponse(ParametreEtablissement entity);

    List<ParametreEtablissementResponse> toResponseList(List<ParametreEtablissement> entities);
}
