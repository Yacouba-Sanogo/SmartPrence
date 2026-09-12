package com.smartpresence.mapper;

import com.smartpresence.dto.response.NoteResponse;
import com.smartpresence.entity.Note;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

/**
 * Conversion des notes vers leur représentation d'API.
 *
 * <p>Extrait du service : le bulletin classique et le relevé LMD lisent les mêmes
 * notes, et en dupliquer la conversion garantissait qu'un champ ajouté d'un côté
 * manquerait de l'autre.</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteMapper {

    @Mapping(target = "etudiantId", source = "etudiant.id")
    @Mapping(target = "etudiantNom", source = "etudiant.nom")
    @Mapping(target = "etudiantPrenom", source = "etudiant.prenom")
    @Mapping(target = "etudiantMatricule", source = "etudiant.matricule")
    @Mapping(target = "matiereId", source = "matiere.id")
    @Mapping(target = "matiereCode", source = "matiere.code")
    @Mapping(target = "matiereLibelle", source = "matiere.libelle")
    @Mapping(target = "enseignantId", source = "enseignant.id")
    @Mapping(target = "enseignantNom", source = "enseignant", qualifiedByName = "nomComplet")
    NoteResponse toResponse(Note note);

    List<NoteResponse> toResponseList(List<Note> notes);

    /** « Salif KEITA » — l'ordre d'usage dans les bulletins de l'établissement. */
    @Named("nomComplet")
    default String nomComplet(com.smartpresence.entity.Personnel personnel) {
        if (personnel == null) {
            return null;
        }
        return (personnel.getPrenom() + " " + personnel.getNom()).trim();
    }
}
