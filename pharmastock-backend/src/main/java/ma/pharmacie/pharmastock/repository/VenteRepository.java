package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.Vente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    @EntityGraph(attributePaths = {"caissier"})
    @Query("""
        SELECT v
        FROM Vente v
        WHERE (:q IS NULL OR v.numeroVente LIKE CONCAT('%', :q, '%'))
        ORDER BY v.dateVente DESC
    """)
    Page<Vente> searchByQ(String q, Pageable pageable);

    @EntityGraph(attributePaths = {"caissier"})
    @Query("""
        SELECT v
        FROM Vente v
        WHERE (:q IS NULL OR v.numeroVente LIKE CONCAT('%', :q, '%'))
          AND v.dateVente >= :from
        ORDER BY v.dateVente DESC
    """)
    Page<Vente> searchByQAndFrom(String q, LocalDateTime from, Pageable pageable);

    @EntityGraph(attributePaths = {"caissier"})
    @Query("""
        SELECT v
        FROM Vente v
        WHERE (:q IS NULL OR v.numeroVente LIKE CONCAT('%', :q, '%'))
          AND v.dateVente <= :to
        ORDER BY v.dateVente DESC
    """)
    Page<Vente> searchByQAndTo(String q, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = {"caissier"})
    @Query("""
        SELECT v
        FROM Vente v
        WHERE (:q IS NULL OR v.numeroVente LIKE CONCAT('%', :q, '%'))
          AND v.dateVente BETWEEN :from AND :to
        ORDER BY v.dateVente DESC
    """)
    Page<Vente> searchByQAndBetween(String q, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(v.totalTtc), 0)
        FROM Vente v
        WHERE v.statut = 'VALIDEE'
          AND v.dateVente >= :from
          AND v.dateVente <= :to
    """)
    BigDecimal sumCa(LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT COUNT(v)
        FROM Vente v
        WHERE v.statut = 'VALIDEE'
          AND v.dateVente >= :from
          AND v.dateVente <= :to
    """)
    Long countVentes(LocalDateTime from, LocalDateTime to);

    boolean existsByNumeroVente(String numeroVente);
}