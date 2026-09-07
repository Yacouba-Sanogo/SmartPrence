package com.smartpresence.dto.request;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.constants.UsageDevice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Requête de création ou mise à jour d'un appareil ESP32.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequest {

    @NotBlank(message = "Le nom de l'appareil est obligatoire")
    private String nom;

    @NotBlank(message = "L'adresse MAC est obligatoire")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", message = "Format d'adresse MAC invalide (ex: AA:BB:CC:DD:EE:FF)")
    private String adresseMac;

    /**
     * Clé d'API en clair de l'appareil. Elle n'est jamais stockée telle quelle :
     * seule son empreinte est conservée en base.
     *
     * <p>La longueur minimale n'est pas cosmétique. L'empreinte étant déterministe et
     * rapide à calculer, la robustesse repose entièrement sur l'<b>entropie de la
     * clé</b> : elle doit être tirée au hasard, jamais dérivée du nom du lecteur ou
     * de la salle.</p>
     */
    @NotBlank(message = "La clé d'API brute est obligatoire pour l'enregistrement/mise à jour")
    @Size(min = 16, message = "La clé d'API doit contenir au moins 16 caractères aléatoires")
    private String apiKey;

    private DeviceStatut statut;
    private String versionFirmware;
    private Long salleId;

    /**
     * Vocation de l'appareil : {@code ETUDIANT} (lecteur de salle),
     * {@code PERSONNEL} (lecteur d'entrée) ou {@code MIXTE}.
     * <p>Absent, l'appareil est enregistré comme {@code MIXTE}.</p>
     */
    private UsageDevice usage;
}
