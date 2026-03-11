package com.moon.digitalwallet.transfer.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.account.service.AccountService;
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

    @Test
    void transfer_withConcurrentRequests_maintainsBalanceIntegrity() throws Exception {
        // given
        Long accountFromId = accountService.createAccount("accountFrom");
        Long accountToId1 = accountService.createAccount("accountTo1");
        Long accountToId2 = accountService.createAccount("accountTo2");

        Account accountFrom = accountRepository.findById(accountFromId).orElseThrow();

        accountFrom.deposit(new BigDecimal("10000.00"));
        accountRepository.saveAndFlush(accountFrom);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();

        // when
        results.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            try {
                transferService.transfer(accountFromId, accountToId1, new BigDecimal("7000.00"));
                return true; // 성공
            } catch (Exception e) {
                return false;
            }
        }));


        results.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            try {
                transferService.transfer(accountFromId, accountToId2, new BigDecimal("5000.00"));
                return true; // 성공
            } catch (Exception e) {
                return false;
            }
        }));


        readyLatch.await(); // 두 스레드가 준비될 때까지 대기
        startLatch.countDown(); // 동시에 시작

        int successCount = 0;

        for(Future<Boolean> result : results) {
            if (result.get()) {
                successCount++;
            }
        }

        // then
        Account from = accountRepository.findById(accountFromId).orElseThrow();

        assertThat(successCount).isEqualTo(1);

        assertThat(from.getBalance()).isIn(
                new BigDecimal("3000.00"), // 7000 송금 성공
                new BigDecimal("5000.00")  // 5000 송금 성공
        );

        executorService.shutdown();
    }
}
