package com.moon.digitalwallet.account.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.ApiException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void createAccount_withValidUsername_createsAccountWithZeroBalance() {
        // given
        String username = "testname";

        // when
        Long accountId = accountService.createAccount(username);

        // then
        assertThat(accountId).isNotNull();

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void withdraw_withInsufficientBalance_returnsInsufficientBalanceErrorCode() {
        // given
        Long accountId = accountService.createAccount("testname");

        // when & then
        assertThatThrownBy(() -> accountService.withdraw(accountId, new BigDecimal("1.00")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    void withdraw_withNonPositiveAmount_returnsInvalidRequestErrorCode() {
        // given
        Long accountId = accountService.createAccount("testname");

        // when & then
        assertThatThrownBy(() -> accountService.withdraw(accountId, BigDecimal.ZERO))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void withdraw_withMissingAccount_returnsAccountNotFoundErrorCode() {
        // given
        Long missingAccountId = 999999L;

        // when & then
        assertThatThrownBy(() -> accountService.withdraw(missingAccountId, new BigDecimal("1.00")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
