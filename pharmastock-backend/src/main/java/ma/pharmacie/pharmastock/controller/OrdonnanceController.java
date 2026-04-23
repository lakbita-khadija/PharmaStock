package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Ordonnance;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.OrdonnanceRepository;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ordonnances")
@RequiredArgsConstructor
@Tag(name = "Ordonnances", description = "Gestion des ordonnances médicales")
public class OrdonnanceController {

    private final OrdonnanceRepository  ordonnanceRepository;
    private final UtilisateurRepository utilisateurRepository;

    record OrdonnanceRequest(
        @NotBlank String numeroOrdonnance,
        String prescripteur,
        String patientNom,
        @NotBlank String datePrescription,
        String dateValidite,
        String patientNaissance
    ) {}

    @GetMapping
    @Operation(summary = "Lister les ordonnances")
    public ResponseEntity<Page<Ordonnance>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        return ResponseEntity.ok(ordonnanceRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ordonnance> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ordonnanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordonnance introuvable : " + id)));
    }

    @PostMapping
    @Operation(summary = "Enregistrer une ordonnance")
    public ResponseEntity<Ordonnance> create(
            @Valid @RequestBody OrdonnanceRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        if (ordonnanceRepository.existsByNumeroOrdonnance(req.numeroOrdonnance())) {
            throw new BusinessException("Ce numéro d'ordonnance existe déjà.");
        }

        Utilisateur user = utilisateurRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Ordonnance o = Ordonnance.builder()
                .numeroOrdonnance(req.numeroOrdonnance())
                .prescripteur(req.prescripteur())
                .patientNom(req.patientNom())
                .datePrescription(LocalDate.parse(req.datePrescription()))
                .dateValidite(req.dateValidite() != null ? LocalDate.parse(req.dateValidite()) : null)
                .patientNaissance(req.patientNaissance() != null ? LocalDate.parse(req.patientNaissance()) : null)
                .valideePar(user)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ordonnanceRepository.save(o));
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    @Operation(summary = "Valider une ordonnance par le pharmacien")
    public ResponseEntity<Ordonnance> valider(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        Ordonnance o = ordonnanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordonnance introuvable : " + id));
        Utilisateur pharmacien = utilisateurRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        o.setValideePar(pharmacien);
        return ResponseEntity.ok(ordonnanceRepository.save(o));
    }
}
