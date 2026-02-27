package com.moon.digitalwallet.user.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, length = 100, updatable = false)
    private OffsetDateTime createdAt;

    protected User() {}

    public User(String name) {
        this.name = name;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() {return id;}
    public String getName() {return name;}
    public OffsetDateTime getCreatedAt() {return createdAt;}
}
