
# -------------------------------------------------------------
# 1) Builder 단계: Gradle로 JAR 빌드
# -------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# (1) Gradle 래퍼와 설정 복사
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew # gradlew에 실행권한 없는 문제 해결
COPY gradle/ gradle/

# (2) 의존성만 먼저 내려받아 캐시
RUN ./gradlew dependencies --no-daemon

# (3) 소스 복사 후 JAR 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# -------------------------------------------------------------
# 2) Runtime 단계: 최소 이미지에 JAR만 복사해 실행
# -------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine
LABEL authors="lcy21"
WORKDIR /app

# (4) 빌드된 JAR만 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

# (5) 컨테이너 포트 노출
EXPOSE 8080

# (6) 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
