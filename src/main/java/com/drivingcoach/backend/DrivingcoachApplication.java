package com.drivingcoach.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ✅ DrivingCoachApplication
 *
 * 스프링 부트 메인 클래스입니다.
 *
 * 포함 기능
 *  - @SpringBootApplication : 컴포넌트 스캔 + 자동설정 + 설정 클래스 등록을 통합하는 부트스트랩 애노테이션
 *  - main(...)             : 내장 톰캣을 구동하고 애플리케이션 컨텍스트를 초기화
 *
 * 참고
 *  - JPA 감사(Auditing)는 별도의 {@code JpaConfig}에서 @EnableJpaAuditing 으로 활성화되어 있습니다.
 *  - 보안/웹소켓/S3 등 공통 설정은 global.config 패키지의 각 설정 클래스를 통해 구성됩니다.
 *  - 실행 프로필/환경 변수는 src/main/resources/application.yml 로 관리합니다.
 */
@EnableAsync // 2. 비동기 활성화 애노테이션 추가
@SpringBootApplication
public class DrivingcoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrivingcoachApplication.class, args);
    }
}
