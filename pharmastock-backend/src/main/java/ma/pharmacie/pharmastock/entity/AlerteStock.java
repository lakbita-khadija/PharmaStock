package ma.pharmacie.pharmastock.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.pharmacie.pharmastock.enums.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertes_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_alerte", length = 50, nullable = false)
    private TypeAlerte typeAlerte;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", length = 50, nullable = false)
    private NiveauAlerte niveau;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 50, nullable = false)
    private StatutAlerte statut = StatutAlerte.ACTIVE;

    @Column(name = "commentaire_acquittement", columnDefinition = "TEXT")
    private String commentaireAquittement;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_acquittement")
    private LocalDateTime dateAquittement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicament_id")
    private Medicament medicament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquitte_par_id")
    private Utilisateur acquittePar;
}