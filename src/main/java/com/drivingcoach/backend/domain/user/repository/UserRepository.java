package com.drivingcoach.backend.domain.user.repository;

import com.drivingcoach.backend.domain.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티 JPA 레포지토리
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    Optional<User> findByIdAndActiveTrue(Long id);
}
