package ma.pharmacie.pharmastock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.entity.Lot;
import ma.pharmacie.pharmastock.entity.MouvementStock;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.enums.StatutLot;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.ResourceNotFoundException;
import ma.pharmacie.pharmastock.repository.LotRepository;
import ma.pharmacie.pharmastock.repository.MedicamentRepository;
import ma.pharmacie.pharmastock.repository.MouvementStockRepository;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import ma.pharmacie.pharmastock.service.impl.StockService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Stock & Lots", description = "Gestion du stock et traçabilité des lots")
public class StockController {

    private final LotRepository lotRepository;
    private final MouvementStockRepository mouvementRepository;
    private final MedicamentRepository medicamentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StockService stockService;

    @GetMapping("/stock")
    @Operation(summary = "Vue globale du stock par médicament")
    public ResponseEntity<List<Map<String, Object>>> getStock(
            @RequestParam(required = false) String q) {

        if (q != null && q.isBlank()) {
            q = null;
        }

        List<Lot> lots = lotRepository.findAllActifsWithMedicament(q);

        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();

        for (Lot lot : lots) {
            Long medId = lot.getMedicament().getId();

            grouped.computeIfAbsent(medId, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("medicamentId", medId);
                m.put("nomCommercial", lot.getMedicament().getNomCommercial());
                m.put("dci", lot.getMedicament().getDci());
                m.put("seuilMinimal", lot.getMedicament().getSeuilMinimal());
                m.put("stockTotal", 0);
                m.put("nbLots", 0);
                m.put("prochainExpiration", null);
                m.put("lots", new ArrayList<Map<String, Object>>());
                return m;
            });

            Map<String, Object> medMap = grouped.get(medId);
            medMap.put("stockTotal", (int) medMap.get("stockTotal") + lot.getQuantiteDisponible());
            medMap.put("nbLots", (int) medMap.get("nbLots") + 1);

            if (medMap.get("prochainExpiration") == null) {
                medMap.put("prochainExpiration", lot.getDateExpiration());
            }

            Map<String, Object> lotMap = new LinkedHashMap<>();
            lotMap.put("id", lot.getId());
            lotMap.put("numeroLot", lot.getNumeroLot());
            lotMap.put("dateFabrication", lot.getDateFabrication());
            lotMap.put("dateExpiration", lot.getDateExpiration());
            lotMap.put("quantiteDisponible", lot.getQuantiteDisponible());
            lotMap.put("statut", lot.getStatut());

            ((List<Map<String, Object>>) medMap.get("lots")).add(lotMap);
        }

        return ResponseEntity.ok(List.copyOf(grouped.values()));
    }

    @GetMapping("/stock/{medId}/lots")
    @Operation(summary = "Lots d'un médicament")
    public ResponseEntity<List<Lot>> getLotsByMedicament(@PathVariable Long medId) {
        return ResponseEntity.ok(
                lotRepository.findByMedicamentIdAndStatutOrderByDateExpirationAsc(medId, StatutLot.ACTIF)
        );
    }

    @GetMapping("/stock/{medId}/mouvements")
    @Operation(summary = "Historique des mouvements d'un médicament")
    public ResponseEntity<List<MouvementStock>> getMouvements(@PathVariable Long medId) {
        Pageable p = PageRequest.of(0, 50);
        return ResponseEntity.ok(mouvementRepository.findByMedicamentId(medId, p));
    }

    @GetMapping("/lots")
    @Operation(summary = "Liste de tous les lots actifs")
    public ResponseEntity<List<Lot>> getAllLots(
            @RequestParam(required = false) Integer jours) {
        if (jours != null) {
            return ResponseEntity.ok(
                    lotRepository.findLotsExpirantAvant(java.time.LocalDate.now().plusDays(jours))
            );
        }
        return ResponseEntity.ok(
                List.of()
        );
    }

    @PutMapping("/lots/{lotId}/bloquer")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIEN')")
    @Operation(summary = "Bloquer un lot (rappel ou anomalie)")
    public ResponseEntity<Lot> bloquerLot(
            @PathVariable Long lotId,
            @AuthenticationPrincipal UserDetails principal) {

        Utilisateur user = utilisateurRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        stockService.bloquerLot(lotId, user);

        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot introuvable : " + lotId));
        return ResponseEntity.ok(lot);
    }
}