package com.moon.digitalwallet.account.domain;

import com.moon.digitalwallet.user.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {

    @Test
    void withdraw_rejectsNonPositiveAmount() {

        Account account = new Account(new User("tester"));

        assertThatThrownBy(() -> account.withdraw(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("-1.00"))).isInstanceOf(IllegalArgumentException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void withdraw_rejectsInsufficientBalance() {
        Account account = new Account(new User("tester"));
        account.deposit(new BigDecimal("100.00"));
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("150.00"))).isInstanceOf(IllegalStateException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }
}
