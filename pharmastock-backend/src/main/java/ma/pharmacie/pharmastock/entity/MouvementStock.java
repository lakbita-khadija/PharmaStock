package ma.pharmacie.pharmastock.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.pharmacie.pharmastock.enums.TypeMouvement;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MouvementStock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", columnDefinition = "type_mouvement", nullable = false)
    private TypeMouvement typeOperation;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "quantite_avant", nullable = false)
    private Integer quantiteAvant;

    @Column(name = "quantite_apres", nullable = false)
    private Integer quantiteApres;

    @Column(name = "reference_doc", length = 50)
    private String referenceDoc;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @CreationTimestamp
    @Column(name = "date_operation", updatable = false)
    private LocalDateTime dateOperation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicament_id", nullable = false)
    private Medicament medicament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
}
