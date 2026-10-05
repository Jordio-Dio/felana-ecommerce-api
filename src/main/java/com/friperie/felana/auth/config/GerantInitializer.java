package com.friperie.felana.auth.config;

import com.friperie.felana.auth.domain.Role;
import com.friperie.felana.auth.domain.User;
import com.friperie.felana.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GerantInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.gerant.name:Gérant Principal}")
    private String defaultName;

    @Value("${app.bootstrap.gerant.password:}")
    private String defaultPassword;

    @Value("${app.bootstrap.gerant.email:gerant@admin.com}")
    private String defaultEmail;

    @Override
    public void run(String... args) {
        // 1. On vérifie si l'email OU le nom existe déjà en BDD
        boolean existsByEmail = userRepository.existsByEmail(defaultEmail);
        boolean existsByName = userRepository.existsByName(defaultName);

        if (existsByEmail || existsByName) {
            log.info("Compte GERANT ('{}' / '{}') déjà présent, aucune action au démarrage.", defaultName, defaultEmail);
            return;
        }

        if (defaultEmail.isBlank() || defaultPassword.isBlank()) {
            log.warn("Aucune propriété/variable d'environnement configurée pour le gérant. / "
                    + "APP_BOOTSTRAP_GERANT_PASSWORD définie : aucun compte GERANT "
                    + "n'a été créé automatiquement. Définissez-les puis redémarrez "
                    + "l'application pour créer le premier compte administrateur.");
            return;
        }

        User gerant = User.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(passwordEncoder.encode(defaultPassword))
                .role(Role.GERANT)
                .enabled(true)
                .build();

        userRepository.save(gerant);
        log.info("Premier compte GERANT '{}' créé avec succès.", defaultEmail);
    }
}