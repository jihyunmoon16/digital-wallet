package com.moon.digitalwallet.account.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.common.error.ApiException;
import com.moon.digitalwallet.common.error.ErrorCode;
import com.moon.digitalwallet.user.domain.User;
import com.moon.digitalwallet.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Long createAccount(String username) {
        User user = userRepository.save(new User(username));
        Account account = new Account(user);
        accountRepository.save(account);

        return account.getId();
    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_NOT_FOUND, "account not found"));

        try {
            account.withdraw(amount);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE, ex.getMessage());
        }
    }

}
