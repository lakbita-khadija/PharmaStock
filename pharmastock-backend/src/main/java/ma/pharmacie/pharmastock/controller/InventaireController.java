package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import ma.pharmacie.pharmastock.service.impl.InventaireService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventaires")
@RequiredArgsConstructor
@Tag(name = "Inventaire", description = "Gestion des inventaires physiques")
public class InventaireController {

    private final InventaireService      inventaireService;
    private final UtilisateurRepository utilisateurRepository;

    record DemarrerRequest(String type, String responsable, String commentaire) {}

    @GetMapping
    @Operation(summary = "Liste des inventaires")
    public ResponseEntity<List<InventaireService.InventaireSession>> getAll() {
        return ResponseEntity.ok(inventaireService.listerTous());
    }

    @PostMapping
    @Operation(summary = "Démarrer un inventaire")
    public ResponseEntity<InventaireService.InventaireSession> demarrer(
            @RequestBody DemarrerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                inventaireService.demarrer(req.type(), req.responsable(), req.commentaire())
        );
    }

    @PutMapping("/{id}/lignes")
    @Operation(summary = "Saisir les quantités physiques")
    public ResponseEntity<InventaireService.InventaireSession> saisir(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> saisies) {
        return ResponseEntity.ok(inventaireService.saisirLignes(id, saisies));
    }

    @GetMapping("/{id}/ecarts")
    @Operation(summary = "Calculer les écarts de l'inventaire")
    public ResponseEntity<List<InventaireService.EcartDto>> ecarts(@PathVariable Long id) {
        return ResponseEntity.ok(inventaireService.calculerEcarts(id));
    }

    @PutMapping("/{id}/valider")
    @Operation(summary = "Valider l'inventaire et régulariser le stock")
    public ResponseEntity<InventaireService.InventaireSession> valider(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Utilisateur user = utilisateurRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return ResponseEntity.ok(inventaireService.valider(id, user));
    }
}
