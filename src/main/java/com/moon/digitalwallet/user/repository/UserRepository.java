package com.moon.digitalwallet.user.repository;

import com.moon.digitalwallet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
