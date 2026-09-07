package com.smartpresence.mapper;

import com.smartpresence.dto.response.HistoriqueSynchronisationResponse;
import com.smartpresence.entity.HistoriqueSynchronisation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link HistoriqueSynchronisation}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HistoriqueSynchronisationMapper {

    @Mapping(target = "deviceId", source = "device.id")
    @Mapping(target = "deviceNom", source = "device.nom")
    @Mapping(target = "nombreEvenements", source = "nbEvenements")
    HistoriqueSynchronisationResponse toResponse(HistoriqueSynchronisation entity);

    List<HistoriqueSynchronisationResponse> toResponseList(List<HistoriqueSynchronisation> entities);
}
