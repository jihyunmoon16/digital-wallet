package com.moon.digitalwallet.transfer.service;

import static org.assertj.core.api.Assertions.*;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.account.service.AccountService;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.repository.TransferRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TransferServiceIntegrationTest {
    @Autowired
    private TransferService transferService;
    @Autowired
    private TransferRepository transferRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void transfer_withInsufficientBalance_returnsInsufficientBalanceErrorCode() {
        // given
        Long accountToId = accountService.createAccount("accountTo");
        Long accountFromId = accountService.createAccount("accountFrom");

        Account accountTo = accountRepository.findById(accountToId).orElseThrow();
        Account accountFrom = accountRepository.findById(accountFromId).orElseThrow();

        // when & then
        assertThatThrownBy(() -> transferService.transfer(accountFromId, accountToId, new BigDecimal("100.00")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));

        assertThat(accountFrom.getBalance()).isEqualByComparingTo("0.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_withSufficientBalance_movesMoneyAndCreatesHistory() {
        // given
        Long accountFromId = accountService.createAccount("accountFrom");
        Long accountToId = accountService.createAccount("accountTo");

        Account accountFrom = accountRepository.findById(accountFromId).orElseThrow();
        Account accountTo = accountRepository.findById(accountToId).orElseThrow();

        accountFrom.deposit(new BigDecimal("10000.00"));

        // when
        assertThatCode(() -> transferService.transfer(accountFromId, accountToId, new BigDecimal("3000.00")))
                .doesNotThrowAnyException();

        // then
        assertThat(accountFrom.getBalance()).isEqualByComparingTo("7000.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("3000.00");

        assertThat(transferRepository.count()).isEqualTo(1);

    }
}
