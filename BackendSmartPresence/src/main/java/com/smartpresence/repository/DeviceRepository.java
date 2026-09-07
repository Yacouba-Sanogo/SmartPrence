package com.smartpresence.repository;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux données des appareils ESP32 (authentification par clé d'API).
 *
 * @since 0.0.1
 * @see Device
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    /**
     * Recherche un appareil par le <b>hash</b> de sa clé d'API.
     * <p>Utilisée lors de l'authentification ESP32 : la clé reçue est hachée puis
     * comparée via cette recherche.</p>
     *
     * @param apiKeyHash hash de la clé d'API (jamais la clé en clair)
     * @return l'appareil trouvé, ou vide si inexistant
     */
    Optional<Device> findByApiKeyHash(String apiKeyHash);

    /**
     * Recherche un appareil par son adresse MAC.
     *
     * @param adresseMac adresse MAC physique
     * @return l'appareil trouvé, ou vide si inexistant
     */
    Optional<Device> findByAdresseMac(String adresseMac);

    /**
     * Indique si un hash de clé d'API est déjà attribué.
     *
     * @param apiKeyHash hash à vérifier
     * @return {@code true} si déjà attribué
     */
    boolean existsByApiKeyHash(String apiKeyHash);

    /**
     * Indique si une adresse MAC est déjà enregistrée.
     *
     * @param adresseMac adresse MAC à vérifier
     * @return {@code true} si déjà enregistrée
     */
    boolean existsByAdresseMac(String adresseMac);

    long countByStatut(DeviceStatut statut);

    Optional<Device> findBySalleId(Long salleId);

}
