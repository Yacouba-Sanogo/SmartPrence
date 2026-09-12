package com.smartpresence.mapper;

import com.smartpresence.dto.response.NumeroCenouResponse;
import com.smartpresence.entity.NumeroCenou;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Conversion des numéros CENOU vers leur représentation d'API.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NumeroCenouMapper {

    @Mapping(target = "classeId", source = "classe.id")
    @Mapping(target = "classeCode", source = "classe.code")
    NumeroCenouResponse toResponse(NumeroCenou entity);

    List<NumeroCenouResponse> toResponseList(List<NumeroCenou> entities);
}
