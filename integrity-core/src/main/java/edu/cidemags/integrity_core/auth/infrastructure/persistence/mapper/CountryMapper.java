package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import org.springframework.stereotype.Component;

@Component
public class CountryMapper {

    public Country toDomain(CountryEntity entity){
        if (entity == null){
            return null;
        }

        return new Country(entity.getId(), entity.getName(), entity.getIsoCode());
    }

    public CountryEntity toEntity(Country domain){
        if (domain == null){
            return null;
        }

        return new CountryEntity(domain.id(), domain.name(), domain.isoCode());
    }

}
