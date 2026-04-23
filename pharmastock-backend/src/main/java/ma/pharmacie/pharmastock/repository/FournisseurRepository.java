package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    @Query("SELECT f FROM Fournisseur f WHERE f.actif = true AND (:q IS NULL OR LOWER(f.nom) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Fournisseur> searchActifs(@Param("q") String q, Pageable pageable);

    java.util.List<Fournisseur> findByActifTrue();
}
