package edu.cidemags.integrity_core.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "phone_numbers")
public class PhoneEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "is_primary")
    private boolean isPrimary;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PhoneEntity() {
    }

    public PhoneEntity(UUID id, UserEntity user, String phoneNumber, boolean isPrimary) {
        this.id = id;
        this.user = user;
        this.phoneNumber = phoneNumber;
        this.isPrimary = isPrimary;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
