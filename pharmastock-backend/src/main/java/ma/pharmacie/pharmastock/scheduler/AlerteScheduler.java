package ma.pharmacie.pharmastock.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.*;
import ma.pharmacie.pharmastock.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlerteScheduler {

    private final LotRepository lotRepository;
    private final MedicamentRepository medicamentRepository;
    private final AlerteStockRepository alerteRepository;

    /**
     * Vérification quotidienne à 01h00 :
     * - Lots périmés → blocage automatique + alerte
     * - Lots expirant dans 7 jours → alerte critique
     * - Lots expirant dans 30 jours → alerte avertissement
     * - Stock sous seuil minimal → alerte stock faible
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void verifierPeremptions() {
        log.info("=== Scheduler AlertePeremption démarré ===");

        LocalDate now    = LocalDate.now();
        LocalDate j7     = now.plusDays(7);
        LocalDate j30    = now.plusDays(30);

        // 1. Lots périmés → BLOQUE + alerte BLOQUANT
        List<Lot> expiresToday = lotRepository.findLotsExpires();
        for (Lot lot : expiresToday) {
            lot.setStatut(StatutLot.EXPIRE);
            lotRepository.save(lot);
            creerAlerteSiAbsente(lot.getMedicament(), lot, TypeAlerte.LOT_EXPIRE, NiveauAlerte.BLOQUANT,
                    "LOT EXPIRÉ — " + lot.getMedicament().getNomCommercial() +
                    " — Lot n°" + lot.getNumeroLot() + " expiré le " + lot.getDateExpiration());
        }

        // 2. Lots expirant dans 7 jours
        List<Lot> expiring7 = lotRepository.findLotsExpirantAvant(j7);
        for (Lot lot : expiring7) {
            if (lot.getStatut() != StatutLot.ACTIF) continue;
            creerAlerteSiAbsente(lot.getMedicament(), lot, TypeAlerte.PEREMPTION_7J, NiveauAlerte.CRITIQUE,
                    "PÉREMPTION IMMINENTE (7j) — " + lot.getMedicament().getNomCommercial() +
                    " — Lot n°" + lot.getNumeroLot() + " expire le " + lot.getDateExpiration());
        }

        // 3. Lots expirant dans 30 jours
        List<Lot> expiring30 = lotRepository.findLotsExpirantAvant(j30);
        for (Lot lot : expiring30) {
            if (lot.getStatut() != StatutLot.ACTIF) continue;
            creerAlerteSiAbsente(lot.getMedicament(), lot, TypeAlerte.PEREMPTION_30J, NiveauAlerte.AVERTISSEMENT,
                    "Péremption dans 30 jours — " + lot.getMedicament().getNomCommercial() +
                    " — Lot n°" + lot.getNumeroLot() + " expire le " + lot.getDateExpiration());
        }

        // 4. Stock faible pour tous les médicaments actifs
        List<Medicament> medicaments = medicamentRepository.findAll().stream()
                .filter(Medicament::isActif).toList();

        for (Medicament med : medicaments) {
            Integer stock = lotRepository.sumStockDisponible(med.getId());
            int stockTotal = stock != null ? stock : 0;

            if (stockTotal == 0) {
                creerAlerteSiAbsente(med, null, TypeAlerte.RUPTURE, NiveauAlerte.CRITIQUE,
                        "RUPTURE DE STOCK — " + med.getNomCommercial() + " : aucune unité disponible.");
            } else if (stockTotal <= med.getSeuilMinimal()) {
                creerAlerteSiAbsente(med, null, TypeAlerte.STOCK_FAIBLE, NiveauAlerte.AVERTISSEMENT,
                        "Stock faible — " + med.getNomCommercial() +
                        " : " + stockTotal + " unité(s) (seuil : " + med.getSeuilMinimal() + ").");
            }
        }

        log.info("=== Scheduler AlertePeremption terminé — {} lots traités ===", expiresToday.size());
    }

    private void creerAlerteSiAbsente(Medicament medicament, Lot lot,
                                       TypeAlerte type, NiveauAlerte niveau, String message) {
        boolean existe = alerteRepository.existsByMedicamentIdAndTypeAlerteAndStatut(
                medicament.getId(), type, StatutAlerte.ACTIVE);
        if (!existe) {
            alerteRepository.save(AlerteStock.builder()
                    .typeAlerte(type).niveau(niveau).message(message)
                    .statut(StatutAlerte.ACTIVE)
                    .medicament(medicament)
                    .lot(lot)
                    .build());
            log.info("Alerte créée : {} — {}", type, medicament.getNomCommercial());
        }
    }
}
