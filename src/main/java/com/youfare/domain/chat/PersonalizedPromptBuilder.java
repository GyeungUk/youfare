package com.youfare.domain.chat;

import com.youfare.domain.user.User;
import com.youfare.domain.welfare.Welfare;
import com.youfare.domain.welfare.WelfareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ★ 개인화 컨텍스트 주입 방식
 *
 * 단순히 같은 프롬프트를 쓰면 모든 유저가 동일한 답을 받는다.
 * 이를 해결하기 위해:
 *
 * 1. [유저 프로필 주입]
 *    로그인 유저의 나이, 지역, 소득구간, 취업상태를 시스템 프롬프트에 동적으로 삽입.
 *    → AI가 "이 사람에게 맞는 답"을 생성하도록 컨텍스트를 제한.
 *
 * 2. [추천 혜택 목록 요약 주입]
 *    DB에서 해당 유저 조건에 맞는 현재 신청 가능한 혜택 상위 5건을 뽑아
 *    제목·설명 요약을 시스템 프롬프트에 포함.
 *    → AI가 실제 데이터를 근거로 답변하도록 강제 (Retrieval-Augmented Generation 패턴).
 *
 * 3. [면책 고지 강제]
 *    프롬프트 말미에 "최종 확인은 공식 기관에서" 안내를 항상 포함하도록 지시.
 *
 * 결과: 같은 질문을 해도 유저 A(서울 20대 취준생)와 유저 B(부산 30대 재직자)는
 *       서로 다른 혜택 목록을 컨텍스트로 받으므로 응답 내용이 달라진다.
 */
@Component
@RequiredArgsConstructor
public class PersonalizedPromptBuilder {

    private final WelfareRepository welfareRepository;

    public String build(User user) {
        int age = calculateAge(user);
        String region = user.getRegion() != null ? user.getRegion() : "미설정";
        String income = user.getIncomeBracket() != null ? user.getIncomeBracket().name() : "UNKNOWN";
        String employment = user.getEmploymentStatus() != null ? user.getEmploymentStatus().name() : "미설정";

        // DB에서 이 유저 조건에 맞는 상위 5개 혜택 조회
        String welfareSummary = buildWelfareSummary(user, age);

        return String.format("""
                당신은 청년 복지·금융 상담 도우미 'Youfare'입니다.
                
                [현재 상담 사용자 프로필]
                - 나이: %d세
                - 거주지역: %s
                - 소득구간: %s
                - 취업상태: %s
                
                [현재 신청 가능한 맞춤 혜택 (상위 5건)]
                %s
                
                위 맥락에 근거하여 사용자의 질문에 답변해 주세요.
                - 답변은 친절하고 이해하기 쉽게 작성하세요.
                - 위 혜택 목록을 적극 활용하되, 목록에 없는 내용은 "공식 기관 확인을 권장한다"고 안내하세요.
                - 반드시 답변 마지막에 다음 면책 고지를 포함하세요:
                  "※ 본 답변은 참고용 정보이며, 정확한 내용과 신청 조건은 해당 기관 공식 채널에서 반드시 확인하시기 바랍니다."
                """,
                age, region, income, employment, welfareSummary);
    }

    private String buildWelfareSummary(User user, int age) {
        try {
            List<Welfare> list = welfareRepository
                    .findPersonalized(age, user.getRegion(), LocalDate.now(), PageRequest.of(0, 5))
                    .getContent();

            if (list.isEmpty()) return "- 현재 조건에 맞는 혜택 정보가 없습니다.";

            return list.stream()
                    .map(w -> String.format("- [%s] %s: %s",
                            w.getCategory() != null ? w.getCategory().name() : "ETC",
                            w.getTitle(),
                            w.getDescription() != null
                                    ? w.getDescription().substring(0, Math.min(80, w.getDescription().length())) + "..."
                                    : "상세 정보 없음"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "- 혜택 정보를 불러오지 못했습니다.";
        }
    }

    private int calculateAge(User user) {
        if (user.getBirthYear() == null) return 0;
        return LocalDate.now().getYear() - user.getBirthYear();
    }
}
