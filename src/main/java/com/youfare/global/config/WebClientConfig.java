package com.youfare.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 외부 API용 WebClient 설정.
 *
 * [최적화 포인트]
 * WebClient는 thread-safe하며 내부에 커넥션 풀을 가진다.
 * 매 요청마다 builder.build()로 새로 만들면 커넥션 풀이 재생성되어 비효율적이므로,
 * 서비스별로 baseUrl·인증 헤더를 미리 설정한 싱글톤 빈을 한 번만 생성해 재사용한다.
 */
@Configuration
public class WebClientConfig {

    // 공공데이터 응답이 클 수 있어 in-memory 버퍼 상한을 16MB로 상향
    private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024;

    @Bean
    public WebClient openAiWebClient(@Value("${openai.base-url}") String baseUrl,
                                     @Value("${openai.api-key}") String apiKey) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }

    @Bean
    public WebClient publicDataWebClient(@Value("${public-data.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }
}
