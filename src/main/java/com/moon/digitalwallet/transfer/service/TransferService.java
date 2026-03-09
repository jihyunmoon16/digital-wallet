package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.domain.Transfer;
import com.moon.digitalwallet.transfer.repository.TransferRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public void transfer(Long accountFromId, Long accountToId, BigDecimal amount) {
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

        transferRepository.save(new Transfer(accountFromId,accountToId, amount));
    }
}
