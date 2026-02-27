package com.moon.digitalwallet.acount.repository;

import com.moon.digitalwallet.acount.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
