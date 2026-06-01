package com.youfare.domain.scrap;

import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.domain.welfare.Welfare;
import com.youfare.domain.welfare.WelfareRepository;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final WelfareRepository welfareRepository;

    /**
     * 스크랩 추가
     *
     * ★ 동시성 처리 전략: 비관적 락(PESSIMISTIC_WRITE) 선택 이유
     * - 포인트 누적은 "읽기 → 계산 → 쓰기" 세 단계가 하나의 트랜잭션 안에서 일어남.
     * - 낙관적 락(@Version)은 충돌 시 예외를 던져 재시도 로직이 필요해 클라이언트 부담 증가.
     * - 비관적 락은 DB 레벨에서 SELECT FOR UPDATE로 행을 잠가,
     *   동시 요청이 순차적으로 처리되어 포인트가 정확히 누적됨.
     * - 스크랩/포인트는 쓰기 빈도가 높지 않아 비관적 락의 성능 부담이 적다고 판단.
     */
    @Transactional
    public ScrapResponse addScrap(Long userId, Long welfareId) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Welfare welfare = welfareRepository.findById(welfareId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WELFARE_NOT_FOUND));

        if (scrapRepository.existsByUserAndWelfare(user, welfare)) {
            throw new BusinessException(ErrorCode.ALREADY_SCRAPPED);
        }

        Scrap scrap = scrapRepository.save(Scrap.of(user, welfare));
        user.addPoint(PointPolicy.SCRAP_ADD);   // 스크랩 +2 포인트

        return ScrapResponse.from(scrap);
    }

    /** 스크랩 해제 */
    @Transactional
    public void removeScrap(Long userId, Long welfareId) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Welfare welfare = welfareRepository.findById(welfareId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WELFARE_NOT_FOUND));

        Scrap scrap = scrapRepository.findByUserAndWelfare(user, welfare)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_NOT_FOUND));

        scrapRepository.delete(scrap);

        // 포인트가 0 아래로 내려가지 않게 보호
        int newPoint = Math.max(0, user.getPoint() + PointPolicy.SCRAP_CANCEL);
        user.setPoint(newPoint);
    }

    /** 내 스크랩 목록 조회 */
    @Transactional(readOnly = true)
    public List<ScrapResponse> getMyScrap(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return scrapRepository.findAllByUserWithWelfare(user).stream()
                .map(ScrapResponse::from)
                .collect(Collectors.toList());
    }
}
