package ma.pharmacie.pharmastock.service;

import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.*;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.*;
import ma.pharmacie.pharmastock.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VenteService — Tests unitaires")
class VenteServiceTest {

    @Mock VenteRepository      venteRepository;
    @Mock MedicamentRepository medicamentRepository;
    @Mock UtilisateurRepository utilisateurRepository;
    @Mock StockService          stockService;
    @Mock AuditLogRepository    auditLogRepository;

    @InjectMocks VenteService venteService;

    private Medicament med;
    private Utilisateur caissier;
    private Lot lot;

    @BeforeEach
    void setUp() {
        med = Medicament.builder().id(1L)
                .nomCommercial("Doliprane").dci("Paracétamol")
                .statutDispensation(StatutDispensation.LIBRE)
                .prixVenteTtc(BigDecimal.valueOf(12.50))
                .seuilMinimal(10).actif(true).build();

        caissier = Utilisateur.builder().id(1L)
                .email("caissier@pharma.ma").nom("C").prenom("C")
                .role(RoleUtilisateur.CAISSIER).build();

        lot = Lot.builder().id(1L).numeroLot("LOT-A")
                .dateExpiration(LocalDate.now().plusDays(90))
                .quantiteDisponible(5).statut(StatutLot.ACTIF)
                .medicament(med).build();
    }

    @Test
    @DisplayName("creerVente — crée une vente et décrémente le stock")
    void creerVente_ok() {
        VenteService.VenteRequest req = new VenteService.VenteRequest(
                List.of(new VenteService.LigneVenteRequest(1L, 2, 0.0)),
                "ESPECES", 30.0
        );

        when(medicamentRepository.findById(1L)).thenReturn(Optional.of(med));
        when(stockService.sortieStockFefo(eq(med), eq(2), anyString(), eq(caissier)))
                .thenReturn(List.of(new StockService.LigneConsommation(lot, 2)));
        when(venteRepository.save(any(Vente.class))).thenAnswer(i -> {
            Vente v = i.getArgument(0);
            v = Vente.builder().id(1L).numeroVente("VNT-20240101-00001")
                    .totalTtc(BigDecimal.valueOf(25.00))
                    .statut(StatutVente.VALIDEE)
                    .caissier(caissier)
                    .lignes(v.getLignes())
                    .build();
            return v;
        });
        when(venteRepository.count()).thenReturn(0L);
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Vente result = venteService.creerVente(req, caissier);

        assertThat(result).isNotNull();
        assertThat(result.getStatut()).isEqualTo(StatutVente.VALIDEE);
        verify(stockService).sortieStockFefo(eq(med), eq(2), anyString(), eq(caissier));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("creerVente — lève BusinessException si panier vide")
    void creerVente_panierVide() {
        VenteService.VenteRequest req = new VenteService.VenteRequest(
                List.of(), "ESPECES", null
        );
        assertThatThrownBy(() -> venteService.creerVente(req, caissier))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vide");
    }

    @Test
    @DisplayName("creerVente — propage StockInsuffisantException du StockService")
    void creerVente_stockInsuffisant() {
        VenteService.VenteRequest req = new VenteService.VenteRequest(
                List.of(new VenteService.LigneVenteRequest(1L, 100, 0.0)),
                "ESPECES", null
        );
        when(medicamentRepository.findById(1L)).thenReturn(Optional.of(med));
        when(stockService.sortieStockFefo(any(), anyInt(), any(), any()))
                .thenThrow(new StockInsuffisantException("Stock insuffisant"));
        when(venteRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> venteService.creerVente(req, caissier))
                .isInstanceOf(StockInsuffisantException.class);
    }

    @Test
    @DisplayName("annulerVente — passe le statut à ANNULEE et restitue le stock")
    void annulerVente_ok() {
        LigneVente ligne = LigneVente.builder()
                .lot(lot).medicament(med).quantiteVendue(3)
                .prixUnitaire(BigDecimal.valueOf(12.50))
                .remisePct(BigDecimal.ZERO)
                .sousTotal(BigDecimal.valueOf(37.50)).build();

        Vente vente = Vente.builder().id(1L).numeroVente("VNT-001")
                .statut(StatutVente.VALIDEE).caissier(caissier)
                .totalTtc(BigDecimal.valueOf(37.50))
                .lignes(List.of(ligne)).build();

        Utilisateur pharmacien = Utilisateur.builder().id(2L)
                .email("pharma@pharma.ma").role(RoleUtilisateur.PHARMACIEN).build();

        when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
        when(venteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int stockAvant = lot.getQuantiteDisponible(); // 5
        Vente result = venteService.annulerVente(1L, "Erreur de saisie", pharmacien);

        assertThat(result.getStatut()).isEqualTo(StatutVente.ANNULEE);
        assertThat(result.getMotifAnnulation()).isEqualTo("Erreur de saisie");
        // Stock restitué : 5 + 3 = 8
        assertThat(lot.getQuantiteDisponible()).isEqualTo(stockAvant + 3);
    }

    @Test
    @DisplayName("annulerVente — lève BusinessException si déjà annulée")
    void annulerVente_dejaAnnulee() {
        Vente vente = Vente.builder().id(1L).statut(StatutVente.ANNULEE)
                .lignes(List.of()).totalTtc(BigDecimal.ZERO).build();

        Utilisateur pharmacien = Utilisateur.builder().id(2L)
                .role(RoleUtilisateur.PHARMACIEN).build();

        when(venteRepository.findById(1L)).thenReturn(Optional.of(vente));
        assertThatThrownBy(() -> venteService.annulerVente(1L, "test", pharmacien))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà annulée");
    }
}
