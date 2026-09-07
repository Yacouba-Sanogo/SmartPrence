package com.smartpresence.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Format de réponse HTTP uniforme pour l'ensemble des API du projet SmartPresence.
 *
 * <p>Conforme aux spécifications REST du projet (cf. {@code SmartPresence_CONTEXT.md} §11.1) :</p>
 * <ul>
 *   <li>{@code success} : {@code true} en cas de succès, {@code false} en cas d'erreur.</li>
 *   <li>{@code message} : description textuelle explicite.</li>
 *   <li>{@code data} : payload utile (objet, liste, ou {@code null}).</li>
 *   <li>{@code timestamp} : horodatage ISO-8601 de la réponse.</li>
 * </ul>
 *
 * @param <T> type de la donnée contenue dans la réponse
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Opération réalisée avec succès");
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }
}
