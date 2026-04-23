package ma.pharmacie.pharmastock.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import ma.pharmacie.pharmastock.security.JwtUtil;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    // ── Réponse de connexion ──
    public record LoginResponse(String token, String refreshToken, UserInfo user) {}
    public record UserInfo(Long id, String nom, String prenom, String email, String role) {}

    @Transactional
    public LoginResponse login(String email, String password) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

        if (!utilisateur.isActif()) {
            throw new DisabledException("Compte désactivé.");
        }
        if (utilisateur.getTentativesEchec() >= 5) {
            throw new LockedException("Compte verrouillé. Contactez l'administrateur.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException e) {
            utilisateurRepository.incrementerTentativesEchec(utilisateur.getId());
            throw new BadCredentialsException("Email ou mot de passe incorrect.");
        }

        // Réinitialiser les tentatives et maj dernière connexion
        utilisateurRepository.reinitialiserTentatives(utilisateur.getId());
        utilisateur.setDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        Map<String, Object> claims = Map.of(
                "role",   utilisateur.getRole().name(),
                "userId", utilisateur.getId()
        );

        String token        = jwtUtil.generateToken(userDetails, claims);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        UserInfo userInfo = new UserInfo(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole().name()
        );

        log.info("Connexion réussie : {}", email);
        return new LoginResponse(token, refreshToken, userInfo);
    }

    @Transactional
    public void changerMotDePasse(Long userId, String ancienMdp, String nouveauMdp) {
        Utilisateur u = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        if (!passwordEncoder.matches(ancienMdp, u.getMotDePasse())) {
            throw new BusinessException("Ancien mot de passe incorrect.");
        }
        u.setMotDePasse(passwordEncoder.encode(nouveauMdp));
        utilisateurRepository.save(u);
    }
}
