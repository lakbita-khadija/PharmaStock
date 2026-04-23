package ma.pharmacie.pharmastock.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordonnances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ordonnance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ordonnance", nullable = false, unique = true, length = 50)
    private String numeroOrdonnance;

    @Column(length = 200)
    private String prescripteur;

    @Column(name = "patient_nom", length = 200)
    private String patientNom;

    @Column(name = "patient_naissance")
    private LocalDate patientNaissance;

    @Column(name = "date_prescription", nullable = false)
    private LocalDate datePrescription;

    @Column(name = "date_validite")
    private LocalDate dateValidite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validee_par_id")
    private Utilisateur valideePar;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;
}
