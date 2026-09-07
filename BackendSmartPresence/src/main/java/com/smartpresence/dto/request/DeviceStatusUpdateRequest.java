package com.smartpresence.dto.request;

import com.smartpresence.constants.DeviceStatut;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Requête de mise à jour du statut d'un appareil ESP32.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusUpdateRequest {

    @NotNull(message = "Le statut est obligatoire")
    private DeviceStatut statut;
}
