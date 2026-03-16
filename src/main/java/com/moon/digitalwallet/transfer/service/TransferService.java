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

    private final TransferTransactionService transferTransactionService;

    public Long transfer(Long accountFromId, Long accountToId, BigDecimal amount) {
        for(int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return transferTransactionService.transferinternal(accountFromId, accountToId, amount);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRIES) {
                    throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
                }
            }
        }
        throw new IllegalStateException("retry loop exited unexpectedly");
    }
}
