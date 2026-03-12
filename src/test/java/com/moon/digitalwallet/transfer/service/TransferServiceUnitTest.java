package com.moon.digitalwallet.transfer.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class TransferServiceUnitTest {
	@Mock
	private TransferTransactionService transferTransactionService;
	@InjectMocks
	private TransferService transferService;

	@Test
	void transfer_withInsufficientBalance_propagatesBusinessError() {
		// given
		Long fromId = 1L;
		Long toId = 2L;
		BigDecimal amount = new BigDecimal("100.00");

		doThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE))
			.when(transferTransactionService).transferinternal(fromId, toId, amount);

		// when & then
		assertThatThrownBy(() -> transferService.transfer(fromId, toId, amount))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex ->
				assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)
			);

		verify(transferTransactionService, times(1)).transferinternal(fromId, toId, amount);
	}

	@Test
	void transfer_withSufficientBalance_delegatesToTransactionService() {
		// given
		Long fromId = 1L;
		Long toId = 2L;
		BigDecimal amount = new BigDecimal("3000.00");

		doNothing().when(transferTransactionService).transferinternal(fromId, toId, amount);

		// when & then
		assertThatCode(() -> transferService.transfer(fromId, toId, amount))
			.doesNotThrowAnyException();

		verify(transferTransactionService, times(1)).transferinternal(fromId, toId, amount);
	}

	@Test
	void transfer_withOptimisticLockFailure_retriesAndThrowsConcurrencyError() {
		// given
		Long fromId = 1L;
		Long toId = 2L;
		BigDecimal amount = new BigDecimal("3000.00");

		doThrow(new OptimisticLockingFailureException("lock failed"))
			.when(transferTransactionService).transferinternal(fromId, toId, amount);

		// when & then
		assertThatThrownBy(() -> transferService.transfer(fromId, toId, amount))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex ->
				assertThat(((BusinessException) ex).getErrorCode())
					.isEqualTo(ErrorCode.CONCURRENT_MODIFICATION)
			);

		verify(transferTransactionService, times(3)).transferinternal(fromId, toId, amount);
	}
}
