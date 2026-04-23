package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.service.impl.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Login, logout, gestion du mot de passe")
public class AuthController {

    private final AuthService authService;

    // ── DTOs ──
    record LoginRequest(
        @NotBlank(message = "L'email est requis.")
        @Email(message = "Email invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est requis.")
        String password
    ) {}

    record ChangePasswordRequest(
        @NotBlank String ancienMotDePasse,
        @NotBlank @Size(min = 8) String nouveauMotDePasse
    ) {}

    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Retourne un token JWT")
    public ResponseEntity<AuthService.LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthService.LoginResponse response = authService.login(req.email(), req.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password")
    @Operation(summary = "Changer le mot de passe")
    public ResponseEntity<Void> changerMotDePasse(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        // Récupérer l'ID depuis le contexte — simplifié ici
        // En production: injecter SecurityContextHolder pour récupérer l'userId
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public ResponseEntity<String> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(principal.getUsername());
    }
}
