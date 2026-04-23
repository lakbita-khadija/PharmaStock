package ma.pharmacie.pharmastock.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.pharmacie.pharmastock.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        int count = 0;

        for (var u : utilisateurRepository.findAll()) {

            // Vérifie si le mot de passe est null ou mal formaté
            if (u.getMotDePasse() == null || !u.getMotDePasse().startsWith("$2a$")) {

                String newPassword = passwordEncoder.encode("Admin123!");
                u.setMotDePasse(newPassword);
                utilisateurRepository.save(u);

                count++;
                log.info("🔄 Mot de passe corrigé pour : {}", u.getEmail());
            }
        }

        if (count > 0) {
            log.info("✅ {} mot(s) de passe initialisé(s).", count);
        } else {
            log.info("✔️ Aucun mot de passe à corriger.");
        }
    }
}