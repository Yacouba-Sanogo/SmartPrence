package com.smartpresence.controller;

import com.smartpresence.dto.request.InscriptionEtudiantRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.ClasseResponse;
import com.smartpresence.dto.response.EtudiantResponse;
import com.smartpresence.service.InscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Inscription des étudiants par eux-mêmes.
 *
 * <p>Seules adresses publiques de l'application avec la connexion : un candidat n'a
 * par définition aucun compte au moment où il s'inscrit. Le verrou n'est donc pas
 * l'authentification mais le numéro CENOU, qui doit figurer au référentiel tenu par
 * la scolarité et n'avoir jamais servi.</p>
 */
@RestController
@RequestMapping("/inscription")
@RequiredArgsConstructor
@Tag(name = "Inscription", description = "Inscription libre des étudiants")
public class InscriptionController {

    private final InscriptionService inscriptionService;

    @Operation(summary = "Lister les classes ouvertes à l'inscription",
            description = "Adresse publique : le candidat doit pouvoir choisir sa classe "
                    + "avant d'avoir le moindre compte. Ne renvoie aucune donnée personnelle.")
    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<List<ClasseResponse>>> classes() {
        return ResponseEntity.ok(ApiResponse.success(
                inscriptionService.classesOuvertes(), "Classes disponibles"));
    }

    @Operation(summary = "S'inscrire",
            description = "Crée l'étudiant et son accès mobile en un seul geste. "
                    + "Refusé si le numéro CENOU est inconnu du référentiel ou déjà utilisé.")
    @PostMapping
    public ResponseEntity<ApiResponse<EtudiantResponse>> inscrire(
            @Valid @RequestBody InscriptionEtudiantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                inscriptionService.inscrire(request),
                "Inscription enregistrée — vous pouvez maintenant vous connecter"));
    }
}
