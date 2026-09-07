package com.smartpresence.mapper;

import com.smartpresence.dto.request.SalleRequest;
import com.smartpresence.dto.response.SalleResponse;
import com.smartpresence.entity.Salle;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Salle}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SalleMapper {

    @Mapping(target = "deviceId", source = "device.id")
    @Mapping(target = "deviceNom", source = "device.nom")
    @Mapping(target = "nom", source = "libelle")
    SalleResponse toResponse(Salle entity);

    List<SalleResponse> toResponseList(List<Salle> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "device", ignore = true)
    @Mapping(target = "libelle", source = "nom")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Salle toEntity(SalleRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "device", ignore = true)
    @Mapping(target = "libelle", source = "nom")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(SalleRequest request, @MappingTarget Salle entity);
}
