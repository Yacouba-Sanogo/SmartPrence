package com.smartpresence.mapper;

import com.smartpresence.dto.response.JustificationAbsenceResponse;
import com.smartpresence.entity.JustificationAbsence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper MapStruct pour l'entité {@link JustificationAbsence}.
 *
 * @since 0.0.1
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JustificationAbsenceMapper {

    @Mapping(target = "etudiantId", source = "etudiant.id")
    @Mapping(target = "etudiantNom", source = "etudiant.nom")
    @Mapping(target = "etudiantPrenom", source = "etudiant.prenom")
    @Mapping(target = "seanceId", source = "seance.id")
    @Mapping(target = "traiteParId", source = "traitePar.id")
    JustificationAbsenceResponse toResponse(JustificationAbsence entity);

    List<JustificationAbsenceResponse> toResponseList(List<JustificationAbsence> entities);
}
