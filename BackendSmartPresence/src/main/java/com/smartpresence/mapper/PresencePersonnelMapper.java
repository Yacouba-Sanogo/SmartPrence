package com.smartpresence.mapper;

import com.smartpresence.dto.response.PointagePersonnelResponse;
import com.smartpresence.entity.PresencePersonnel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link PresencePersonnel}.
 *
 * <p>Calcule la latence de synchronisation en millisecondes
 * ({@code synchroniseLe - creeLeDevice}), de la même façon que {@link PresenceMapper},
 * afin que les deux flux restent comparables dans l'évaluation du mémoire.</p>
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PresencePersonnelMapper {

    @Mapping(target = "personnelId", source = "personnel.id")
    @Mapping(target = "personnelMatricule", source = "personnel.matricule")
    @Mapping(target = "personnelNom", source = "personnel.nom")
    @Mapping(target = "personnelPrenom", source = "personnel.prenom")
    @Mapping(target = "personnelType", source = "personnel.type")
    @Mapping(target = "personnelService", source = "personnel.service")
    @Mapping(target = "deviceId", source = "device.id")
    @Mapping(target = "deviceNom", source = "device.nom")
    @Mapping(target = "latenceSynchronisationMs", expression = "java(calculateLatenceMs(entity))")
    PointagePersonnelResponse toResponse(PresencePersonnel entity);

    List<PointagePersonnelResponse> toResponseList(List<PresencePersonnel> entities);

    default Long calculateLatenceMs(PresencePersonnel entity) {
        if (entity.getSynchroniseLe() != null && entity.getCreeLeDevice() != null) {
            return entity.getSynchroniseLe().toEpochMilli() - entity.getCreeLeDevice().toEpochMilli();
        }
        return null;
    }
}
