package com.smartpresence.service;

import com.smartpresence.dto.response.DemandeEnrolementResponse;
import com.smartpresence.dto.response.EnrolementAServirResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Enrôlement biométrique en libre-service.
 *
 * <h2>Le problème que cela résout</h2>
 * <p>Jusqu'ici, enrôler un étudiant demandait deux personnes : l'étudiant devant le
 * capteur, et un agent de la scolarité devant l'interface d'administration, occupé à
 * recopier à la main la référence affichée à l'écran du lecteur. Pour une promotion
 * entière, le compte est vite fait — et une seule ligne recopiée de travers attribue
 * une empreinte au mauvais étudiant.</p>
 *
 * <h2>Comment</h2>
 * <p>L'étudiant ouvre lui-même une demande depuis son téléphone ; le serveur lui rend
 * un code. Le lecteur, de son côté, demande qui est attendu, affiche ce même code et
 * le nom. L'étudiant vérifie que c'est bien le sien, pose le doigt, et le lecteur
 * renvoie la référence produite <b>en citant le code</b> — ce qui suffit au serveur
 * pour relier l'empreinte à la bonne personne, sans qu'aucun identifiant interne
 * n'ait quitté la base.</p>
 *
 * @since 0.0.1
 */
public interface EnrolementService {

    /**
     * Ouvre une demande pour l'étudiant connecté, ou rend celle qui court déjà.
     *
     * <p>Volontairement idempotent : rouvrir l'écran ne doit pas produire un second
     * code et invalider le premier, que l'étudiant a peut-être déjà sous les yeux.</p>
     *
     * @throws com.smartpresence.exception.BusinessException si l'étudiant est déjà
     *         enrôlé ou son dossier inactif
     */
    DemandeEnrolementResponse demander(UUID utilisateurId);

    /** État de la dernière demande de l'étudiant connecté. */
    DemandeEnrolementResponse maDemande(UUID utilisateurId);

    /** Referme la demande en cours de l'étudiant connecté. */
    DemandeEnrolementResponse annuler(UUID utilisateurId);

    /**
     * Prochaine demande à servir, du point de vue d'un lecteur.
     *
     * <p>La demande rendue est aussitôt réservée à ce lecteur pour quelques minutes :
     * deux capteurs voisins ne doivent pas appeler le même étudiant en même temps.</p>
     *
     * @return la demande, ou {@link Optional#empty()} si personne n'attend
     */
    Optional<EnrolementAServirResponse> prochaine(UUID deviceId);

    /**
     * Associe à l'étudiant la référence produite par le capteur.
     *
     * @param deviceId  lecteur ayant réalisé la capture
     * @param code      code de la demande servie
     * @param reference référence logique de l'emplacement occupé dans le capteur
     */
    DemandeEnrolementResponse enregistrer(UUID deviceId, String code, String reference);
}
