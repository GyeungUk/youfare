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

        // 프로필 미입력 항목은 프롬프트에서 아예 빼서, AI가 "정보가 없어 답을 못 한다"고
        // 회피하지 않도록 한다. (예전엔 '나이: 0세 / 미설정'이 주입돼 AI가 질문을 거부했음)
        StringBuilder profile = new StringBuilder();
        if (age > 0)                              profile.append("- 나이: ").append(age).append("세\n");
        if (user.getRegion() != null)             profile.append("- 거주지역: ").append(user.getRegion()).append("\n");
        if (user.getIncomeBracket() != null)      profile.append("- 소득구간: ").append(user.getIncomeBracket().name()).append("\n");
        if (user.getEmploymentStatus() != null)   profile.append("- 취업상태: ").append(user.getEmploymentStatus().name()).append("\n");

        boolean hasProfile = profile.length() > 0;
        String profileSection = hasProfile
                ? profile.toString().stripTrailing()
                : "- (아직 입력된 프로필 정보가 없습니다)";

        // DB에서 이 유저 조건에 맞는 상위 5개 혜택 조회
        String welfareSummary = buildWelfareSummary(age, user.getRegion());

        return compose(profileSection, welfareSummary);
    }

    /**
     * 비로그인(게스트) 상담용 프롬프트.
     * 프로필이 없으므로 개인화 대신 '현재 신청 가능한 일반 혜택' 상위 5건을 컨텍스트로 주입한다.
     * (나이·지역 필터 없이 조회 → age/region = null)
     */
    public String buildAnonymous() {
        String profileSection = "- (비로그인 상태입니다. 로그인 후 나이·지역·소득·취업상태를 입력하면 맞춤 추천을 받을 수 있어요)";
        String welfareSummary = buildWelfareSummary(null, null);
        return compose(profileSection, welfareSummary);
    }

    private String compose(String profileSection, String welfareSummary) {
        return String.format("""
                당신은 청년 복지·금융 상담 도우미 'Youfare'입니다.

                [가장 중요한 규칙 — 무조건 지킬 것]
                1. 답변은 오직 한국어(한글)로만 작성합니다. 한자(漢字)나 중국어·일본어·영어 단어를
                   절대 섞지 마세요. (예: '答案'→'답변', '本'→'이/본', '申請'→'신청'처럼 반드시 한글로)
                   사람 이름·기관명 같은 고유명사가 아니면 한글 외 문자는 한 글자도 쓰지 마세요.
                2. 먼저 사용자의 질문 의도를 정확히 파악한 뒤, 그 질문에 직접 답하세요.
                   질문과 동떨어진 일반론만 늘어놓지 말고, 무엇을 묻는지부터 짚고 시작하세요.

                [현재 상담 사용자 프로필]
                %s

                [현재 신청 가능한 맞춤 혜택 (DB 조회 결과)]
                %s

                [답변 지침]
                - 위 맞춤 혜택 목록이 있으면 적극 활용해 구체적으로 안내하세요.
                - 혜택 목록이 비어 있거나 프로필 정보가 부족하더라도, 절대 "정보가 없어 답할 수 없다"고 회피하지 마세요.
                  대신 청년 복지·금융에 대한 일반적인 지식으로 최대한 도움이 되는 답을 제공하세요.
                - 프로필 정보가 비어 있다면, 답변 끝에 "온보딩에서 나이·지역·소득·취업상태를 입력하면 더 정확한 맞춤 추천을 받을 수 있어요"라고 한 줄 안내만 덧붙이세요(질문 거부 금지).
                - 목록에 없는 구체적 신청 조건·금액은 "공식 기관 확인을 권장한다"고 안내하세요.
                - 면책 고지 문구는 시스템이 자동으로 덧붙이니, 답변에 직접 쓰지 마세요.
                """,
                profileSection, welfareSummary);
    }

    // age/region 이 null 이면 해당 필터를 건너뛰고 일반 혜택을 조회한다(게스트 상담용).
    private String buildWelfareSummary(Integer age, String region) {
        try {
            List<Welfare> list = welfareRepository
                    .findPersonalized(age, region, null, LocalDate.now(), PageRequest.of(0, 5))
                    .getContent();

            if (list.isEmpty()) return "- 현재 조건에 맞는 혜택 정보가 없습니다.";

            return list.stream()
                    .map(w -> String.format("- [%s] %s: %s",
                            w.getCategory() != null ? w.getCategory().name() : "ETC",
                            w.getTitle(),
                            w.getDescription() != null
                                    ? w.getDescription().substring(0, Math.min(160, w.getDescription().length())) + "..."
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
