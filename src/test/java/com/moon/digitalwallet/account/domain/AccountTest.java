package com.moon.digitalwallet.account.domain;

import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.user.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {

    @Test
    void deposit_withNonPositiveAmount_throwsBusinessException() {
        // given
        Account account = new Account(new User("tester"));

        // when & then
        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-1.00")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void withdraw_withNonPositiveAmount_throwsBusinessException() {
        // given
        Account account = new Account(new User("tester"));

        // when & then
        assertThatThrownBy(() -> account.withdraw(BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("-1.00")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void withdraw_withInsufficientBalance_throwsBusinessException() {
        // given
        Account account = new Account(new User("tester"));
        account.deposit(new BigDecimal("100.00"));

        // when & then
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("150.00")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void deposit_withPositiveAmount_increasesBalance() {
        // given
        Account account = new Account(new User("tester"));

        // when
        account.deposit(new BigDecimal("100.00"));

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void withdraw_withSufficientBalance_decreasesBalance() {
        // given
        Account account = new Account(new User("tester"));
        account.deposit(new BigDecimal("100.00"));

        // when
        account.withdraw(new BigDecimal("50.00"));

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void withdraw_withExactBalance_setsBalanceToZero() {
        // given
        Account account = new Account(new User("tester"));
        account.deposit(new BigDecimal("100.00"));

        // when
        account.withdraw(new BigDecimal("100.00"));

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");

    }
}
