package com.smartpresence.controller;

import com.smartpresence.dto.request.EnseignementRequests;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.NoteResponse;
import com.smartpresence.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Décision de l'administration sur les notes transmises par les enseignants.
 *
 * <p>Tant qu'une note n'est pas validée ici, elle n'apparaît dans aucun bulletin.</p>
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_SCOLARITE')")
@Tag(name = "Validation des notes", description = "Notes transmises par les enseignants, à publier ou à renvoyer")
public class ValidationNoteController {

    private final NoteService noteService;

    @GetMapping("/notes/a-valider")
    @Operation(summary = "Notes transmises en attente de décision",
            description = "Triées par classe puis matière. Filtrables par classe.")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> aValider(
            @RequestParam(required = false) Long classeId) {
        return ResponseEntity.ok(ApiResponse.success(noteService.notesAValider(classeId)));
    }

    @PostMapping("/notes/validation")
    @Operation(summary = "Valider des notes transmises",
            description = "Elles entrent dans les bulletins. Le lot est traité en entier ou pas du tout.")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> valider(
            @Valid @RequestBody EnseignementRequests.LotNotes lot) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.valider(lot.noteIds()), "Notes validées"));
    }

    @PostMapping("/notes/renvoi")
    @Operation(summary = "Renvoyer des notes à leur enseignant",
            description = "Elles redeviennent des brouillons, avec le motif à corriger.")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> renvoyer(
            @Valid @RequestBody EnseignementRequests.LotNotes lot) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.renvoyer(lot.noteIds(), lot.motif()), "Notes renvoyées à l'enseignant"));
    }
}
