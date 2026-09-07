package com.smartpresence.mapper;

import com.smartpresence.dto.request.DeviceRequest;
import com.smartpresence.dto.response.DeviceResponse;
import com.smartpresence.entity.Device;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Device}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeviceMapper {

    @Mapping(target = "salleId", source = "salle.id")
    @Mapping(target = "salleNom", source = "salle.libelle")
    @Mapping(target = "salleCode", source = "salle.code")
    DeviceResponse toResponse(Device entity);

    List<DeviceResponse> toResponseList(List<Device> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apiKeyHash", ignore = true)
    @Mapping(target = "salle", ignore = true)
    @Mapping(target = "derniereConnexion", ignore = true)
    @Mapping(target = "derniereSynchronisation", ignore = true)
    @Mapping(target = "presences", ignore = true)
    @Mapping(target = "historiques", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usage", source = "usage", defaultValue = "MIXTE")
    Device toEntity(DeviceRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apiKeyHash", ignore = true)
    @Mapping(target = "salle", ignore = true)
    @Mapping(target = "derniereConnexion", ignore = true)
    @Mapping(target = "derniereSynchronisation", ignore = true)
    @Mapping(target = "presences", ignore = true)
    @Mapping(target = "historiques", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DeviceRequest request, @MappingTarget Device entity);
}
