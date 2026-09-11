package com.smartpresence.controller;

import com.smartpresence.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Point de contrôle public, destiné à la surveillance externe.
 *
 * <p>L'hébergement gratuit endort l'instance après quinze minutes sans trafic, et son
 * réveil demande près de trois minutes — le premier pointage de la journée échouerait.
 * Un service de surveillance (UptimeRobot ou équivalent) appelle donc cette adresse à
 * intervalle régulier pour maintenir le serveur éveillé.</p>
 *
 * <p>La réponse est volontairement <b>minuscule et sans accès à la base</b> : un appel
 * toutes les cinq minutes, jour et nuit, ne doit peser ni sur la base de données ni sur
 * le quota de l'hébergeur. C'est ce qui distingue cette adresse de
 * {@code /v3/api-docs}, qui fait 80 Ko et met vingt secondes à s'initialiser.</p>
 *
 * <p>Elle n'expose aucune information sensible : ni version, ni état de la base, ni
 * configuration — seulement la preuve que l'application répond.</p>
 */
@RestController
@RequestMapping("/ping")
@Tag(name = "Surveillance", description = "Vérification publique de disponibilité")
public class PingController {

    @Operation(summary = "Vérifier que le service répond",
            description = "Adresse publique et sans authentification, appelée par la surveillance externe.")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("statut", "ok", "horodatage", Instant.now()),
                "Service en ligne"));
    }
}
