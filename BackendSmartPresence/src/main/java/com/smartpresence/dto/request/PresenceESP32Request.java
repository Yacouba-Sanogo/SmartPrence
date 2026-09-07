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
 * Paquet de synchronisation transmis par un appareil ESP32.
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceESP32Request {

    @NotNull(message = "Le deviceId émetteur est obligatoire")
    private UUID deviceId;

    @NotEmpty(message = "La liste des événements de présence ne peut pas être vide")
    @Valid
    private List<PresenceItemRequest> presences;

    private Integer nombreTentatives;
}
