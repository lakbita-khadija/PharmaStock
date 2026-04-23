package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.StatutCommande;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/commandes")
@RequiredArgsConstructor
@Tag(name = "Commandes fournisseurs")
public class CommandeController {

    private final CommandeFournisseurRepository commandeRepository;
    private final FournisseurRepository fournisseurRepository;
    private final MedicamentRepository medicamentRepository;
    private final UtilisateurRepository utilisateurRepository;

    record LigneCommandeRequest(
        @NotNull Long medicamentId,
        @NotNull @Min(1) Integer quantiteCommandee,
        BigDecimal prixUnitaire
    ) {}

    record CommandeRequest(
        @NotNull Long fournisseurId,
        @NotEmpty List<LigneCommandeRequest> lignes
    ) {}

    @GetMapping
    @Operation(summary = "Lister les commandes")
    public ResponseEntity<List<CommandeFournisseur>> getAll(
            @RequestParam(required = false) String statut) {
        if (statut != null && !statut.isBlank()) {
            return ResponseEntity.ok(commandeRepository.findByStatutOrderByDateCreationDesc(StatutCommande.valueOf(statut)));
        }
        return ResponseEntity.ok(commandeRepository.findAllWithDetails());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeFournisseur> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
            commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id))
        );
    }

    @PostMapping
    @Operation(summary = "Créer une commande fournisseur")
    public ResponseEntity<CommandeFournisseur> create(
            @Valid @RequestBody CommandeRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        Utilisateur createur = utilisateurRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Fournisseur fournisseur = fournisseurRepository.findById(req.fournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + req.fournisseurId()));

        CommandeFournisseur commande = CommandeFournisseur.builder()
                .numeroCommande(genererNumero())
                .statut(StatutCommande.BROUILLON)
                .fournisseur(fournisseur)
                .createur(createur)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (LigneCommandeRequest ligneReq : req.lignes()) {
            Medicament med = medicamentRepository.findById(ligneReq.medicamentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable : " + ligneReq.medicamentId()));

            LigneCommande ligne = LigneCommande.builder()
                    .commande(commande)
                    .medicament(med)
                    .quantiteCommandee(ligneReq.quantiteCommandee())
                    .prixUnitaire(ligneReq.prixUnitaire())
                    .build();
            commande.getLignes().add(ligne);

            if (ligneReq.prixUnitaire() != null) {
                total = total.add(ligneReq.prixUnitaire().multiply(BigDecimal.valueOf(ligneReq.quantiteCommandee())));
            }
        }
        commande.setMontantTotal(total);

        return ResponseEntity.status(HttpStatus.CREATED).body(commandeRepository.save(commande));
    }

    @PutMapping("/{id}/envoyer")
    @Operation(summary = "Envoyer la commande au fournisseur")
    public ResponseEntity<CommandeFournisseur> envoyer(@PathVariable Long id) {
        CommandeFournisseur commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id));
        if (commande.getStatut() != StatutCommande.BROUILLON) {
            throw new BusinessException("Seule une commande en brouillon peut être envoyée.");
        }
        commande.setStatut(StatutCommande.ENVOYEE);
        commande.setDateEnvoi(LocalDateTime.now());
        return ResponseEntity.ok(commandeRepository.save(commande));
    }

    @PutMapping("/{id}/annuler")
    @Operation(summary = "Annuler une commande")
    public ResponseEntity<CommandeFournisseur> annuler(@PathVariable Long id) {
        CommandeFournisseur commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id));
        if (commande.getStatut() == StatutCommande.RECUE_TOTALE) {
            throw new BusinessException("Impossible d'annuler une commande déjà réceptionnée.");
        }
        commande.setStatut(StatutCommande.ANNULEE);
        return ResponseEntity.ok(commandeRepository.save(commande));
    }

    private String genererNumero() {
        String prefix = "CMD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = commandeRepository.count() + 1;
        return prefix + "-" + String.format("%04d", count);
    }
}
