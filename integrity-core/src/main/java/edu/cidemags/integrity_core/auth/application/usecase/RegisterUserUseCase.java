package edu.cidemags.integrity_core.auth.application.usecase;

import edu.cidemags.integrity_core.auth.application.dto.RegisterUserCommand;
import edu.cidemags.integrity_core.auth.domain.exception.CountryNotFoundException;
import edu.cidemags.integrity_core.auth.domain.exception.EmailAlreadyRegisteredException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidRoleAssignmentException;
import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.CountryRepository;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import edu.cidemags.integrity_core.auth.domain.service.PasswordHasher;
import edu.cidemags.integrity_core.auth.domain.service.RegistrationCodeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final PasswordHasher passwordHasher;
    private final RegistrationCodeValidator registrationCodeValidator;

    public RegisterUserUseCase(UserRepository userRepository, CountryRepository countryRepository,
            PasswordHasher passwordHasher, RegistrationCodeValidator registrationCodeValidator) {
        this.userRepository = userRepository;
        this.countryRepository = countryRepository;
        this.passwordHasher = passwordHasher;
        this.registrationCodeValidator = registrationCodeValidator;
    }

    @Transactional
    public User execute(RegisterUserCommand command) {
        if (!registrationCodeValidator.isValid(command.role(), command.registrationCode())) {
            throw new InvalidRoleAssignmentException(
                    "Codigo de registro invalido o faltante para el rol " + command.role());
        }
        User user = doRegister(command);
        registrationCodeValidator.consume(command.registrationCode());
        return user;
    }


    @Transactional
    public User executeAsAdmin(RegisterUserCommand command) {
        return doRegister(command);
    }

    private User doRegister(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException(command.email());
        }

        Country country = countryRepository.findById(command.countryId())
                .orElseThrow(() -> new CountryNotFoundException(command.countryId()));

        String passwordHash = passwordHasher.hash(command.rawPassword());

        List<edu.cidemags.integrity_core.auth.domain.model.Phone> phones = new ArrayList<>();

        User user = new User(
                command.firstNames(),
                command.lastNames(),
                command.email(),
                passwordHash,
                country,
                phones,
                command.role());

        if (command.phoneNumber() != null && !command.phoneNumber().isBlank()) {
            user.addPhone(command.phoneNumber(), true);
        }

        return userRepository.save(user);
    }
}