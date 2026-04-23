package ma.pharmacie.pharmastock.service;

import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.enums.RoleUtilisateur;
import ma.pharmacie.pharmastock.repository.*;
import ma.pharmacie.pharmastock.security.JwtUtil;
import ma.pharmacie.pharmastock.service.impl.AuthService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Tests unitaires")
class AuthServiceTest {

    @Mock UtilisateurRepository utilisateurRepository;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock JwtUtil                jwtUtil;
    @Mock UserDetailsService     userDetailsService;
    @Mock AuthenticationManager  authenticationManager;

    @InjectMocks AuthService authService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .id(1L).nom("Alami").prenom("Fatima")
                .email("pharma@test.ma").motDePasse("hashed")
                .role(RoleUtilisateur.PHARMACIEN)
                .actif(true).tentativesEchec(0).build();
    }

    @Test
    @DisplayName("login — succès : retourne token JWT")
    void login_succes() {
        UserDetails ud = User.withUsername("pharma@test.ma")
                .password("hashed").roles("PHARMACIEN").build();

        when(utilisateurRepository.findByEmail("pharma@test.ma")).thenReturn(Optional.of(utilisateur));
        when(userDetailsService.loadUserByUsername("pharma@test.ma")).thenReturn(ud);
        when(jwtUtil.generateToken(any(), anyMap())).thenReturn("jwt-token-123");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token-456");
        when(utilisateurRepository.save(any())).thenReturn(utilisateur);
        doNothing().when(utilisateurRepository).reinitialiserTentatives(1L);

        AuthService.LoginResponse response = authService.login("pharma@test.ma", "motdepasse");

        assertThat(response.token()).isEqualTo("jwt-token-123");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.user().email()).isEqualTo("pharma@test.ma");
        assertThat(response.user().role()).isEqualTo("PHARMACIEN");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(utilisateurRepository).reinitialiserTentatives(1L);
    }

    @Test
    @DisplayName("login — compte inactif → DisabledException")
    void login_compteInactif() {
        utilisateur.setActif(false);
        when(utilisateurRepository.findByEmail("pharma@test.ma")).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> authService.login("pharma@test.ma", "mdp"))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("login — compte verrouillé (5 tentatives) → LockedException")
    void login_compteVerrouille() {
        utilisateur.setTentativesEchec(5);
        when(utilisateurRepository.findByEmail("pharma@test.ma")).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> authService.login("pharma@test.ma", "mdp"))
                .isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("login — mauvais mot de passe → incrémente tentatives")
    void login_mauvaisMotDePasse() {
        when(utilisateurRepository.findByEmail("pharma@test.ma")).thenReturn(Optional.of(utilisateur));
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());
        doNothing().when(utilisateurRepository).incrementerTentativesEchec(1L);

        assertThatThrownBy(() -> authService.login("pharma@test.ma", "mauvais"))
                .isInstanceOf(BadCredentialsException.class);

        verify(utilisateurRepository).incrementerTentativesEchec(1L);
    }

    @Test
    @DisplayName("login — email inexistant → BadCredentialsException")
    void login_emailInexistant() {
        when(utilisateurRepository.findByEmail("inconnu@test.ma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("inconnu@test.ma", "mdp"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
