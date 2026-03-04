package com.moon.digitalwallet.account.service;

import com.moon.digitalwallet.acount.domain.Account;
import com.moon.digitalwallet.acount.repository.AccountRepository;
import com.moon.digitalwallet.acount.service.AccountService;
import com.moon.digitalwallet.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AccountServiceTest {
    @Autowired
    AccountService accountService;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    void createAccount_createsAccountWithZeroBalance() {
        // when
        Long accountId = accountService.createAccount("testname");

        // then
        assertThat(accountId).isNotNull();

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");

        assertThat(userRepository.count()).isEqualTo(1);
    }


}
