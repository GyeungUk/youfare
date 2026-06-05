package com.youfare.external.openai;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OpenAiClient {

    // 한국어 답변에 섞여 나오는 한자(漢字)를 잡는다. \p{IsHan} = CJK 한자(Han) 스크립트 전체.
    private static final Pattern HANJA = Pattern.compile("\\p{IsHan}");

    // 한자가 가장 적은 답변을 고르기 위한 카운트용(개수까지 센다).
    private static final Pattern HANJA_COUNT = Pattern.compile("\\p{IsHan}");

    // '신청(申請)', '주거 （住居）'처럼 괄호 안이 통째로 한자/공백인 부연 표기를 통째로 제거한다.
    // (한글 단어 뒤에 한자를 괄호로 덧붙이는 Llama의 가장 흔한 패턴)
    private static final Pattern HANJA_PAREN =
            Pattern.compile("\\s*[(\\uFF08][\\p{IsHan}\\s]+[)\\uFF09]");

    // 한자 전용 재요청 최대 횟수. 1회로는 부족한 경우가 잦아 2회까지 시도한다.
    private static final int MAX_HANGUL_RETRY = 2;

    // 한자가 감지됐을 때 시스템 프롬프트에 덧붙여 "한글로만 다시 써라"고 강하게 지시하는 문구.
    private static final String HANGUL_ONLY_REINFORCE =
            "\n\n[재작성 지시] 직전 답변에 한자(漢字)가 섞여 있었습니다. " +
            "이번에는 한자를 단 한 글자도 쓰지 말고, 오직 한글·숫자·기본 문장부호만 사용해 같은 내용을 다시 작성하세요. " +
            "한글 단어 뒤에 한자를 괄호로 덧붙이는 표기(예: '신청(申請)')도 절대 하지 마세요.";

    private final WebClient openAiWebClient;
    private final String model;
    private final String apiKey;
    private final double temperature;
    private final double topP;

    // baseUrl·인증 헤더가 설정된 싱글톤 WebClient 빈을 주입받아 재사용 (매 요청 재생성 방지)
    public OpenAiClient(WebClient openAiWebClient,
                        @Value("${openai.model}") String model,
                        @Value("${openai.api-key}") String apiKey,
                        @Value("${openai.temperature:0.3}") double temperature,
                        @Value("${openai.top-p:0.9}") double topP) {
        this.openAiWebClient = openAiWebClient;
        this.model = model;
        this.apiKey = apiKey;
        this.temperature = temperature;
        this.topP = topP;
    }

    // 앱 기동 시점에 API 키 미설정을 한 번 경고해 "무슨 말을 해도 오류" 상황의 원인을 명확히 한다.
    @PostConstruct
    void warnIfKeyMissing() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("dummy")) {
            log.warn("[OpenAI/Groq] API 키가 설정되지 않았습니다(현재 값: '{}'). " +
                    "환경변수 GROQ_API_KEY에 실제 키(gsk_...)를 넣지 않으면 AI 상담이 항상 실패합니다.", apiKey);
        }
    }

    public String chat(String systemPrompt, String userMessage) {
        String best = callOnce(systemPrompt, userMessage);
        int bestCount = countHanja(best);

        // Llama 계열은 한국어 답변에 한자를 섞는 일이 잦다. 감지되면 강화 지시로 재요청하되,
        // 한 번으로는 부족한 경우가 많아 최대 MAX_HANGUL_RETRY회까지 시도한다.
        // 매번 '한자가 가장 적은' 답변을 best로 유지하고, 한자가 0이 되면 즉시 중단한다.
        for (int attempt = 1; attempt <= MAX_HANGUL_RETRY && bestCount > 0; attempt++) {
            log.warn("[OpenAI/Groq] 응답에 한자가 감지되어 한글 전용으로 재요청합니다. (시도 {}/{})",
                    attempt, MAX_HANGUL_RETRY);
            String retried = callOnce(systemPrompt + HANGUL_ONLY_REINFORCE, userMessage);
            int retriedCount = countHanja(retried);
            if (retriedCount < bestCount) {
                best = retried;
                bestCount = retriedCount;
            }
        }

        // 재요청을 다 거쳐도 한자가 남으면, 최후의 보루로 코드에서 직접 제거한다.
        // → 모델이 끝내 협조하지 않아도 사용자에게는 한자가 절대 노출되지 않는다.
        if (bestCount > 0) {
            log.warn("[OpenAI/Groq] 재요청 후에도 한자가 {}자 남아 코드에서 직접 제거합니다.", bestCount);
            best = stripHanja(best);
        }
        return best;
    }

    private int countHanja(String text) {
        if (text == null) return 0;
        int count = 0;
        var m = HANJA_COUNT.matcher(text);
        while (m.find()) count++;
        return count;
    }

    /**
     * 답변에 남은 한자를 제거하는 최후의 보루.
     * 1) '신청(申請)'처럼 괄호 안이 통째로 한자인 부연 표기는 괄호째 삭제 → '신청'
     * 2) 그 밖에 남은 낱개 한자는 한 글자씩 삭제
     * 3) 한자 제거로 생긴 군더더기(빈 괄호, 연속 공백)를 정리
     */
    private String stripHanja(String text) {
        if (text == null) return null;
        String cleaned = HANJA_PAREN.matcher(text).replaceAll("");   // 괄호 안 한자 부연 제거
        cleaned = HANJA.matcher(cleaned).replaceAll("");             // 남은 낱개 한자 제거
        cleaned = cleaned.replaceAll("[(\\uFF08]\\s*[)\\uFF09]", "") // 비워진 괄호 제거
                         .replaceAll("[ \\t]{2,}", " ")              // 연속 공백 축소
                         .replaceAll(" +([,.!?])", "$1");            // 부호 앞 공백 정리
        return cleaned;
    }

    private String callOnce(String systemPrompt, String userMessage) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .maxTokens(1000)
                .temperature(temperature)
                .topP(topP)
                .messages(List.of(
                        ChatCompletionRequest.Message.builder().role("system").content(systemPrompt).build(),
                        ChatCompletionRequest.Message.builder().role("user").content(userMessage).build()
                ))
                .build();

        try {
            ChatCompletionResponse response = openAiWebClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            String content = response != null ? response.getContent() : "";
            return (content != null && !content.isBlank()) ? content : "응답을 받지 못했습니다.";

        } catch (WebClientResponseException e) {
            // 실제 HTTP 상태코드와 응답 본문을 남겨야 401(키 오류)·400(모델명 오류) 등을 구분할 수 있다.
            log.error("OpenAI API 호출 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
