package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.enums.RoleUtilisateur;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Utilisateurs", description = "Gestion des comptes (Admin uniquement)")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    record UtilisateurRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        String motDePasse,
        @NotBlank String role,
        Boolean actif
    ) {}

    @GetMapping
    @Operation(summary = "Lister tous les utilisateurs")
    public ResponseEntity<List<Utilisateur>> getAll() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Utilisateur> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
            utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id))
        );
    }

    @PostMapping
    @Operation(summary = "Créer un utilisateur")
    public ResponseEntity<Utilisateur> create(@Valid @RequestBody UtilisateurRequest req) {
        if (utilisateurRepository.existsByEmail(req.email())) {
            throw new BusinessException("Un compte avec cet email existe déjà.");
        }
        if (req.motDePasse() == null || req.motDePasse().isBlank()) {
            throw new BusinessException("Le mot de passe est requis à la création.");
        }

        Utilisateur u = Utilisateur.builder()
                .nom(req.nom()).prenom(req.prenom()).email(req.email())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .role(RoleUtilisateur.valueOf(req.role()))
                .actif(req.actif() != null ? req.actif() : true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurRepository.save(u));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity<Utilisateur> update(@PathVariable Long id,
                                               @Valid @RequestBody UtilisateurRequest req) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));

        u.setNom(req.nom()); u.setPrenom(req.prenom()); u.setEmail(req.email());
        u.setRole(RoleUtilisateur.valueOf(req.role()));
        if (req.actif() != null) u.setActif(req.actif());
        if (req.motDePasse() != null && !req.motDePasse().isBlank()) {
            u.setMotDePasse(passwordEncoder.encode(req.motDePasse()));
        }

        return ResponseEntity.ok(utilisateurRepository.save(u));
    }

    @PutMapping("/{id}/deverrouiller")
    @Operation(summary = "Déverrouiller un compte")
    public ResponseEntity<Utilisateur> deverrouiller(@PathVariable Long id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        u.setTentativesEchec(0);
        u.setActif(true);
        return ResponseEntity.ok(utilisateurRepository.save(u));
    }

    @PutMapping("/{id}/desactiver")
    @Operation(summary = "Désactiver un compte")
    public ResponseEntity<Utilisateur> desactiver(@PathVariable Long id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        u.setActif(false);
        return ResponseEntity.ok(utilisateurRepository.save(u));
    }
}
