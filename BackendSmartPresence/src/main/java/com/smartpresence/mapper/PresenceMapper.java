package com.smartpresence.mapper;

import com.smartpresence.dto.response.PresenceResponse;
import com.smartpresence.entity.Presence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link Presence}.
 *
 * <p>Calcule la latence de synchronisation en millisecondes
 * ({@code synchroniseLe - creeLeDevice}).</p>
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PresenceMapper {

    @Mapping(target = "etudiantId", source = "etudiant.id")
    @Mapping(target = "etudiantMatricule", source = "etudiant.matricule")
    @Mapping(target = "etudiantNom", source = "etudiant.nom")
    @Mapping(target = "etudiantPrenom", source = "etudiant.prenom")
    @Mapping(target = "classeCode", source = "etudiant.classe.code")
    @Mapping(target = "deviceId", source = "device.id")
    @Mapping(target = "deviceNom", source = "device.nom")
    @Mapping(target = "salleNom", source = "device.salle.libelle")
    @Mapping(target = "seanceId", source = "seance.id")
    @Mapping(target = "matiereLibelle", source = "seance.matiere.libelle")
    @Mapping(target = "latenceSynchronisationMs", expression = "java(calculateLatenceMs(entity))")
    PresenceResponse  toResponse(Presence entity);

    List<PresenceResponse> toResponseList(List<Presence> entities);

    default Long calculateLatenceMs(Presence entity) {
        if (entity.getSynchroniseLe() != null && entity.getCreeLeDevice() != null) {
            return entity.getSynchroniseLe().toEpochMilli() - entity.getCreeLeDevice().toEpochMilli();
        }
        return null;
    }
}
