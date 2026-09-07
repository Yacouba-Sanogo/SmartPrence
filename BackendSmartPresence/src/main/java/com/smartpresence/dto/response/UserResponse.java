package com.smartpresence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Données d'un utilisateur exposées via l'API REST.
 *
 * <p>Ne contient <b>jamais</b> le mot de passe haché.</p>
 *
 * @since 0.0.1
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String nom;
    private String prenom;
    private boolean actif;
    private Set<RoleResponse> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
