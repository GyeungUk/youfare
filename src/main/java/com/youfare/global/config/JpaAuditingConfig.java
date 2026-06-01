package com.youfare.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @EnableJpaAuditing을 YoufareApplication에서 분리한 이유:
 * @SpringBootApplication과 같은 클래스에 두면 @WebMvcTest 같은 슬라이스 테스트 시
 * JPA 관련 빈을 못 찾아 컨텍스트 로딩 실패가 발생할 수 있음.
 * 별도 @Configuration으로 분리하면 슬라이스 테스트 시 exclude 가능.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
