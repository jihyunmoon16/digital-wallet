package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;


    @Transactional
    public void transfer(Long accountFromId, Long accountToId, BigDecimal amount) {
        Account accountFrom = accountRepository.findById(accountFromId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "account not found"));
        Account accountTo = accountRepository.findById(accountToId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "account not found"));

        accountFrom.withdraw(amount);
        accountTo.deposit(amount);
    }
}
