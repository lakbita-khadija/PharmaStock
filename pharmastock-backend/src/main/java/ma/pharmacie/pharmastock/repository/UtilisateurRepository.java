package ma.pharmacie.pharmastock.repository;

import ma.pharmacie.pharmastock.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE Utilisateur u SET u.tentativesEchec = u.tentativesEchec + 1 WHERE u.id = :id")
    void incrementerTentativesEchec(Long id);

    @Modifying
    @Query("UPDATE Utilisateur u SET u.tentativesEchec = 0 WHERE u.id = :id")
    void reinitialiserTentatives(Long id);
}
