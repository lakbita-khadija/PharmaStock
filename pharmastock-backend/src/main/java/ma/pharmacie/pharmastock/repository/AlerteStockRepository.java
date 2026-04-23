package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.AlerteStock;
import ma.pharmacie.pharmastock.enums.NiveauAlerte;
import ma.pharmacie.pharmastock.enums.StatutAlerte;
import ma.pharmacie.pharmastock.enums.TypeAlerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlerteStockRepository extends JpaRepository<AlerteStock, Long> {

    List<AlerteStock> findByStatutOrderByDateCreationDesc(StatutAlerte statut);

    @Query("""
        SELECT COUNT(a)
        FROM AlerteStock a
        WHERE a.statut = 'ACTIVE'
    """)
    Long countActives();

    @Query("""
        SELECT COUNT(a)
        FROM AlerteStock a
        WHERE a.statut = 'ACTIVE'
          AND a.niveau IN ('CRITIQUE', 'BLOQUANT')
    """)
    Long countCritiques();

    boolean existsByMedicamentIdAndTypeAlerteAndStatut(Long medicamentId, TypeAlerte type, StatutAlerte statut);

    @Query("""
        SELECT DISTINCT a
        FROM AlerteStock a
        LEFT JOIN FETCH a.medicament m
        LEFT JOIN FETCH m.categorie
        LEFT JOIN FETCH m.fournisseur
        LEFT JOIN FETCH a.lot
        LEFT JOIN FETCH a.acquittePar
        WHERE a.statut = :statut
        ORDER BY a.dateCreation DESC
    """)
    List<AlerteStock> findByStatutWithRelations(@Param("statut") StatutAlerte statut);

    @Query("""
        SELECT DISTINCT a
        FROM AlerteStock a
        LEFT JOIN FETCH a.medicament m
        LEFT JOIN FETCH m.categorie
        LEFT JOIN FETCH m.fournisseur
        LEFT JOIN FETCH a.lot
        LEFT JOIN FETCH a.acquittePar
        WHERE (:statut IS NULL OR a.statut = :statut)
          AND (:niveau IS NULL OR a.niveau = :niveau)
        ORDER BY a.dateCreation DESC
    """)
    List<AlerteStock> searchWithRelations(
            @Param("statut") StatutAlerte statut,
            @Param("niveau") NiveauAlerte niveau
    );
}