package com.moon.digitalwallet.transfer.service;

import static org.assertj.core.api.Assertions.*;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.account.service.AccountService;
import com.moon.digitalwallet.common.error.BusinessException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.transfer.repository.TransferRepository;

import jakarta.transaction.Transactional;
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

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TransferServiceIntegrationTest {
    @Autowired
    private TransferService transferService;
    @Autowired
    private TransferRepository transferRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void transfer_withInsufficientBalance_returnsInsufficientBalanceErrorCode() {
        // given
        Long accountToId = accountService.createAccount("accountTo");
        Long accountFromId = accountService.createAccount("accountFrom");

        Account accountTo = accountRepository.findById(accountToId).orElseThrow();
        Account accountFrom = accountRepository.findById(accountFromId).orElseThrow();

        // when & then
        assertThatThrownBy(() -> transferService.transfer(accountFromId, accountToId, new BigDecimal("100.00")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));

        assertThat(accountFrom.getBalance()).isEqualByComparingTo("0.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_withSuffientBalance_movesMoneyAndCreatesHistory() {
        // given
        Long accountFromId = accountService.createAccount("accountFrom");
        Long accountToId = accountService.createAccount("accountTo");

        Account accountFrom = accountRepository.findById(accountFromId).orElseThrow();
        Account accountTo = accountRepository.findById(accountToId).orElseThrow();

        accountFrom.deposit(new BigDecimal("10000.00"));

        // when
        assertThatCode(() -> transferService.transfer(accountFromId, accountToId, new BigDecimal("3000.00")))
                .doesNotThrowAnyException();

        // then
        assertThat(accountFrom.getBalance()).isEqualByComparingTo("7000.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("3000.00");

        assertThat(transferRepository.count()).isEqualTo(1);

    }
    /*
     * concurrency test
     */
    @Test
    void transfer_withConcurrentRequests_maintainsbalanceIntegrity() throws Exception {
        // given
        Long accountFromId = accountService.createAccount("accountFrom");
        Long accountToId1 = accountService.createAccount("accountTo1");
        Long accountToId2 = accountService.createAccount("accountTo2");

        Account accountFrom = accountRepository.findById(accountFromId).orElseThrow();

        accountFrom.deposit(new BigDecimal("10000.00"));


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
