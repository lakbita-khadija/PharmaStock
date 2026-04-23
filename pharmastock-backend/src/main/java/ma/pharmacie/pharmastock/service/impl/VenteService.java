package ma.pharmacie.pharmastock.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.*;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenteService {

    private final VenteRepository venteRepository;
    private final MedicamentRepository medicamentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StockService stockService;
    private final AuditLogRepository auditLogRepository;

    public record LigneVenteRequest(Long medicamentId, Integer quantite, Double remise) {}
    public record VenteRequest(List<LigneVenteRequest> lignes, String modePaiement, Double montantDonne) {}

    @Transactional
    public Vente creerVente(VenteRequest request, Utilisateur caissier) {

        if (request.lignes() == null || request.lignes().isEmpty()) {
            throw new BusinessException("Le panier est vide.");
        }

        Vente vente = Vente.builder()
                .numeroVente(genererNumeroVente())
                .dateVente(LocalDateTime.now())
                .modePaiement(ModePaiement.valueOf(request.modePaiement()))
                .statut(StatutVente.VALIDEE)
                .caissier(caissier)
                .totalTtc(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (LigneVenteRequest ligneReq : request.lignes()) {
            Medicament medicament = medicamentRepository.findById(ligneReq.medicamentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable : " + ligneReq.medicamentId()));

            if (medicament.getStatutDispensation() != StatutDispensation.LIBRE && vente.getOrdonnance() == null) {
                log.warn("Tentative de vente sans ordonnance : {}", medicament.getNomCommercial());
            }

            List<StockService.LigneConsommation> consommations =
                    stockService.sortieStockFefo(
                            medicament,
                            ligneReq.quantite(),
                            "VENTE-" + vente.getNumeroVente(),
                            caissier
                    );

            BigDecimal remise = BigDecimal.valueOf(ligneReq.remise() != null ? ligneReq.remise() : 0.0);
            BigDecimal prixUnit = medicament.getPrixVenteTtc();

            for (StockService.LigneConsommation conso : consommations) {
                BigDecimal sousTotal = prixUnit
                        .multiply(BigDecimal.valueOf(conso.quantite()))
                        .multiply(BigDecimal.ONE.subtract(
                                remise.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        ));

                LigneVente ligne = LigneVente.builder()
                        .vente(vente)
                        .lot(conso.lot())
                        .medicament(medicament)
                        .quantiteVendue(conso.quantite())
                        .prixUnitaire(prixUnit)
                        .remisePct(remise)
                        .sousTotal(sousTotal)
                        .build();

                vente.getLignes().add(ligne);
                total = total.add(sousTotal);
            }
        }

        vente.setTotalTtc(total.setScale(2, RoundingMode.HALF_UP));

        if (request.montantDonne() != null) {
            BigDecimal montant = BigDecimal.valueOf(request.montantDonne());
            vente.setMontantDonne(montant);
            vente.setRenduMonnaie(montant.subtract(total).setScale(2, RoundingMode.HALF_UP));
        }

        vente = venteRepository.save(vente);

        enregistrerAudit(
                "CREATE_VENTE",
                "Vente",
                vente.getId(),
                null,
                "Vente " + vente.getNumeroVente() + " — Total : " + total + " DH",
                caissier
        );

        log.info("Vente {} créée — {} DH", vente.getNumeroVente(), total);
        return vente;
    }

    @Transactional
    public Vente annulerVente(Long venteId, String motif, Utilisateur pharmacien) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente introuvable : " + venteId));

        if (vente.getStatut() == StatutVente.ANNULEE) {
            throw new BusinessException("Cette vente est déjà annulée.");
        }

        vente.setStatut(StatutVente.ANNULEE);
        vente.setMotifAnnulation(motif);
        vente.setAnnulePar(pharmacien);
        vente.setDateAnnulation(LocalDateTime.now());

        for (LigneVente ligne : vente.getLignes()) {
            Lot lot = ligne.getLot();
            lot.setQuantiteDisponible(lot.getQuantiteDisponible() + ligne.getQuantiteVendue());
            if (lot.getStatut() == StatutLot.EPUISE) {
                lot.setStatut(StatutLot.ACTIF);
            }
        }

        enregistrerAudit(
                "CANCEL_VENTE",
                "Vente",
                venteId,
                "VALIDEE",
                "ANNULEE — " + motif,
                pharmacien
        );

        return venteRepository.save(vente);
    }

    public Page<Vente> listerVentes(String q, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (q != null && q.isBlank()) {
            q = null;
        }

        if (from != null && to != null) {
            return venteRepository.searchByQAndBetween(q, from, to, pageable);
        }
        if (from != null) {
            return venteRepository.searchByQAndFrom(q, from, pageable);
        }
        if (to != null) {
            return venteRepository.searchByQAndTo(q, to, pageable);
        }
        return venteRepository.searchByQAndBetween(q, from, to, pageable);
    }

    public Vente findById(Long id) {
        return venteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente introuvable : " + id));
    }

    private String genererNumeroVente() {
        String prefix = "VNT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = venteRepository.count() + 1;
        return prefix + "-" + String.format("%05d", count);
    }

    private void enregistrerAudit(String action, String entite, Long idEntite,
                                  String ancienne, String nouvelle, Utilisateur user) {
        auditLogRepository.save(
                AuditLog.builder()
                        .action(action)
                        .entite(entite)
                        .idEntite(idEntite)
                        .ancienneValeur(ancienne)
                        .nouvelleValeur(nouvelle)
                        .utilisateur(user)
                        .build()
        );
    }
}