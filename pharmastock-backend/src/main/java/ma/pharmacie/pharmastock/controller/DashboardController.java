package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Lot;
import ma.pharmacie.pharmastock.enums.StatutLot;
import ma.pharmacie.pharmastock.repository.AlerteStockRepository;
import ma.pharmacie.pharmastock.repository.LotRepository;
import ma.pharmacie.pharmastock.repository.MedicamentRepository;
import ma.pharmacie.pharmastock.repository.VenteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "KPIs et données analytiques")
public class DashboardController {

    private final VenteRepository venteRepository;
    private final MedicamentRepository medicamentRepository;
    private final AlerteStockRepository alerteRepository;
    private final LotRepository lotRepository;

    @GetMapping("/kpis")
    @Operation(summary = "KPIs principaux du tableau de bord")
    public ResponseEntity<Map<String, Object>> getKpis() {
        LocalDate today = LocalDate.now();

        LocalDateTime debutJour = today.atStartOfDay();
        LocalDateTime finJour = today.atTime(23, 59, 59);

        LocalDateTime debutMois = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime debutMoisPrecedent = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMoisPrecedent = debutMois.minusSeconds(1);

        BigDecimal caJour = orZero(venteRepository.sumCa(debutJour, finJour));
        BigDecimal caMois = orZero(venteRepository.sumCa(debutMois, finJour));
        BigDecimal caMoisPrecedent = orZero(venteRepository.sumCa(debutMoisPrecedent, finMoisPrecedent));

        long ventesJour = venteRepository.countVentes(debutJour, finJour);
        long alertesActives = alerteRepository.countActives();
        long alertesCritiques = alerteRepository.countCritiques();
        long nbMedicaments = medicamentRepository.count();

        List<Lot> lots = lotRepository.findAllWithMedicament();

        BigDecimal valeurStock = lots.stream()
                .filter(l -> l.getStatut() == StatutLot.ACTIF)
                .filter(l -> l.getMedicament() != null)
                .filter(l -> l.getMedicament().getPrixVenteTtc() != null)
                .filter(l -> l.getQuantiteDisponible() != null)
                .map(l -> l.getMedicament()
                        .getPrixVenteTtc()
                        .multiply(BigDecimal.valueOf(l.getQuantiteDisponible())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double evoCaMois = 0.0;
        if (caMoisPrecedent.compareTo(BigDecimal.ZERO) > 0) {
            evoCaMois = caMois.subtract(caMoisPrecedent)
                    .divide(caMoisPrecedent, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("caJour", caJour);
        kpis.put("caMois", caMois);
        kpis.put("evoCaMois", Math.round(evoCaMois * 10.0) / 10.0);
        kpis.put("ventesJour", ventesJour);
        kpis.put("alertesActives", alertesActives);
        kpis.put("alertesCritiques", alertesCritiques);
        kpis.put("nbMedicaments", nbMedicaments);
        kpis.put("valeurStock", valeurStock);

        return ResponseEntity.ok(kpis);
    }

    @GetMapping("/ventes-chart")
    @Operation(summary = "Données pour le graphique ventes des 30 derniers jours")
    public ResponseEntity<List<Map<String, Object>>> getVentesChart(
            @RequestParam(defaultValue = "month") String period) {

        List<Map<String, Object>> data = new ArrayList<>();
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(29);

        for (int i = 0; i < 30; i++) {
            LocalDateTime dayStart = start.plusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1).minusSeconds(1);
            BigDecimal total = orZero(venteRepository.sumCa(dayStart, dayEnd));

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("jour", dayStart.format(DateTimeFormatter.ofPattern("dd/MM")));
            point.put("total", total);
            data.add(point);
        }

        return ResponseEntity.ok(data);
    }

    @GetMapping("/top-medicaments")
    @Operation(summary = "Top 10 médicaments par chiffre d'affaires du mois")
    public ResponseEntity<List<Map<String, Object>>> getTopMedicaments() {
        return ResponseEntity.ok(List.of());
    }

    private BigDecimal orZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}