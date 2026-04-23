package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.AlerteStock;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.enums.NiveauAlerte;
import ma.pharmacie.pharmastock.enums.StatutAlerte;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.ResourceNotFoundException;
import ma.pharmacie.pharmastock.repository.AlerteStockRepository;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alertes")
@RequiredArgsConstructor
@Tag(name = "Alertes", description = "Centre des alertes automatiques")
public class AlerteController {

    private final AlerteStockRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;

    record AcquittementRequest(String commentaire) {}

    @GetMapping
    @Operation(summary = "Liste des alertes avec filtre statut/niveau")
    public ResponseEntity<List<AlerteStock>> getAll(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String niveau,
            @RequestParam(required = false) Integer size) {

        StatutAlerte statutEnum = null;
        NiveauAlerte niveauEnum = null;

        if (statut != null && !statut.isBlank()) {
            statutEnum = StatutAlerte.valueOf(statut.trim().toUpperCase());
        }

        if (niveau != null && !niveau.isBlank()) {
            niveauEnum = NiveauAlerte.valueOf(niveau.trim().toUpperCase());
        }

        List<AlerteStock> alertes = alerteRepository.searchWithRelations(statutEnum, niveauEnum);

        if (size != null && size > 0 && alertes.size() > size) {
            alertes = alertes.subList(0, size);
        }

        return ResponseEntity.ok(alertes);
    }

    @GetMapping("/count")
    @Operation(summary = "Compteur d'alertes actives (pour badge header)")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of(
                "count", alerteRepository.countActives(),
                "critiques", alerteRepository.countCritiques()
        ));
    }

    @PutMapping("/{id}/acquitter")
    @Operation(summary = "Acquitter une alerte")
    public ResponseEntity<AlerteStock> acquitter(
            @PathVariable Long id,
            @RequestBody(required = false) AcquittementRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        AlerteStock alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte introuvable : " + id));

        alerte.setStatut(StatutAlerte.ACQUITTEE);
        alerte.setDateAquittement(LocalDateTime.now());

        if (req != null && req.commentaire() != null && !req.commentaire().isBlank()) {
            alerte.setCommentaireAquittement(req.commentaire().trim());
        }

        if (principal != null) {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(principal.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Utilisateur introuvable : " + principal.getUsername()
                    ));
            alerte.setAcquittePar(utilisateur);
        }

        AlerteStock saved = alerteRepository.save(alerte);
        return ResponseEntity.ok(saved);
    }
}