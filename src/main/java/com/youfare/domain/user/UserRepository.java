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

    boolean existsByEmailAndProvider(String email, Provider provider);

    /**
     * 같은 이메일로 가입된 소셜(비-LOCAL) 계정을 찾는다.
     * 폼 가입·아이디찾기·비번재설정에서 "이 이메일은 네이버/카카오 계정이에요"라고 안내하는 데 쓴다.
     * (email,provider) 유니크 제약상 provider별로 최대 1건이므로 First로 한 건만 가져온다.
     * IgnoreCase: 소셜 계정 email은 OAuth 응답 원본 그대로 저장돼(정규화 안 됨) 대소문자가 섞일 수 있으므로
     *             정규화된(소문자) 입력과 대소문자 무시로 매칭해야 누락 없이 잡힌다.
     */
    Optional<User> findFirstByEmailIgnoreCaseAndProviderNot(String email, Provider provider);

    Optional<User> findByUsernameAndProvider(String username, Provider provider);

    boolean existsByUsernameAndProvider(String username, Provider provider);

    /**
     * 포인트 적립 시 race condition 방지용 비관적 락 조회
     * SELECT ... FOR UPDATE → 동시 요청을 순차 처리
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);
}
