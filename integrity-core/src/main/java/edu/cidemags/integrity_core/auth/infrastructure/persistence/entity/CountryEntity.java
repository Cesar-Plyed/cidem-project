package edu.cidemags.integrity_core.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "countries")
public class CountryEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "iso_code", nullable = false, unique = true, length = 3)
    private String isoCode;

    protected CountryEntity() {
    }

    public CountryEntity(UUID id, String name, String isoCode) {
        this.id = id;
        this.name = name;
        this.isoCode = isoCode;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }
}
