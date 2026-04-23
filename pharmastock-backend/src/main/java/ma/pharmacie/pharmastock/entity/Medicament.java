package ma.pharmacie.pharmastock.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.pharmacie.pharmastock.enums.StatutDispensation;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicaments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Medicament {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_commercial", nullable = false, length = 200)
    private String nomCommercial;

    @Column(nullable = false, length = 200)
    private String dci;

    @Column(nullable = false, length = 100)
    private String formegalenique;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(name = "code_barre", unique = true, length = 30)
    private String codeBarre;

    @Column(name = "code_atc", length = 20)
    private String codeAtc;

    @Column(name = "prix_achat_ht", precision = 10, scale = 2)
    private BigDecimal prixAchatHt;

    @Column(name = "prix_vente_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixVenteTtc;

    @Column(name = "seuil_minimal", nullable = false)
    private Integer seuilMinimal = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_dispensation", columnDefinition = "statut_dispensation")
    private StatutDispensation statutDispensation = StatutDispensation.LIBRE;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}
