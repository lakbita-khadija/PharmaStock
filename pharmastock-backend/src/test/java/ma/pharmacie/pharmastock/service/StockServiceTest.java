package ma.pharmacie.pharmastock.service;

import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.*;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.*;
import ma.pharmacie.pharmastock.service.impl.StockService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests StockService — Règle FEFO")
class StockServiceTest {

    @Mock LotRepository lotRepository;
    @Mock MouvementStockRepository mouvementRepository;
    @Mock AlerteStockRepository alerteRepository;
    @Mock MedicamentRepository medicamentRepository;
    @InjectMocks StockService stockService;

    private Medicament med;
    private Utilisateur user;

    @BeforeEach void setUp() {
        med = Medicament.builder().id(1L).nomCommercial("Doliprane")
                .dci("Paracétamol").prixVenteTtc(new BigDecimal("12.50"))
                .seuilMinimal(10).actif(true).build();
        user = Utilisateur.builder().id(1L).email("test@test.com")
                .role(RoleUtilisateur.CAISSIER).build();
    }

    @Test @DisplayName("FEFO : consomme le lot le plus proche de l'expiration en premier")
    void fefo_consommeLotLePlusProche() {
        Lot l1 = lot(1L, "LOT-001", LocalDate.now().plusDays(10), 30);
        Lot l2 = lot(2L, "LOT-002", LocalDate.now().plusDays(90), 50);
        when(lotRepository.sumStockDisponible(1L)).thenReturn(80);
        when(lotRepository.findLotsDisponiblesFefo(1L)).thenReturn(List.of(l1, l2));
        when(mouvementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(alerteRepository.existsByMedicamentIdAndTypeAlerteAndStatut(any(),any(),any())).thenReturn(false);

        var result = stockService.sortieStockFefo(med, 20, "VNT-001", user);

        assertThat(result.get(0).lot().getId()).isEqualTo(1L);
        assertThat(l1.getQuantiteDisponible()).isEqualTo(10);
    }

    @Test @DisplayName("FEFO : utilise plusieurs lots si nécessaire")
    void fefo_multipleLotsSimEstInsuffisant() {
        Lot l1 = lot(1L, "LOT-001", LocalDate.now().plusDays(5), 5);
        Lot l2 = lot(2L, "LOT-002", LocalDate.now().plusDays(60), 50);
        when(lotRepository.sumStockDisponible(1L)).thenReturn(55);
        when(lotRepository.findLotsDisponiblesFefo(1L)).thenReturn(List.of(l1, l2));
        when(mouvementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(alerteRepository.existsByMedicamentIdAndTypeAlerteAndStatut(any(),any(),any())).thenReturn(false);

        var result = stockService.sortieStockFefo(med, 15, "VNT-002", user);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).quantite()).isEqualTo(5);
        assertThat(result.get(1).quantite()).isEqualTo(10);
    }

    @Test @DisplayName("Lève StockInsuffisantException si stock insuffisant")
    void fefo_stockInsuffisant() {
        when(lotRepository.sumStockDisponible(1L)).thenReturn(3);
        assertThatThrownBy(() -> stockService.sortieStockFefo(med, 10, "VNT-003", user))
                .isInstanceOf(StockInsuffisantException.class);
    }

    @Test @DisplayName("Lève LotBloqueException si lot périmé")
    void fefo_lotPerime() {
        Lot expire = lot(1L, "EXP-001", LocalDate.now().minusDays(1), 20);
        when(lotRepository.sumStockDisponible(1L)).thenReturn(20);
        when(lotRepository.findLotsDisponiblesFefo(1L)).thenReturn(List.of(expire));
        assertThatThrownBy(() -> stockService.sortieStockFefo(med, 5, "VNT-004", user))
                .isInstanceOf(LotBloqueException.class);
    }

    @Test @DisplayName("Crée une alerte STOCK_FAIBLE quand stock < seuil")
    void alerte_stockFaible() {
        Lot l = lot(1L, "LOT-001", LocalDate.now().plusDays(90), 8);
        when(lotRepository.sumStockDisponible(1L)).thenReturn(8);
        when(lotRepository.findLotsDisponiblesFefo(1L)).thenReturn(List.of(l));
        when(mouvementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(alerteRepository.existsByMedicamentIdAndTypeAlerteAndStatut(
                eq(1L), eq(TypeAlerte.STOCK_FAIBLE), eq(StatutAlerte.ACTIVE))).thenReturn(false);

        stockService.sortieStockFefo(med, 2, "VNT-005", user);

        verify(alerteRepository).save(argThat(a -> a.getTypeAlerte() == TypeAlerte.STOCK_FAIBLE));
    }

    private Lot lot(Long id, String num, LocalDate exp, int qty) {
        return Lot.builder().id(id).numeroLot(num).dateExpiration(exp)
                .quantiteDisponible(qty).statut(StatutLot.ACTIF).medicament(med).build();
    }
}
