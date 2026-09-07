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
    public ResponseEntity<ApiResponse<Void>> marquerCommeLue(@PathVariable UUID id) {
        notificationService.marquerCommeLue(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marquée comme lue"));
    }
}
