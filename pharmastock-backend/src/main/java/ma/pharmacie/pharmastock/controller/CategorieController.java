package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Categorie;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.CategorieRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories", description = "Catégories thérapeutiques")
public class CategorieController {

    private final CategorieRepository categorieRepository;

    record CategorieRequest(@NotBlank String nom, String description) {}

    @GetMapping
    @Operation(summary = "Lister toutes les catégories")
    public ResponseEntity<List<Categorie>> getAll() {
        return ResponseEntity.ok(categorieRepository.findAllByOrderByNomAsc());
    }

    @PostMapping
    @Operation(summary = "Créer une catégorie")
    public ResponseEntity<Categorie> create(@Valid @RequestBody CategorieRequest req) {
        if (categorieRepository.existsByNom(req.nom())) {
            throw new BusinessException("Une catégorie avec ce nom existe déjà.");
        }
        Categorie cat = Categorie.builder().nom(req.nom()).description(req.description()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(categorieRepository.save(cat));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une catégorie")
    public ResponseEntity<Categorie> update(@PathVariable Long id,
                                             @Valid @RequestBody CategorieRequest req) {
        Categorie cat = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        cat.setNom(req.nom());
        cat.setDescription(req.description());
        return ResponseEntity.ok(categorieRepository.save(cat));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une catégorie")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
