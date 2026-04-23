package ma.pharmacie.pharmastock.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.pharmacie.pharmastock.enums.RoleUtilisateur;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateurs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Utilisateur {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "role_utilisateur")
    private RoleUtilisateur role;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "tentatives_echec")
    private int tentativesEchec = 0;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
