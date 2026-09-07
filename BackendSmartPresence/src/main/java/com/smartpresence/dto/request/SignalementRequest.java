package com.smartpresence.dto.request;

import com.smartpresence.constants.TypeSignalement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Anomalie signalée par un enseignant sur une de ses séances.
 *
 * <p>La séance n'est pas transmise : elle figure dans l'URL, et l'enseignant est déduit
 * du jeton. Un enseignant ne peut donc signaler que sur un cours qui est le sien.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementRequest {

    @NotNull(message = "Le type de signalement est obligatoire")
    private TypeSignalement type;

    /**
     * Étudiant concerné.
     *
     * <p>Obligatoire pour un {@code ETUDIANT_NON_RECONNU} — sans lui, la scolarité ne
     * saurait pas qui régulariser. Ignoré pour une panne de lecteur.</p>
     */
    private UUID etudiantId;

    /**
     * Description de l'anomalie — <b>obligatoire</b>.
     *
     * <p>C'est le témoignage sur lequel la scolarité s'appuiera : un signalement sans
     * explication ne lui donnerait rien à arbitrer.</p>
     */
    @NotBlank(message = "La description de l'anomalie est obligatoire")
    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;
}
