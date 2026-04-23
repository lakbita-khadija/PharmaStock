package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.Medicament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MedicamentRepository extends JpaRepository<Medicament, Long> {

    @Query("""
        SELECT m FROM Medicament m
        LEFT JOIN FETCH m.categorie
        LEFT JOIN FETCH m.fournisseur
        WHERE m.actif = true
        AND (:q IS NULL OR LOWER(m.nomCommercial) LIKE LOWER(CONCAT('%',:q,'%'))
             OR LOWER(m.dci) LIKE LOWER(CONCAT('%',:q,'%'))
             OR m.codeBarre = :q)
        AND (:categorieId IS NULL OR m.categorie.id = :categorieId)
    """)
    Page<Medicament> searchActifs(@Param("q") String q,
                                   @Param("categorieId") Long categorieId,
                                   Pageable pageable);

    @Query("SELECT m FROM Medicament m WHERE m.codeBarre = :codeBarre AND m.actif = true")
    Optional<Medicament> findByCodeBarre(@Param("codeBarre") String codeBarre);

    @Query("""
        SELECT m FROM Medicament m
        WHERE m.actif = true
        AND (LOWER(m.nomCommercial) LIKE LOWER(CONCAT('%',:q,'%'))
             OR LOWER(m.dci) LIKE LOWER(CONCAT('%',:q,'%'))
             OR m.codeBarre = :q)
    """)
    java.util.List<Medicament> searchForSale(@Param("q") String q, Pageable pageable);
}
