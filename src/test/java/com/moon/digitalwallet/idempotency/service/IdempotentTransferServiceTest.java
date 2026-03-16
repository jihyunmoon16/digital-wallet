package com.moon.digitalwallet.idempotency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.account.service.AccountService;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.repository.TransferRepository;

@SpringBootTest
@ActiveProfiles("test")
class IdempotentTransferServiceTest {

	@Autowired
	private IdempotentTransferService idempotentTransferService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private TransferRepository transferRepository;
	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@AfterEach
	void clearIdempotencyKeys() {
		Set<String> keys = stringRedisTemplate.keys("idem:transfer:*");
		if (keys != null && !keys.isEmpty()) {
			stringRedisTemplate.delete(keys);
		}
	}

	@Test
	void transfer_withSameKeyAndSameRequest_processesOnlyOnce() {
		// given
		long transferCountBefore = transferRepository.count();
		Long fromId = accountService.createAccount("from");
		Long toId = accountService.createAccount("to");
		Account from = accountRepository.findById(fromId).orElseThrow();
		from.deposit(new BigDecimal("10000.00"));
		accountRepository.saveAndFlush(from);

		String key = "idem-" + UUID.randomUUID();

		// when
		Long firstTransferId = idempotentTransferService.transfer(key, fromId, toId, new BigDecimal("3000.00"));
		Long secondTransferId = idempotentTransferService.transfer(key, fromId, toId, new BigDecimal("3000.00"));

		// then
		Account reloadedFrom = accountRepository.findById(fromId).orElseThrow();
		Account reloadedTo = accountRepository.findById(toId).orElseThrow();

		assertThat(firstTransferId).isEqualTo(secondTransferId);
		assertThat(reloadedFrom.getBalance()).isEqualByComparingTo("7000.00");
		assertThat(reloadedTo.getBalance()).isEqualByComparingTo("3000.00");
		assertThat(transferRepository.count()).isEqualTo(transferCountBefore + 1);
	}

	@Test
	void transfer_withSameKeyAndDifferentRequest_returnsConflictErrorCode() {
		// given
		long transferCountBefore = transferRepository.count();
		Long fromId = accountService.createAccount("from2");
		Long toId = accountService.createAccount("to2");
		Account from = accountRepository.findById(fromId).orElseThrow();
		from.deposit(new BigDecimal("10000.00"));
		accountRepository.saveAndFlush(from);

		String key = "idem-" + UUID.randomUUID();
		idempotentTransferService.transfer(key, fromId, toId, new BigDecimal("3000.00"));

		// when & then
		assertThatThrownBy(() -> idempotentTransferService.transfer(key, fromId, toId, new BigDecimal("2000.00")))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex ->
				assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT)
			);

		Account reloadedFrom = accountRepository.findById(fromId).orElseThrow();
		Account reloadedTo = accountRepository.findById(toId).orElseThrow();
		assertThat(reloadedFrom.getBalance()).isEqualByComparingTo("7000.00");
		assertThat(reloadedTo.getBalance()).isEqualByComparingTo("3000.00");
		assertThat(transferRepository.count()).isEqualTo(transferCountBefore + 1);
	}
}
