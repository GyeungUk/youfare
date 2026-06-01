package com.youfare.domain.user;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** 내 프로필 조회 */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        User user = findById(userId);
        return UserResponse.from(user);
    }

    /** 온보딩 정보 저장/수정 */
    @Transactional
    public UserResponse updateOnboarding(Long userId, OnboardingRequest request) {
        // birthYear 동적 상한 검증 (@Max 하드코딩 대신 현재 연도 기준)
        int currentYear = LocalDate.now().getYear();
        if (request.getBirthYear() > currentYear) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "출생연도는 " + currentYear + "년 이하여야 합니다.");
        }

        User user = findById(userId);
        user.updateOnboarding(
                request.getBirthYear(),
                request.getRegion(),
                request.getIncomeBracket(),
                request.getEmploymentStatus()
        );
        return UserResponse.from(user);
    }

    private User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
