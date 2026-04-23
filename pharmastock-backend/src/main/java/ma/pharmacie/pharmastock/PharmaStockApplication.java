package ma.pharmacie.pharmastock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PharmaStockApplication {
    public static void main(String[] args) {
        SpringApplication.run(PharmaStockApplication.class, args);
    }
}
