package com.smartpresence.dto.response;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.constants.UsageDevice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Données d'un appareil ESP32 exposées via l'API REST.
 *
 * <p>La clé d'API hachée n'est <b>jamais</b> transmise aux clients.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {

    private UUID id;
    private String nom;
    private String adresseMac;
    private DeviceStatut statut;
    private UsageDevice usage;
    private String versionFirmware;
    private Instant derniereConnexion;
    private Instant derniereSynchronisation;
    private Long salleId;
    private String salleNom;
    private String salleCode;
    private Instant createdAt;
    private Instant updatedAt;
}
