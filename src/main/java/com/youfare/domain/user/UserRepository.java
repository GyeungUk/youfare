package com.youfare.domain.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySocialIdAndProvider(String socialId, Provider provider);

    Optional<User> findByEmailAndProvider(String email, Provider provider);

    Optional<User> findByPhoneNumberAndProvider(String phoneNumber, Provider provider);

    boolean existsByEmailAndProvider(String email, Provider provider);

    boolean existsByPhoneNumberAndProvider(String phoneNumber, Provider provider);

    /**
     * 포인트 적립 시 race condition 방지용 비관적 락 조회
     * SELECT ... FOR UPDATE → 동시 요청을 순차 처리
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);
}
