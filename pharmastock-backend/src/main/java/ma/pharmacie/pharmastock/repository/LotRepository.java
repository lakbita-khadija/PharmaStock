package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.Lot;
import ma.pharmacie.pharmastock.enums.StatutLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {

    List<Lot> findByMedicamentIdAndStatutOrderByDateExpirationAsc(Long medicamentId, StatutLot statut);

    @Query("""
        SELECT l
        FROM Lot l
        WHERE l.medicament.id = :medId
          AND l.statut = 'ACTIF'
          AND l.quantiteDisponible > 0
        ORDER BY l.dateExpiration ASC
    """)
    List<Lot> findLotsDisponiblesFefo(@Param("medId") Long medicamentId);

    @Query("""
        SELECT l
        FROM Lot l
        WHERE l.dateExpiration <= :date
          AND l.statut = 'ACTIF'
    """)
    List<Lot> findLotsExpirantAvant(@Param("date") LocalDate date);

    @Query("""
        SELECT l
        FROM Lot l
        WHERE l.dateExpiration < CURRENT_DATE
          AND l.statut = 'ACTIF'
    """)
    List<Lot> findLotsExpires();

    @Query("""
        SELECT COALESCE(SUM(l.quantiteDisponible), 0)
        FROM Lot l
        WHERE l.medicament.id = :medId
          AND l.statut = 'ACTIF'
    """)
    Integer sumStockDisponible(@Param("medId") Long medicamentId);

    @Query("""
        SELECT l
        FROM Lot l
        JOIN FETCH l.medicament m
        WHERE l.statut = 'ACTIF'
          AND l.quantiteDisponible > 0
          AND (:q IS NULL OR LOWER(m.nomCommercial) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY l.dateExpiration ASC
    """)
    List<Lot> findAllActifsWithMedicament(@Param("q") String q);

    @Query("""
        SELECT l
        FROM Lot l
        JOIN FETCH l.medicament m
        WHERE l.statut = 'ACTIF'
    """)
    List<Lot> findAllWithMedicament();
}