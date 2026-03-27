package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MILLIS = 50L;

    private final TransferTransactionService transferTransactionService;

    public Long transfer(Long accountFromId, Long accountToId, BigDecimal amount) {
        for(int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return transferTransactionService.transferinternal(accountFromId, accountToId, amount);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRIES) {
                    throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
                }
                backoff(attempt);
            }
        }
        throw new IllegalStateException("retry loop exited unexpectedly");
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(BASE_BACKOFF_MILLIS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("retry backoff interrupted", e);
        }
    }
}
