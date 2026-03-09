package com.moon.digitalwallet.transfer.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "transfers")
public class Transfer {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long fromAccountId;

	private Long toAccountId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	private OffsetDateTime createdAt;

	protected Transfer() {}

	public Transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
	}

	@PrePersist
	void prePersist() {
		if(createdAt == null) {createdAt = OffsetDateTime.now();}
	}

	public Long getId() { return id; }
}
