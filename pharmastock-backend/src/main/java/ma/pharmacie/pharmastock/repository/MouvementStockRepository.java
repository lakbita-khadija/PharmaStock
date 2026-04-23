package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.MouvementStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    @Query("SELECT m FROM MouvementStock m LEFT JOIN FETCH m.lot LEFT JOIN FETCH m.utilisateur WHERE m.medicament.id = :medId ORDER BY m.dateOperation DESC")
    List<MouvementStock> findByMedicamentId(@Param("medId") Long medicamentId, Pageable pageable);

    @Query("SELECT m FROM MouvementStock m WHERE m.dateOperation >= :from AND m.dateOperation <= :to ORDER BY m.dateOperation DESC")
    Page<MouvementStock> findByPeriode(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
