package edu.cidemags.integrity_core.auth.infrastructure.security;

import edu.cidemags.integrity_core.auth.domain.port.RegistrationCodeRepository;
import edu.cidemags.integrity_core.auth.domain.service.RegistrationCodeValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RegistrationCodeValidator registrationCodeValidator(
            RegistrationCodeRepository registrationCodeRepository,
            @Value("${security.registration-codes.administrativo:}") String adminCode,
            @Value("${security.registration-codes.profesor:}") String professorCode) {
        return new DbRegistrationCodeValidator(registrationCodeRepository, adminCode, professorCode);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
