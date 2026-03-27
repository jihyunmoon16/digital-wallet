package com.moon.digitalwallet.transfer.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.domain.Transfer;
import com.moon.digitalwallet.transfer.repository.TransferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferTransactionService {

	private final AccountRepository accountRepository;
	private final TransferRepository transferRepository;

	@Transactional(timeout = 3)
	public Long transferinternal(Long accountFromId, Long accountToId, BigDecimal amount) {
		Account accountFrom = accountRepository.findById(accountFromId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

		Account accountTo = accountRepository.findById(accountToId)
			.orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

		if (accountFromId.equals(accountToId)) {
			throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED);
		}

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(ErrorCode.INVALID_TRANSFER_AMOUNT);
		}

		accountFrom.withdraw(amount);
		accountTo.deposit(amount);

		// 여기서는 optimistic lock 예외를 잡지 말고 그대로 둔다
		Transfer transfer = transferRepository.saveAndFlush(new Transfer(accountFromId, accountToId, amount));
		return transfer.getId();
	}
}
