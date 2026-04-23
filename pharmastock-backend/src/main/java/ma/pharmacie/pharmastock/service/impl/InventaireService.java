package ma.pharmacie.pharmastock.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.*;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventaireService {

    private final LotRepository          lotRepository;
    private final MouvementStockRepository mouvementRepository;
    private final AuditLogRepository      auditLogRepository;

    // Tables inventaire — stockées en mémoire de session (simplifié)
    // En production : utiliser des entités Inventaire et LigneInventaire en BDD
    private final Map<Long, InventaireSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private long nextId = 1L;

    // ── DTO ──
    public record InventaireSession(
            Long id, String type, String responsable, String commentaire,
            String statut, LocalDateTime dateDebut, LocalDateTime dateValidation,
            List<LigneInventaireDto> lignes
    ) {}

    public record LigneInventaireDto(
            Long lotId, String numeroLot, String nomMedicament,
            int quantiteTheorique, Integer quantitePhysique
    ) {}

    public record EcartDto(Long lotId, String nomMedicament, String numeroLot,
                           int theorique, int physique, int ecart) {}

    // ── Démarrer un inventaire ──
    @Transactional(readOnly = true)
    public InventaireSession demarrer(String type, String responsable, String commentaire) {
        List<Lot> lots = lotRepository.findAllActifsWithMedicament(null);

        List<LigneInventaireDto> lignes = lots.stream()
                .filter(l -> l.getStatut() == StatutLot.ACTIF)
                .map(l -> new LigneInventaireDto(
                        l.getId(),
                        l.getNumeroLot(),
                        l.getMedicament().getNomCommercial(),
                        l.getQuantiteDisponible(),
                        null  // à saisir
                ))
                .toList();

        long id = nextId++;
        InventaireSession session = new InventaireSession(
                id, type, responsable, commentaire,
                "EN_COURS", LocalDateTime.now(), null, new ArrayList<>(lignes)
        );
        sessions.put(id, session);
        log.info("Inventaire {} démarré — {} lots à compter", id, lignes.size());
        return session;
    }

    // ── Saisir les quantités physiques ──
    @Transactional
    public InventaireSession saisirLignes(Long inventaireId, List<Map<String, Object>> saisies) {
        InventaireSession session = getSession(inventaireId);

        List<LigneInventaireDto> mises = session.lignes().stream()
                .map(l -> {
                    Optional<Map<String, Object>> saisie = saisies.stream()
                            .filter(s -> Objects.equals(s.get("lotId"), l.lotId()))
                            .findFirst();
                    int physique = saisie.map(s -> (Integer) s.get("quantitePhysique"))
                            .orElse(l.quantitePhysique());
                    return new LigneInventaireDto(l.lotId(), l.numeroLot(),
                            l.nomMedicament(), l.quantiteTheorique(), physique);
                }).toList();

        InventaireSession updated = new InventaireSession(
                session.id(), session.type(), session.responsable(), session.commentaire(),
                "EN_COURS", session.dateDebut(), null, new ArrayList<>(mises)
        );
        sessions.put(inventaireId, updated);
        return updated;
    }

    // ── Calculer les écarts ──
    public List<EcartDto> calculerEcarts(Long inventaireId) {
        InventaireSession session = getSession(inventaireId);
        return session.lignes().stream()
                .filter(l -> l.quantitePhysique() != null && l.quantitePhysique() != l.quantiteTheorique())
                .map(l -> new EcartDto(l.lotId(), l.nomMedicament(), l.numeroLot(),
                        l.quantiteTheorique(), l.quantitePhysique(),
                        l.quantitePhysique() - l.quantiteTheorique()))
                .toList();
    }

    // ── Valider et régulariser le stock ──
    @Transactional
    public InventaireSession valider(Long inventaireId, Utilisateur validateur) {
        InventaireSession session = getSession(inventaireId);

        int nbEcarts = 0;
        for (LigneInventaireDto ligne : session.lignes()) {
            if (ligne.quantitePhysique() == null) continue;

            Lot lot = lotRepository.findById(ligne.lotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lot introuvable : " + ligne.lotId()));

            int ecart = ligne.quantitePhysique() - ligne.quantiteTheorique();
            if (ecart == 0) continue;

            nbEcarts++;
            int avant = lot.getQuantiteDisponible();
            lot.setQuantiteDisponible(ligne.quantitePhysique());
            if (lot.getQuantiteDisponible() == 0) lot.setStatut(StatutLot.EPUISE);
            lotRepository.save(lot);

            // Enregistrer le mouvement d'ajustement
            mouvementRepository.save(MouvementStock.builder()
                    .typeOperation(TypeMouvement.AJUSTEMENT_INV)
                    .quantite(Math.abs(ecart))
                    .quantiteAvant(avant)
                    .quantiteApres(ligne.quantitePhysique())
                    .lot(lot)
                    .medicament(lot.getMedicament())
                    .utilisateur(validateur)
                    .referenceDoc("INV-" + inventaireId)
                    .commentaire("Régularisation inventaire #" + inventaireId)
                    .build());
        }

        // Audit
        auditLogRepository.save(AuditLog.builder()
                .action("VALIDER_INVENTAIRE")
                .entite("Inventaire")
                .idEntite(inventaireId)
                .nouvelleValeur("VALIDE — " + nbEcarts + " écart(s) régularisé(s)")
                .utilisateur(validateur)
                .build());

        InventaireSession validated = new InventaireSession(
                session.id(), session.type(), session.responsable(), session.commentaire(),
                "VALIDE", session.dateDebut(), LocalDateTime.now(), session.lignes()
        );
        sessions.put(inventaireId, validated);
        log.info("Inventaire {} validé — {} écarts régularisés", inventaireId, nbEcarts);
        return validated;
    }

    public List<InventaireSession> listerTous() {
        return new ArrayList<>(sessions.values());
    }

    private InventaireSession getSession(Long id) {
        InventaireSession s = sessions.get(id);
        if (s == null) throw new ResourceNotFoundException("Inventaire introuvable : " + id);
        return s;
    }
}
