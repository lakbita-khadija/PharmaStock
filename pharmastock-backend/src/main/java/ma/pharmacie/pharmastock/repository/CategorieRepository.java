package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    List<Categorie> findAllByOrderByNomAsc();
    boolean existsByNom(String nom);
}
