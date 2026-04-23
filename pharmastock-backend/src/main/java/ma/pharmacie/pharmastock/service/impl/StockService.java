package ma.pharmacie.pharmastock.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.pharmacie.pharmastock.entity.AlerteStock;
import ma.pharmacie.pharmastock.entity.Lot;
import ma.pharmacie.pharmastock.entity.Medicament;
import ma.pharmacie.pharmastock.entity.MouvementStock;
import ma.pharmacie.pharmastock.entity.Utilisateur;
import ma.pharmacie.pharmastock.enums.NiveauAlerte;
import ma.pharmacie.pharmastock.enums.StatutAlerte;
import ma.pharmacie.pharmastock.enums.StatutLot;
import ma.pharmacie.pharmastock.enums.TypeAlerte;
import ma.pharmacie.pharmastock.enums.TypeMouvement;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.LotBloqueException;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.ResourceNotFoundException;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.StockInsuffisantException;
import ma.pharmacie.pharmastock.repository.AlerteStockRepository;
import ma.pharmacie.pharmastock.repository.LotRepository;
import ma.pharmacie.pharmastock.repository.MedicamentRepository;
import ma.pharmacie.pharmastock.repository.MouvementStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final LotRepository lotRepository;
    private final MouvementStockRepository mouvementRepository;
    private final AlerteStockRepository alerteRepository;
    private final MedicamentRepository medicamentRepository;

    /**
     * Entrée de stock (réception livraison).
     * Crée ou met à jour un lot et enregistre le mouvement.
     */
    @Transactional
    public Lot entreeStock(Medicament medicament, String numeroLot,
                           java.time.LocalDate dateExpiration,
                           java.time.LocalDate dateFabrication,
                           int quantite, String refDoc, Utilisateur operateur) {

        Lot lot = lotRepository
                .findByMedicamentIdAndStatutOrderByDateExpirationAsc(medicament.getId(), StatutLot.ACTIF)
                .stream()
                .filter(l -> l.getNumeroLot().equals(numeroLot))
                .findFirst()
                .orElseGet(() -> Lot.builder()
                        .numeroLot(numeroLot)
                        .dateExpiration(dateExpiration)
                        .dateFabrication(dateFabrication)
                        .quantiteDisponible(0)
                        .statut(StatutLot.ACTIF)
                        .medicament(medicament)
                        .build());

        int avantQty = lot.getQuantiteDisponible();
        lot.setQuantiteDisponible(avantQty + quantite);
        lot = lotRepository.save(lot);

        enregistrerMouvement(
                TypeMouvement.ENTREE,
                quantite,
                avantQty,
                lot.getQuantiteDisponible(),
                lot,
                medicament,
                refDoc,
                operateur
        );

        verifierEtResoudreAlerte(medicament);
        log.info("Entrée stock : {} x {} (lot {})", quantite, medicament.getNomCommercial(), numeroLot);
        return lot;
    }

    /**
     * Sortie de stock FEFO — utilisé lors d'une vente.
     * Retourne la liste des lots consommés.
     */
    @Transactional
    public List<LigneConsommation> sortieStockFefo(Medicament medicament, int quantiteDemandee,
                                                   String refDoc, Utilisateur operateur) {

        int stockTotal = stockDisponible(medicament.getId());
        if (stockTotal < quantiteDemandee) {
            throw new StockInsuffisantException(
                    "Stock insuffisant pour " + medicament.getNomCommercial() +
                            " (demandé : " + quantiteDemandee + ", disponible : " + stockTotal + ")."
            );
        }

        List<Lot> lotsFefo = lotRepository.findLotsDisponiblesFefo(medicament.getId());
        List<LigneConsommation> consommations = new ArrayList<>();
        int restant = quantiteDemandee;

        for (Lot lot : lotsFefo) {
            if (restant <= 0) {
                break;
            }

            if (lot.isExpire() || lot.getStatut() != StatutLot.ACTIF) {
                throw new LotBloqueException("Lot " + lot.getNumeroLot() + " est périmé ou bloqué.");
            }

            int aConsommer = Math.min(lot.getQuantiteDisponible(), restant);
            int avant = lot.getQuantiteDisponible();

            lot.setQuantiteDisponible(avant - aConsommer);
            if (lot.getQuantiteDisponible() == 0) {
                lot.setStatut(StatutLot.EPUISE);
            }

            lotRepository.save(lot);

            enregistrerMouvement(
                    TypeMouvement.SORTIE,
                    aConsommer,
                    avant,
                    lot.getQuantiteDisponible(),
                    lot,
                    medicament,
                    refDoc,
                    operateur
            );

            consommations.add(new LigneConsommation(lot, aConsommer));
            restant -= aConsommer;
        }

        verifierSeuilAlerte(medicament);
        return consommations;
    }

    public int stockDisponible(Long medicamentId) {
        Integer q = lotRepository.sumStockDisponible(medicamentId);
        return q != null ? q : 0;
    }

    @Transactional
    public void bloquerLot(Long lotId, Utilisateur operateur) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot introuvable : " + lotId));

        lot.setStatut(StatutLot.BLOQUE);
        lotRepository.save(lot);

        log.info("Lot {} bloqué par {}", lot.getNumeroLot(), operateur.getEmail());
    }

    private void enregistrerMouvement(TypeMouvement type, int quantite, int avant, int apres,
                                      Lot lot, Medicament medicament, String refDoc, Utilisateur utilisateur) {
        mouvementRepository.save(
                MouvementStock.builder()
                        .typeOperation(type)
                        .quantite(quantite)
                        .quantiteAvant(avant)
                        .quantiteApres(apres)
                        .lot(lot)
                        .medicament(medicament)
                        .referenceDoc(refDoc)
                        .utilisateur(utilisateur)
                        .build()
        );
    }

    private void verifierSeuilAlerte(Medicament medicament) {
        int stock = stockDisponible(medicament.getId());

        if (stock == 0) {
            creerAlerteSiAbsente(
                    medicament,
                    TypeAlerte.RUPTURE,
                    NiveauAlerte.CRITIQUE,
                    "RUPTURE TOTALE — " + medicament.getNomCommercial() + " : stock épuisé."
            );
        } else if (stock <= medicament.getSeuilMinimal()) {
            creerAlerteSiAbsente(
                    medicament,
                    TypeAlerte.STOCK_FAIBLE,
                    NiveauAlerte.AVERTISSEMENT,
                    "Stock faible — " + medicament.getNomCommercial() + " : " + stock + " unité(s) restante(s)."
            );
        }
    }

    private void verifierEtResoudreAlerte(Medicament medicament) {
        int stock = stockDisponible(medicament.getId());

        if (stock > medicament.getSeuilMinimal()) {
            alerteRepository.findByStatutWithRelations(StatutAlerte.ACTIVE).stream()
                    .filter(a -> a.getMedicament() != null)
                    .filter(a -> a.getMedicament().getId().equals(medicament.getId()))
                    .filter(a ->
                            a.getTypeAlerte() == TypeAlerte.STOCK_FAIBLE ||
                                    a.getTypeAlerte() == TypeAlerte.RUPTURE
                    )
                    .forEach(a -> {
                        a.setStatut(StatutAlerte.RESOLUE);
                        alerteRepository.save(a);
                    });
        }
    }

    private void creerAlerteSiAbsente(Medicament medicament, TypeAlerte type,
                                      NiveauAlerte niveau, String message) {
        boolean existe = alerteRepository.existsByMedicamentIdAndTypeAlerteAndStatut(
                medicament.getId(),
                type,
                StatutAlerte.ACTIVE
        );

        if (!existe) {
            AlerteStock alerte = AlerteStock.builder()
                    .typeAlerte(type)
                    .niveau(niveau)
                    .message(message)
                    .statut(StatutAlerte.ACTIVE)
                    .medicament(medicament)
                    .build();

            alerteRepository.save(alerte);
        }
    }

    public record LigneConsommation(Lot lot, int quantite) {}
}