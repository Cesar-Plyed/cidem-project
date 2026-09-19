package edu.cidemags.integrity_core.auth.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;

public interface UserRepository {
    List<User> findAll();
    List<User> findAllByRole(Role role);
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}