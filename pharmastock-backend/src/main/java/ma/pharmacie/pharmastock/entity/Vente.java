package ma.pharmacie.pharmastock.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.pharmacie.pharmastock.enums.ModePaiement;
import ma.pharmacie.pharmastock.enums.StatutVente;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vente {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_vente", nullable = false, unique = true, length = 30)
    private String numeroVente;

    @Column(name = "date_vente", nullable = false)
    private LocalDateTime dateVente;

    @Column(name = "total_ttc", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTtc;

    @Column(name = "montant_donne", precision = 12, scale = 2)
    private BigDecimal montantDonne;

    @Column(name = "rendu_monnaie", precision = 12, scale = 2)
    private BigDecimal renduMonnaie;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", columnDefinition = "mode_paiement")
    private ModePaiement modePaiement = ModePaiement.ESPECES;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "statut_vente")
    private StatutVente statut = StatutVente.VALIDEE;

    @Column(name = "motif_annulation", columnDefinition = "TEXT")
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caissier_id", nullable = false)
    private Utilisateur caissier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordonnance_id")
    private Ordonnance ordonnance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par_id")
    private Utilisateur annulePar;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LigneVente> lignes = new ArrayList<>();
}
