package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.StatutDispensation;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medicaments")
@RequiredArgsConstructor
@Tag(name = "Médicaments", description = "Gestion du catalogue")
public class MedicamentController {

    private final MedicamentRepository medicamentRepository;
    private final CategorieRepository categorieRepository;
    private final FournisseurRepository fournisseurRepository;

    // ── DTO ──
    record MedicamentRequest(
        @NotBlank String nomCommercial,
        @NotBlank String dci,
        @NotBlank String formegalenique,
        @NotBlank String dosage,
        String codeBarre,
        Long categorieId,
        Long fournisseurId,
        BigDecimal prixAchatHt,
        @NotNull BigDecimal prixVenteTtc,
        Integer seuilMinimal,
        String statutDispensation,
        Boolean actif
    ) {}

    @GetMapping
    @Operation(summary = "Lister les médicaments avec pagination et recherche")
    public ResponseEntity<Page<Medicament>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nomCommercial").ascending());
        return ResponseEntity.ok(medicamentRepository.searchActifs(q, categorieId, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche rapide pour la caisse")
    public ResponseEntity<List<Medicament>> search(@RequestParam String q) {
        Pageable p = PageRequest.of(0, 10);
        return ResponseEntity.ok(medicamentRepository.searchForSale(q, p));
    }

    @GetMapping("/barcode/{code}")
    @Operation(summary = "Recherche par code-barres")
    public ResponseEntity<Medicament> getByBarcode(@PathVariable String code) {
        return ResponseEntity.ok(
            medicamentRepository.findByCodeBarre(code)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable avec code : " + code))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un médicament")
    public ResponseEntity<Medicament> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
            medicamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable : " + id))
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    @Operation(summary = "Créer un médicament")
    public ResponseEntity<Medicament> create(@Valid @RequestBody MedicamentRequest req) {
        Medicament med = buildFromRequest(req, new Medicament());
        return ResponseEntity.status(HttpStatus.CREATED).body(medicamentRepository.save(med));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    @Operation(summary = "Modifier un médicament")
    public ResponseEntity<Medicament> update(@PathVariable Long id,
                                              @Valid @RequestBody MedicamentRequest req) {
        Medicament med = medicamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable : " + id));
        buildFromRequest(req, med);
        return ResponseEntity.ok(medicamentRepository.save(med));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    @Operation(summary = "Archiver un médicament (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Medicament med = medicamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable : " + id));
        med.setActif(false);
        medicamentRepository.save(med);
        return ResponseEntity.noContent().build();
    }

    private Medicament buildFromRequest(MedicamentRequest req, Medicament med) {
        med.setNomCommercial(req.nomCommercial());
        med.setDci(req.dci());
        med.setFormegalenique(req.formegalenique());
        med.setDosage(req.dosage());
        med.setCodeBarre(req.codeBarre());
        med.setPrixAchatHt(req.prixAchatHt());
        med.setPrixVenteTtc(req.prixVenteTtc());
        med.setSeuilMinimal(req.seuilMinimal() != null ? req.seuilMinimal() : 10);
        med.setActif(req.actif() != null ? req.actif() : true);

        if (req.statutDispensation() != null) {
            med.setStatutDispensation(StatutDispensation.valueOf(req.statutDispensation()));
        }
        if (req.categorieId() != null) {
            categorieRepository.findById(req.categorieId()).ifPresent(med::setCategorie);
        }
        if (req.fournisseurId() != null) {
            fournisseurRepository.findById(req.fournisseurId()).ifPresent(med::setFournisseur);
        }
        return med;
    }
}
