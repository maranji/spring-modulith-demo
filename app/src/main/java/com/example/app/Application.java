package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Applicazione sempre attiva che compone i due moduli/starter:
 * {@code market-data-spring-boot-starter} e {@code analytics-spring-boot-starter}.
 *
 * <p>Questo modulo ("app") si limita a esporre l'API REST (package
 * {@code com.example.app.web}); tutta la logica di dominio vive negli
 * starter, che vengono attivati automaticamente tramite auto-configuration
 * appena presenti sul classpath.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
