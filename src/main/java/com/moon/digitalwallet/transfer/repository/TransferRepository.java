package com.moon.digitalwallet.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moon.digitalwallet.transfer.domain.Transfer;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
