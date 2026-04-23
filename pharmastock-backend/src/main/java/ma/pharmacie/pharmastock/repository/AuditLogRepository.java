package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findAllWithUtilisateur(Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.utilisateur.id = :utilisateurId
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findByUtilisateurIdWithUtilisateur(Long utilisateurId, Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.timestamp >= :from
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findByTimestampGreaterThanEqualWithUtilisateur(LocalDateTime from, Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.timestamp <= :to
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findByTimestampLessThanEqualWithUtilisateur(LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.timestamp BETWEEN :from AND :to
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findByTimestampBetweenWithUtilisateur(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.utilisateur.id = :utilisateurId
          AND a.timestamp >= :from
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findByUtilisateurIdAndTimestampGreaterThanEqualWithUtilisateur(
            Long utilisateurId,
            LocalDateTime from,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.utilisateur.id = :utilisateurId
          AND a.timestamp <= :to
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findByUtilisateurIdAndTimestampLessThanEqualWithUtilisateur(
            Long utilisateurId,
            LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"utilisateur"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE a.utilisateur.id = :utilisateurId
          AND a.timestamp BETWEEN :from AND :to
        ORDER BY a.timestamp DESC
    """)
    Page<AuditLog> findByUtilisateurIdAndTimestampBetweenWithUtilisateur(
            Long utilisateurId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}