package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Fournisseur;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.FournisseurRepository;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs", description = "Gestion des fournisseurs")
public class FournisseurController {

    private final FournisseurRepository fournisseurRepository;

    record FournisseurRequest(
        @NotBlank String nom,
        String raisonSociale, String adresse,
        String telephone, String email, String contactNom
    ) {}

    @GetMapping
    @Operation(summary = "Lister les fournisseurs")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom"));
        return ResponseEntity.ok(fournisseurRepository.searchActifs(q, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "Tous les fournisseurs actifs (pour select)")
    public ResponseEntity<List<Fournisseur>> getAllActifs() {
        return ResponseEntity.ok(fournisseurRepository.findByActifTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Fournisseur> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
            fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + id))
        );
    }

    @PostMapping
    @Operation(summary = "Créer un fournisseur")
    public ResponseEntity<Fournisseur> create(@Valid @RequestBody FournisseurRequest req) {
        Fournisseur f = Fournisseur.builder()
            .nom(req.nom()).raisonSociale(req.raisonSociale())
            .adresse(req.adresse()).telephone(req.telephone())
            .email(req.email()).contactNom(req.contactNom())
            .actif(true).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(fournisseurRepository.save(f));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un fournisseur")
    public ResponseEntity<Fournisseur> update(@PathVariable Long id,
                                               @Valid @RequestBody FournisseurRequest req) {
        Fournisseur f = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + id));
        f.setNom(req.nom()); f.setRaisonSociale(req.raisonSociale());
        f.setAdresse(req.adresse()); f.setTelephone(req.telephone());
        f.setEmail(req.email()); f.setContactNom(req.contactNom());
        return ResponseEntity.ok(fournisseurRepository.save(f));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un fournisseur")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Fournisseur f = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable : " + id));
        f.setActif(false);
        fournisseurRepository.save(f);
        return ResponseEntity.noContent().build();
    }
}
