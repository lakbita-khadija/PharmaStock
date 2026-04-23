package ma.pharmacie.pharmastock.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.pharmacie.pharmastock.entity.*;
import ma.pharmacie.pharmastock.enums.StatutDispensation;
import ma.pharmacie.pharmastock.exception.GlobalExceptionHandler.*;
import ma.pharmacie.pharmastock.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicamentService {

    private final MedicamentRepository medicamentRepository;
    private final CategorieRepository  categorieRepository;
    private final FournisseurRepository fournisseurRepository;
    private final AuditLogRepository   auditLogRepository;

    // ── Lister avec pagination et recherche ──
    public Page<Medicament> lister(String q, Long categorieId, Pageable pageable) {
        return medicamentRepository.searchActifs(q, categorieId, pageable);
    }

    // ── Recherche rapide pour la caisse ──
    public List<Medicament> rechercher(String q) {
        return medicamentRepository.searchForSale(q, PageRequest.of(0, 10));
    }

    // ── Trouver par ID ──
    public Medicament findById(Long id) {
        return medicamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médicament introuvable : " + id));
    }

    // ── Trouver par code-barres ──
    public Medicament findByCodeBarre(String code) {
        return medicamentRepository.findByCodeBarre(code)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun médicament avec le code : " + code));
    }

    // ── Créer ──
    @Transactional
    public Medicament creer(MedicamentRequest req, Utilisateur createur) {
        Medicament med = buildEntity(req, new Medicament());
        med = medicamentRepository.save(med);
        audit("CREATE_MEDICAMENT", "Medicament", med.getId(), null, med.getNomCommercial(), createur);
        log.info("Médicament créé : {} par {}", med.getNomCommercial(), createur.getEmail());
        return med;
    }

    // ── Modifier ──
    @Transactional
    public Medicament modifier(Long id, MedicamentRequest req, Utilisateur modificateur) {
        Medicament med = findById(id);
        String avant = med.getNomCommercial();
        buildEntity(req, med);
        med = medicamentRepository.save(med);
        audit("UPDATE_MEDICAMENT", "Medicament", id, avant, med.getNomCommercial(), modificateur);
        return med;
    }

    // ── Archiver (soft delete) ──
    @Transactional
    public void archiver(Long id, Utilisateur utilisateur) {
        Medicament med = findById(id);
        med.setActif(false);
        medicamentRepository.save(med);
        audit("ARCHIVE_MEDICAMENT", "Medicament", id, "ACTIF", "INACTIF", utilisateur);
        log.info("Médicament archivé : {} par {}", med.getNomCommercial(), utilisateur.getEmail());
    }

    // ── Privé ──
    private Medicament buildEntity(MedicamentRequest req, Medicament med) {
        med.setNomCommercial(req.nomCommercial());
        med.setDci(req.dci());
        med.setFormegalenique(req.formegalenique());
        med.setDosage(req.dosage());
        med.setCodeBarre(req.codeBarre());
        med.setPrixAchatHt(req.prixAchatHt());
        med.setPrixVenteTtc(req.prixVenteTtc());
        med.setSeuilMinimal(req.seuilMinimal() != null ? req.seuilMinimal() : 10);
        med.setActif(req.actif() != null ? req.actif() : true);

        if (req.statutDispensation() != null) {
            med.setStatutDispensation(StatutDispensation.valueOf(req.statutDispensation()));
        }
        if (req.categorieId() != null) {
            categorieRepository.findById(req.categorieId()).ifPresent(med::setCategorie);
        }
        if (req.fournisseurId() != null) {
            fournisseurRepository.findById(req.fournisseurId()).ifPresent(med::setFournisseur);
        }
        return med;
    }

    private void audit(String action, String entite, Long id, String avant, String apres, Utilisateur user) {
        auditLogRepository.save(AuditLog.builder()
                .action(action).entite(entite).idEntite(id)
                .ancienneValeur(avant).nouvelleValeur(apres)
                .utilisateur(user).build());
    }

    // ── DTO interne ──
    public record MedicamentRequest(
            String nomCommercial, String dci, String formegalenique, String dosage,
            String codeBarre, Long categorieId, Long fournisseurId,
            BigDecimal prixAchatHt, BigDecimal prixVenteTtc,
            Integer seuilMinimal, String statutDispensation, Boolean actif
    ) {}
}
