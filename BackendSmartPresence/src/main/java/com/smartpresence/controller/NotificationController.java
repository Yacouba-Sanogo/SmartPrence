package com.smartpresence.controller;

import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.NotificationResponse;
import com.smartpresence.security.CustomUserDetails;
import com.smartpresence.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion des notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lister les notifications du destinataire connecté")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> findByDestinataire(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.findByDestinataire(userDetails.getId()), "Notifications récupérées"));
    }

    @Operation(summary = "Compter les notifications non lues")
    @GetMapping("/non-lues")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> countNonLues(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.countNonLues(userDetails.getId()), "Nombre de notifications non lues"));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PatchMapping("/{id}/lue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> marquerCommeLue(Authentication authentication, @PathVariable UUID id) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        notificationService.marquerCommeLue(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marquée comme lue"));
    }

    @Operation(summary = "Marquer toutes mes notifications comme lues")
    @PatchMapping("/lues")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> toutMarquerCommeLu(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.toutMarquerCommeLu(userDetails.getId()), "Notifications marquées comme lues"));
    }

    @Operation(summary = "Diffuser une annonce aux enseignants",
            description = "Destinataires : un enseignant nommé, sinon les enseignants d'une classe, "
                    + "sinon tous les enseignants actifs. Réservé à l'administration.")
    @PostMapping("/annonces")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
    public ResponseEntity<ApiResponse<Integer>> diffuser(
            @jakarta.validation.Valid @RequestBody com.smartpresence.dto.request.AnnonceRequest requete) {
        int destinataires = notificationService.diffuserAuxEnseignants(
                requete.type() == null ? com.smartpresence.constants.TypeNotification.ANNONCE : requete.type(),
                requete.titre().trim(), requete.message().trim(), requete.classeId(), requete.personnelId());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(ApiResponse.success(
                destinataires, destinataires + " enseignant(s) notifié(s)"));
    }
}
