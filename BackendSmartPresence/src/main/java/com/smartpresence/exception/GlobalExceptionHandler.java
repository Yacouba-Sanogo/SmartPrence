package com.smartpresence.exception;

import com.smartpresence.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions pour les API REST SmartPresence.
 *
 * <p>Centralise l'interception des erreurs applicatives et techniques pour
 * retourner des réponses HTTP au format uniforme {@link ApiResponse}.</p>
 *
 * @since 0.0.1
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Ressource non trouvée : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Erreur métier [{}] : {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        log.warn("Erreur de validation : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation Bean échouée : {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Erreur de validation des champs", errors));
    }

    /**
     * Corps de requête illisible : JSON malformé, ou valeur d'un type inattendu.
     *
     * <p>Sans ce gestionnaire, une simple date sans fuseau ({@code "2026-08-20T08:00:00"}
     * pour un {@code Instant}) tombait dans le filet général et renvoyait 500 « erreur
     * interne » — alors que la faute est côté client et parfaitement corrigeable.</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {
        log.warn("Corps de requête illisible : {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(messageLisible(ex)));
    }

    /**
     * Traduit un échec de désérialisation en une phrase qui désigne le vrai coupable.
     *
     * <p>Ce gestionnaire renvoyait auparavant une phrase unique conseillant d'ajouter
     * un fuseau horaire. Envoyer {@code "type": "TP"} sur un champ énuméré produisait
     * donc le conseil d'horodater une valeur qui n'est pas une date — une piste fausse
     * qui coûte plus de temps qu'un message absent. Jackson connaît le champ fautif et
     * le type attendu : les nommer suffit.</p>
     */
    private String messageLisible(HttpMessageNotReadableException ex) {
        // getCause(), pas getMostSpecificCause() : pour une date mal formée ce dernier
        // descend jusqu'au DateTimeParseException, qui ne porte ni le champ ni le type
        // attendu. C'est l'InvalidFormatException juste au-dessus qui les connaît.
        if (ex.getCause() instanceof InvalidFormatException cause) {
            String champ = cause.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            String ou = champ.isEmpty() ? "" : " du champ « " + champ + " »";
            Class<?> attendu = cause.getTargetType();

            if (attendu != null && attendu.isEnum()) {
                String valeurs = Arrays.stream(attendu.getEnumConstants())
                        .map(String::valueOf).collect(Collectors.joining(", "));
                return "La valeur « " + cause.getValue() + " »" + ou
                        + " n'est pas reconnue. Valeurs acceptées : " + valeurs + ".";
            }
            if (attendu != null && Temporal.class.isAssignableFrom(attendu)) {
                return "La date « " + cause.getValue() + " »" + ou + " est mal formée. "
                        + "Les instants doivent porter un fuseau, par exemple "
                        + "2026-08-20T08:00:00Z ; les dates seules s'écrivent 2026-08-20.";
            }
            return "La valeur « " + cause.getValue() + " »" + ou + " est invalide.";
        }
        return "Le corps de la requête est illisible : il n'est pas au format JSON attendu.";
    }

    /**
     * Paramètre d'URL obligatoire absent.
     *
     * <p>Comme pour un corps illisible, la faute est côté client : lui renvoyer « erreur
     * interne » l'enverrait chercher le problème au mauvais endroit.</p>
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleParametreManquant(
            MissingServletRequestParameterException ex) {
        log.warn("Paramètre obligatoire absent : {}", ex.getParameterName());
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Le paramètre « " + ex.getParameterName() + " » est obligatoire."));
    }

    /** Paramètre d'URL du mauvais type : identifiant non numérique, date mal formée. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.warn("Paramètre invalide : {}={}", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Le paramètre « " + ex.getName() + " » n'a pas un format valide."));
    }

    /**
     * Une URL inconnue est une faute d'adressage, pas une panne du serveur.
     *
     * <p>Sans ce gestionnaire, Spring laisse remonter {@link NoResourceFoundException}
     * jusqu'au filet général, qui répond « 500 — une erreur interne du serveur est
     * survenue ». Appeler {@code /moi} au lieu de {@code /moi/profil} envoyait donc
     * chercher une panne côté serveur pour une simple faute de chemin.</p>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRouteInconnue(NoResourceFoundException ex) {
        log.warn("Route inconnue : {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(
                "Aucune ressource à l'adresse « /" + ex.getResourcePath() + " »."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentification échouée : identifiants invalides");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email ou mot de passe incorrect"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Accès refusé : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Accès refusé. Privilèges insuffisants."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        log.error("Erreur interne non gérée : ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Une erreur interne du serveur est survenue"));
    }
}
