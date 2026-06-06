package com.youfare.domain.chat;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비로그인(게스트) AI 상담 횟수 제한기.
 *
 * - 게스트는 식별할 계정이 없으므로 클라이언트 IP 기준으로 하루 호출 횟수를 센다.
 * - 외부 저장소 없이 인메모리(ConcurrentHashMap)로 관리한다. 서버 재시작 시 초기화되며,
 *   날짜가 바뀌면 카운트가 자동으로 리셋된다(엔트리의 date가 오늘과 다르면 1부터 다시 시작).
 * - 로그인 유저에게는 적용하지 않는다(컨트롤러에서 게스트일 때만 호출).
 *
 * 한도는 chat.guest-daily-limit 설정값(기본 5회)을 따른다.
 */
@Component
public class GuestChatRateLimiter {

    private record Window(LocalDate date, int count) {}

    private final int dailyLimit;
    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    public GuestChatRateLimiter(@Value("${chat.guest-daily-limit:5}") int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    /**
     * 게스트 1회 사용을 기록한다. 오늘 한도를 초과하면 BusinessException(429)을 던진다.
     */
    public void checkAndConsume(String clientKey) {
        LocalDate today = LocalDate.now();
        Window updated = counters.compute(clientKey, (k, w) ->
                (w == null || !w.date().equals(today))
                        ? new Window(today, 1)
                        : new Window(today, w.count() + 1));

        if (updated.count() > dailyLimit) {
            throw new BusinessException(
                    ErrorCode.GUEST_CHAT_LIMIT_EXCEEDED,
                    String.format("비로그인 상태에서는 AI 상담을 하루 %d회까지만 이용할 수 있어요. "
                            + "로그인하면 횟수 제한 없이 이용할 수 있어요.", dailyLimit));
        }
    }

    /**
     * 클라이언트 IP. 원시 X-Forwarded-For를 직접 파싱하면 맨 앞 토큰을 임의로 위조해
     * IP별 한도를 손쉽게 우회할 수 있으므로 사용하지 않는다.
     * 대신 application.yml의 forward-headers-strategy=framework 가 신뢰 가능한 프록시 헤더를
     * 이미 remoteAddr로 해석해 두므로, 그 값을 그대로 사용한다.
     */
    public static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
