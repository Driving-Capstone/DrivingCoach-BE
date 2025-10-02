package com.drivingcoach.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * - @CreatedDate, @LastModifiedDate 사용을 위해 활성화
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
