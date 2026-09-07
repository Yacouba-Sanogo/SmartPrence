package com.smartpresence.security;

import com.smartpresence.constants.DeviceStatut;
import com.smartpresence.entity.Device;
import com.smartpresence.repository.DeviceRepository;
import com.smartpresence.utils.HachageCleApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.Collections;
import java.util.Optional;

/**
 * Filtre d'authentification par clé d'API pour les appareils ESP32.
 *
 * <p>Intercepte uniquement les requêtes adressées aux endpoints {@code /api/esp32/**}.</p>
 * <p>Validation de la clé d'API transmise via le header {@code X-API-KEY} par rapport
 * aux hashes enregistrés dans la table {@code devices}.</p>
 *
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";

    /** Racine des endpoints réservés aux appareils embarqués. */
    private static final String PREFIXE_ESP32 = "/esp32/";

    private final DeviceRepository deviceRepository;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !cheminApplicatif(request).startsWith(PREFIXE_ESP32);
    }

    /**
     * Chemin de la requête relatif à l'application, hors chemin de contexte.
     *
     * <p>{@code getServletPath()} a été écarté : sa valeur dépend du mapping de la
     * servlet et n'est pas garantie hors d'un conteneur complet — elle est vide dans
     * un test {@code MockMvc}, ce qui désactivait silencieusement ce filtre et
     * transformait toute requête ESP32 en {@code 403}. Le calcul à partir de
     * {@code requestURI} moins {@code contextPath} est, lui, défini de la même
     * façon partout : en conteneur, derrière un proxy et en test.</p>
     */
    private String cheminApplicatif(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexte = request.getContextPath();
        if (StringUtils.hasLength(contexte) && uri.startsWith(contexte)) {
            return uri.substring(contexte.length());
        }
        return uri;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getHeader("X-Device-API-Key");
        }

        if (StringUtils.hasText(apiKey)) {
            // Rechercher l'appareil correspondant dans la base
            Optional<Device> deviceOpt = findDeviceByApiKey(apiKey);
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                if (device.getStatut() == DeviceStatut.ACTIF) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    device,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVICE"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Appareil ESP32 authentifié avec succès : {}", device.getNom());
                } else {
                    log.warn("Tentative d'accès par un appareil inactif/suspendu : {}", device.getNom());
                }
            } else {
                log.warn("Clé d'API ESP32 non reconnue");
            }
        } else {
            log.warn("Requête ESP32 sans clé d'API dans les headers");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Résout l'appareil correspondant à une clé d'API.
     *
     * <p><b>Une seule lecture indexée</b> sur l'empreinte déterministe de la clé. La
     * version antérieure chargeait tout le parc et testait BCrypt sur chaque appareil à
     * chaque requête : le coût croissait avec le nombre de lecteurs déployés, et la
     * première correspondance trouvée l'emportait — donc arbitrairement lorsque deux
     * appareils partageaient une clé (cf. {@link HachageCleApi}).</p>
     */
    private Optional<Device> findDeviceByApiKey(String apiKey) {
        return deviceRepository.findByApiKeyHash(HachageCleApi.empreinte(apiKey));
    }
}
