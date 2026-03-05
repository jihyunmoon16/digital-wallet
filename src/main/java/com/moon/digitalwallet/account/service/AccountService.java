package com.moon.digitalwallet.account.service;

import com.moon.digitalwallet.account.domain.Account;
import com.moon.digitalwallet.account.repository.AccountRepository;
import com.moon.digitalwallet.user.domain.User;
import com.moon.digitalwallet.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

}
