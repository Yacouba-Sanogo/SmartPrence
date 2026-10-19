package com.smartpresence.service;

import com.smartpresence.dto.request.LoginRequest;
import com.smartpresence.dto.request.RefreshTokenRequest;
import com.smartpresence.dto.request.UserRegistrationRequest;
import com.smartpresence.dto.response.AuthResponse;
import com.smartpresence.dto.response.UserResponse;

/**
 * Service gérant l'authentification et l'inscription des utilisateurs.
 *
 * @since 0.0.1
 */
public interface AuthenticationService {

    /**
     * Authentifie un utilisateur humain et émet un jeton JWT.
     *
     * @param request identifiants de connexion (email + mot de passe)
     * @return réponse d'authentification avec les jetons JWT
     */
    AuthResponse login(LoginRequest request);

    /**
     * Rafraîchit un jeton d'accès expiré via un jeton de rafraîchissement.
     *
     * @param request jeton de rafraîchissement
     * @return nouveaux jetons d'accès
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Enregistre un nouvel utilisateur.
     *
     * @param request données d'inscription
     * @return l'utilisateur créé
     */
    UserResponse register(UserRegistrationRequest request);

    /**
     * Change le mot de passe de l'utilisateur connecté.
     *
     * <p>L'ancien est exigé : un jeton volé sur un téléphone déverrouillé ne doit pas
     * suffire à s'approprier le compte.</p>
     */
    void changerMotDePasse(java.util.UUID utilisateurId, String motDePasseActuel, String nouveauMotDePasse);
}
