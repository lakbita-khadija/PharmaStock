package ma.pharmacie.pharmastock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.enums.RoleUtilisateur;
import ma.pharmacie.pharmastock.repository.*;
import ma.pharmacie.pharmastock.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("MedicamentController — Tests d'intégration")
class MedicamentControllerIntegrationTest {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @Autowired JwtUtil        jwtUtil;
    @Autowired UtilisateurRepository utilisateurRepository;

    private String tokenAdmin;
    private String tokenCaissier;

    @BeforeEach
    void setUp() {
        // Créer un token admin de test
        tokenAdmin = jwtUtil.generateToken(
                User.withUsername("admin@test.ma")
                    .password("x")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build(),
                Map.of("role", "ADMIN", "userId", 1L)
        );

        tokenCaissier = jwtUtil.generateToken(
                User.withUsername("caissier@test.ma")
                    .password("x")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_CAISSIER")))
                    .build(),
                Map.of("role", "CAISSIER", "userId", 2L)
        );
    }

    @Test
    @DisplayName("GET /medicaments — retourne 200 et liste paginée")
    void getMedicaments_ok() throws Exception {
        mockMvc.perform(get("/api/v1/medicaments")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /medicaments — retourne 401 sans token")
    void getMedicaments_sansToken() throws Exception {
        mockMvc.perform(get("/api/v1/medicaments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /medicaments — retourne 403 pour CAISSIER")
    void createMedicament_forbidden_caissier() throws Exception {
        Map<String, Object> body = Map.of(
                "nomCommercial", "TestMed",
                "dci", "DCI Test",
                "formegalenique", "Comprimé",
                "dosage", "100mg",
                "prixVenteTtc", 10.0,
                "statutDispensation", "LIBRE"
        );

        mockMvc.perform(post("/api/v1/medicaments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /medicaments — crée et retourne 201 pour ADMIN")
    void createMedicament_ok_admin() throws Exception {
        Map<String, Object> body = Map.of(
                "nomCommercial", "TestMed",
                "dci", "Ibuprofène Test",
                "formegalenique", "Comprimé",
                "dosage", "200mg",
                "prixVenteTtc", 15.0,
                "seuilMinimal", 5,
                "statutDispensation", "LIBRE",
                "actif", true
        );

        mockMvc.perform(post("/api/v1/medicaments")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomCommercial").value("TestMed"))
                .andExpect(jsonPath("$.dci").value("Ibuprofène Test"));
    }

    @Test
    @DisplayName("GET /medicaments/search — recherche par nom")
    void searchMedicaments_ok() throws Exception {
        mockMvc.perform(get("/api/v1/medicaments/search")
                        .param("q", "Doliprane")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("DELETE /medicaments/{id} — archive le médicament")
    void archiveMedicament_ok() throws Exception {
        // D'abord créer un médicament
        Map<String, Object> body = Map.of(
                "nomCommercial", "A Supprimer",
                "dci", "DCI Delete",
                "formegalenique", "Gélule",
                "dosage", "50mg",
                "prixVenteTtc", 8.0,
                "statutDispensation", "LIBRE"
        );

        String responseBody = mockMvc.perform(post("/api/v1/medicaments")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
        Long id = Long.valueOf(map.get("id").toString());

        mockMvc.perform(delete("/api/v1/medicaments/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
    }
}
