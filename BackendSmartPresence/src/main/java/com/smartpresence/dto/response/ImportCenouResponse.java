package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Compte rendu d'un import de numéros CENOU.
 *
 * <p>Un import de plusieurs centaines de lignes échoue rarement en bloc : il
 * comporte presque toujours quelques doublons et quelques cellules vides. Tout
 * rejeter pour autant obligerait à corriger le fichier entier avant de pouvoir
 * recommencer. Les lignes valides sont donc enregistrées, et ce compte rendu dit
 * précisément ce qui a été écarté, avec le numéro de ligne du tableur.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCenouResponse {

    /** Nombre de lignes de données lues, en-tête exclu. */
    private int lignesLues;

    /** Numéros effectivement ajoutés au référentiel. */
    private int ajoutes;

    /** Numéros déjà présents, laissés intacts. */
    private int doublons;

    /** Lignes écartées, décrites une à une (« ligne 12 : numéro vide »). */
    private List<String> rejets;
}
