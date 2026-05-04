package com.tenslots.adapter.out.persistence;

import com.tenslots.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {
}
