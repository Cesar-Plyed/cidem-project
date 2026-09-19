package edu.cidemags.integrity_core.auth.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.domain.port.UserRepository;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.UserEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.UserMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.CountryJpaRepository;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.UserJpaRepository;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final CountryJpaRepository countryJpaRepository;
    private final UserMapper userMapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, CountryJpaRepository countryJpaRepository,
            UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.countryJpaRepository = countryJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream().map(userMapper::toDomain).toList();
    }

    @Override
    public List<User> findAllByRole(edu.cidemags.integrity_core.auth.domain.model.Role role) {
        return userJpaRepository.findAllByRole(role).stream().map(userMapper::toDomain).toList();
    }

    @Override
    public User save(User user) {
        CountryEntity countryEntity = countryJpaRepository.getReferenceById(user.getCountry().id());
        UserEntity entity = userMapper.toEntity(user, countryEntity);
        UserEntity saved = userJpaRepository.save(entity);

        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }
}
