package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.CommandeFournisseur;
import ma.pharmacie.pharmastock.enums.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommandeFournisseurRepository extends JpaRepository<CommandeFournisseur, Long> {

    @Query("SELECT c FROM CommandeFournisseur c LEFT JOIN FETCH c.fournisseur LEFT JOIN FETCH c.createur ORDER BY c.dateCreation DESC")
    List<CommandeFournisseur> findAllWithDetails();

    List<CommandeFournisseur> findByStatutOrderByDateCreationDesc(StatutCommande statut);

    boolean existsByNumeroCommande(String numeroCommande);
}
