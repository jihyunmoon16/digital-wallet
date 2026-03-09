package com.moon.digitalwallet.transfer.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.domain.Transfer;
import com.moon.digitalwallet.transfer.repository.TransferRepository;
import com.moon.digitalwallet.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TransferServiceUnitTest {
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private TransferRepository transferRepository;
	@InjectMocks
	private TransferService transferService;

	@Test
	void transfer_withInsufficientBalance_returnsInsufficientBalanceErrorCode() {
		// given
		Long fromId = 1L;
		Long toId = 2L;

		User userFrom = new User("userFrom");
		User userTo = new User("userTo");

		Account from = new Account(userFrom);
		Account to = new Account(userTo);

		when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
		when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

		// when & then
		assertThatThrownBy(() -> transferService.transfer(fromId, toId, new BigDecimal("100.00")))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex ->
				assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)
			);

		assertThat(from.getBalance()).isEqualByComparingTo("0.00");
		assertThat(to.getBalance()).isEqualByComparingTo("0.00");

		verify(transferRepository, never()).save(any());
	}

	@Test
	void transfer_withSufficientBalance_movesMoneyAndCreatesHistory() {
		// given
		Long fromId = 1L;
		Long toId = 2L;

		User userFrom = new User("userFrom");
		User userTo = new User("userTo");

		Account from = new Account(userFrom);
		Account to = new Account(userTo);

		from.deposit(new BigDecimal("10000.00"));

		when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
		when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

		// when & then
		assertThatCode(() -> transferService.transfer(fromId, toId, new BigDecimal("3000.00")))
			.doesNotThrowAnyException();

		assertThat(from.getBalance()).isEqualByComparingTo("7000.00");
		assertThat(to.getBalance()).isEqualByComparingTo("3000.00");

		verify(transferRepository, times(1)).save(any(Transfer.class));
	}

	/**
	 * 송금 대상 계좌 없으면 예외
	 */
	@Test
	void transfer_withNonExistingAccountTo_returnsAccountNotFoundErrorCode() {
		// given
		Long fromId = 1L;
		Long toId = 2L;

		User userFrom = new User("userFrom");

		Account from = new Account(userFrom);

		from.deposit(new BigDecimal("10000.00"));

		when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
		when(accountRepository.findById(toId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> transferService.transfer(fromId, toId, new BigDecimal("3000.00")))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex ->
				assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND)
			);

		verify(transferRepository, never()).save(any());
	}

}
