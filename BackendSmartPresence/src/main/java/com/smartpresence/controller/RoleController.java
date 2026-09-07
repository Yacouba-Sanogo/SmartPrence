package com.smartpresence.controller;

import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.RoleResponse;
import com.smartpresence.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur de gestion des rôles.
 *
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Gestion des rôles et permissions")
public class RoleController {

    private final RoleService roleService;

    /**
     * Récupérer tous les rôles disponibles.
     */
    @Operation(summary = "Lister les rôles", description = "Récupère tous les rôles définis dans le système")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        List<RoleResponse> roles = roleService.findAll();
        return ResponseEntity.ok(ApiResponse.success(roles, "Rôles récupérés"));
    }
}
