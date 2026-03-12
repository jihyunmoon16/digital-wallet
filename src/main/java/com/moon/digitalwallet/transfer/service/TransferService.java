package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.domain.Transfer;
import com.moon.digitalwallet.transfer.repository.TransferRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final int MAX_RETRIES = 3;

    private final TransferTransactionService transferTransactionService;

    public void transfer(Long accountFromId, Long accountToId, BigDecimal amount) {
        for(int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                transferTransactionService.transferinternal(accountFromId, accountToId, amount);
                return; // 성공하면 종료
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRIES) {
                    throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
                }
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
