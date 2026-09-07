package com.smartpresence.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Lot de pointages de personnel transmis par un lecteur ESP32 d'entrée.
 *
 * <p>L'envoi est groupé et différé : l'appareil accumule les pointages en NVS et vide sa
 * file dès que la connectivité le permet (cf. {@code SmartPresence_CONTEXT.md} §8.4).</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointageESP32Request {

    @NotNull(message = "Le deviceId émetteur est obligatoire")
    private UUID deviceId;

    @NotEmpty(message = "La liste des pointages ne peut pas être vide")
    @Valid
    private List<PointageItemRequest> pointages;

    /** Nombre de tentatives d'envoi côté appareil — mesure de fiabilité réseau. */
    private Integer nombreTentatives;
}
