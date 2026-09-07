package com.smartpresence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Enrôlement biométrique d'un membre du personnel.
 *
 * <p>Associe l'agent à la <b>référence logique</b> de l'empreinte qui vient d'être
 * capturée sur le lecteur. Le gabarit d'empreinte reste dans la mémoire interne du
 * capteur AS608 : le backend n'en reçoit ni n'en stocke aucune trace
 * (cf. {@code SmartPresence_CONTEXT.md} §13).</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrolementBiometriqueRequest {

    /**
     * Référence logique de correspondance produite lors de l'enrôlement
     * (par exemple {@code PER-0042} pour le slot 42 du capteur).
     */
    @NotBlank(message = "Le biometricId est obligatoire")
    @Size(max = 100, message = "Le biometricId ne peut pas dépasser 100 caractères")
    private String biometricId;

    /** Lecteur sur lequel l'empreinte a été enrôlée, à des fins de traçabilité. */
    private UUID deviceId;
}
