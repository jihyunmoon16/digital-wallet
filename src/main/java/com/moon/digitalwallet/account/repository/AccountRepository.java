package com.moon.digitalwallet.account.repository;

import com.moon.digitalwallet.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
