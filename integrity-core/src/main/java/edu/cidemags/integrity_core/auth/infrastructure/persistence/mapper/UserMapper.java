package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.Phone;
import edu.cidemags.integrity_core.auth.domain.model.User;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.PhoneEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    private final CountryMapper countryMapper;
    private final PhoneMapper phoneMapper;

    public UserMapper(CountryMapper countryMapper, PhoneMapper phoneMapper) {
        this.countryMapper = countryMapper;
        this.phoneMapper = phoneMapper;
    }

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        List<Phone> phones = entity.getPhones().stream()
                .map(phoneMapper::toDomain)
                .collect(Collectors.toList());

        return new User(
                entity.getId(),
                entity.getFirstNames(),
                entity.getLastNames(),
                entity.getEmail(),
                entity.getPasswordHash(),
                countryMapper.toDomain(entity.getCountry()),
                phones,
                entity.getRole()
        );
    }

    public UserEntity toEntity(User domain, CountryEntity countryEntity) {
        if (domain == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity(
                domain.getId(),
                countryEntity,
                domain.getFirstNames(),
                domain.getLastNames(),
                domain.getEmail(),
                domain.getPasswordHash(),
                domain.getRole()
        );

        List<PhoneEntity> phoneEntities = domain.getPhones().stream()
                .map(phone -> phoneMapper.toEntity(phone, userEntity))
                .collect(Collectors.toList());
        userEntity.setPhones(phoneEntities);

        return userEntity;
    }
}