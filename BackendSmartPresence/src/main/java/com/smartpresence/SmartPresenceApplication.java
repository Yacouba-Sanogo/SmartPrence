package com.smartpresence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée principal de l'application backend SmartPresence.
 *
 * <p>Système biométrique embarqué de gestion automatisée de présence universitaire.
 * Cette classe amorce le contexte Spring Boot et l'autoconfiguration de l'application.</p>
 *
 * <p><b>Stack :</b> Java 25 · Spring Boot 3.5.x · Spring Security · JWT ·
 * Spring Data JPA · Hibernate · MySQL · Bean Validation · Lombok · MapStruct · Swagger.</p>
 *
 * <p><b>Référence :</b> {@code SmartPresence_CONTEXT.md} — document de contexte technique officiel.</p>
 *
 * @see <a href="https://spring.io/projects/spring-boot">Spring Boot</a>
 */
@SpringBootApplication
public class SmartPresenceApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * @param args arguments de ligne de commande transmis au contexte Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartPresenceApplication.class, args);
    }

}
