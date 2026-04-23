package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.entity.Vente;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.ResourceNotFoundException;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import ma.pharmacie.pharmastock.service.impl.VenteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/ventes")
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Gestion des ventes")
public class VenteController {

    private final VenteService venteService;
    private final UtilisateurRepository utilisateurRepository;

    record AnnulationRequest(String motif) {}

    @GetMapping
    @Operation(summary = "Historique des ventes")
    public ResponseEntity<Page<Vente>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        if (q != null && q.isBlank()) {
            q = null;
        }

        LocalDateTime from = null;
        LocalDateTime to = null;

        if (dateFrom != null && !dateFrom.isBlank()) {
            from = LocalDate.parse(dateFrom).atStartOfDay();
        }

        if (dateTo != null && !dateTo.isBlank()) {
            to = LocalDate.parse(dateTo).atTime(23, 59, 59);
        }

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(venteService.listerVentes(q, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une vente")
    public ResponseEntity<Vente> getById(@PathVariable Long id) {
        return ResponseEntity.ok(venteService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle vente (caisse)")
    public ResponseEntity<Vente> create(
            @Valid @RequestBody VenteService.VenteRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        Utilisateur caissier = utilisateurRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Vente vente = venteService.creerVente(req, caissier);
        return ResponseEntity.status(HttpStatus.CREATED).body(vente);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    @Operation(summary = "Annuler une vente (pharmacien uniquement)")
    public ResponseEntity<Vente> annuler(
            @PathVariable Long id,
            @RequestBody AnnulationRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        Utilisateur pharmacien = utilisateurRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return ResponseEntity.ok(venteService.annulerVente(id, req.motif(), pharmacien));
    }
}