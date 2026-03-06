package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.ApiException;
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
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND, "account not found"));
        Account accountTo = accountRepository.findById(accountToId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND, "account not found"));

        try {
            accountFrom.withdraw(amount);
            accountTo.deposit(amount);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE, ex.getMessage());
        }
    }
}
