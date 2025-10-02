package com.drivingcoach.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ SwaggerConfig (springdoc-openapi)
 *
 * 목적
 *  - Swagger UI(/swagger-ui) 및 OpenAPI 문서(/v3/api-docs)를 구성합니다.
 *  - JWT Bearer 인증 스키마를 등록하여, Swagger UI에서 "Authorize" 버튼으로
 *    액세스 토큰을 입력하고 인증이 필요한 API를 바로 호출할 수 있게 합니다.
 *
 * 사용 방법
 *  1) 애플리케이션 실행
 *  2) 브라우저에서 /swagger-ui 접속
 *  3) 우측 상단 "Authorize" 클릭 → "Bearer {accessToken}" 형식으로 입력 (앞에 Bearer 붙이지 않아도 됨)
 *
 * 참고
 *  - build.gradle 에 springdoc-openapi-starter-webmvc-ui 의존성이 필요합니다.
 *  - application.yml 에서 springdoc 경로를 커스터마이징할 수 있습니다.
 */
@Configuration
public class SwaggerConfig {

    /** Swagger/OpenAPI 메타 정보 + 시큐리티 스키마 등록 */
    @Bean
    public OpenAPI drivingCoachOpenAPI() {
        // JWT Bearer 스키마 정의
        final String securitySchemeName = "bearerAuth";
        SecurityScheme bearer = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer 토큰을 입력하세요. 예) eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");

        // 모든 API 기본 보안 요구(옵션): 인증이 필요한 엔드포인트에서만 적용되므로 부담 없음
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        return new OpenAPI()
                .info(new Info()
                        .title("Driving Coach API")
                        .description("운전 습관 분석·코치 캡스톤 프로젝트의 백엔드 API 문서")
                        .version("v1.0.0")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                        .contact(new Contact().name("DrivingCoach Team").email("team@example.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("README / API 가이드(예시)")
                        .url("https://example.com/docs"))
                .components(new Components().addSecuritySchemes(securitySchemeName, bearer))
                .addSecurityItem(securityRequirement);
    }
}
