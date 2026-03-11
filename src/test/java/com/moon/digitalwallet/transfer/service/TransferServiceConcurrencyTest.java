package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.account.service.AccountService;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class TransferServiceConcurrencyTest {
    @Autowired
    private TransferService transferService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransferRepository transferRepository;

    @Test
    void transfer_withConcurrentRequests_returnsOneSuccessAndOneConcurrencyError() throws Exception {
        // given
        Long accountFromId = accountService.createAccount("accountFrom");
        Long accountToId1 = accountService.createAccount("accountTo1");
        Long accountToId2 = accountService.createAccount("accountTo2");
        long transferCountBefore = transferRepository.count();

        Account accountFrom = accountRepository.findById(accountFromId).orElseThrow();

        accountFrom.deposit(new BigDecimal("10000.00"));
        accountRepository.saveAndFlush(accountFrom);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<AttemptResult>> results = new ArrayList<>();

        // when
        results.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            try {
                transferService.transfer(accountFromId, accountToId1, new BigDecimal("7000.00"));
                return new AttemptResult(true, null);
            } catch (BusinessException e) {
                return new AttemptResult(false, e.getErrorCode());
            }
        }));


        results.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            try {
                transferService.transfer(accountFromId, accountToId2, new BigDecimal("5000.00"));
                return new AttemptResult(true, null);
            } catch (BusinessException e) {
                return new AttemptResult(false, e.getErrorCode());
            }
        }));


        readyLatch.await(); // 두 스레드가 준비될 때까지 대기
        startLatch.countDown(); // 동시에 시작

        List<AttemptResult> attemptResults = new ArrayList<>();
        for (Future<AttemptResult> result : results) {
            attemptResults.add(result.get());
        }

        long successCount = attemptResults.stream()
                .filter(AttemptResult::success)
                .count();

        List<AttemptResult> failedResults = attemptResults.stream()
                .filter(result -> !result.success())
                .toList();

        // then
        Account from = accountRepository.findById(accountFromId).orElseThrow();
        ErrorCode expectedConcurrencyError = ErrorCode.CONCURRENT_MODIFICATION;

        assertThat(successCount).isEqualTo(1);
        assertThat(failedResults).hasSize(1);
        assertThat(failedResults.getFirst().errorCode()).isEqualTo(expectedConcurrencyError);

        assertThat(from.getBalance()).isIn(
                new BigDecimal("3000.00"), // 7000 송금 성공
                new BigDecimal("5000.00")  // 5000 송금 성공
        );
        assertThat(transferRepository.count()).isEqualTo(transferCountBefore + 1);

        executorService.shutdown();
    }

    private record AttemptResult(boolean success, ErrorCode errorCode) {
    }
}
