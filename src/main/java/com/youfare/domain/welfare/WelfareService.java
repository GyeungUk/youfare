package com.youfare.domain.welfare;

import com.youfare.domain.user.User;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelfareService {

    private final WelfareRepository welfareRepository;

    /** 전체 목록 조회 (카테고리/지역/상태 필터 + 페이징) */
    public Page<WelfareResponse> getList(WelfareCategory category, String region,
                                         WelfareStatus status, Pageable pageable) {
        String categoryName = (category != null) ? category.name() : null;
        String statusName = (status != null) ? status.name() : null;
        return welfareRepository.findByFilter(categoryName, region, statusName, LocalDate.now(), pageable)
                .map(WelfareResponse::from);
    }

    /** 개인화 추천 — 유저 프로필 기반 필터링
     *  - 로그인 유저의 birthYear → 나이 계산
     *  - region, applyEndDate >= 오늘 조건
     *  - 프로필이 다른 두 유저는 다른 결과를 받게 됨
     */
    public Page<WelfareResponse> getRecommended(User user, WelfareCategory category, Pageable pageable) {
        Integer age = null;
        if (user.getBirthYear() != null) {
            age = LocalDate.now().getYear() - user.getBirthYear();
        }
        String categoryName = (category != null) ? category.name() : null;
        return welfareRepository.findPersonalized(age, user.getRegion(), categoryName, LocalDate.now(), pageable)
                .map(WelfareResponse::from);
    }

    /** 상세 조회 */
    public WelfareResponse getDetail(Long welfareId) {
        return welfareRepository.findById(welfareId)
                .map(WelfareResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.WELFARE_NOT_FOUND));
    }
}
