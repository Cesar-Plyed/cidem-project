package edu.cidemags.integrity_core.auth.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class User {
    private UUID id;
    private String firstNames;
    private String lastNames;
    private String email;
    private String passwordHash;
    private Country country;
    private List<Phone> phones;
    private Role role;

    public User(UUID id, String firstNames, String lastNames, String email, String passwordHash,
                Country country, List<Phone> phone, Role role) {
        validateRole(role);
        this.id = id;
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.email = email;
        this.passwordHash = passwordHash;
        this.country = country;
        this.phones = phone;
        this.role = role;
    }

    public User(String firstNames, String lastNames, String email, String passwordHash,
                Country country, List<Phone> phone, Role role) {
        validateRole(role);
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.email = email;
        this.passwordHash = passwordHash;
        this.country = country;
        this.phones = phone;
        this.role = role;
    }

    private void validateRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("El usuario debe tener un rol asignado");
        }
    }

    public void addPhone(String phoneNumber, boolean isPrimary) {
        if (isPrimary) {
            this.phones.forEach(Phone::unmarkPrimary);
        }
        this.phones.add(new Phone(phoneNumber, isPrimary));
    }

    public boolean hasRole(Role candidate) {
        return this.role == candidate;
    }

    public void changeRole(Role newRole) {
        validateRole(newRole);
        this.role = newRole;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    public String getLastNames() {
        return lastNames;
    }

    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public List<Phone> getPhones() {
        return Collections.unmodifiableList(phones);
    }

    public void setPhones(List<Phone> phones) {
        this.phones = phones;
    }

    public Role getRole() {
        return role;
    }
}