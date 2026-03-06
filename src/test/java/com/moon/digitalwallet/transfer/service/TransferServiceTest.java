package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.account.service.AccountService;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
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
public class TransferServiceTest {
    @Autowired
    private TransferService transferService;
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
}
