package com.moon.digitalwallet.acount.domain;

import com.moon.digitalwallet.user.domain.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user+id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Account() {}

    public Account(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
    }

    @PrePersist
    void prePersist() {
        if(createdAt == null) {createdAt = OffsetDateTime.now();}
        if(balance == null) {balance = BigDecimal.ZERO;}
    }

    public Long getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public User getUser() { return user; }

    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }



}
