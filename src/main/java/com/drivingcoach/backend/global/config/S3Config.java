package com.drivingcoach.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * AWS S3 설정 (SDK v2)
 * - 자격증명은 우선순위: 환경변수/프로파일(~/.aws/credentials)/EC2 역할 등 DefaultCredentialsProvider 사용
 * - application.yml 예시
 *   aws:
 *     s3:
 *       region: ap-northeast-2
 *       bucket: your-bucket-name
 *       endpoint: ""  # (선택) 커스텀 엔드포인트 사용 시
 */
@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        // 환경 변수, 프로파일, EC2/ECS 역할 순으로 자동 탐색
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider provider) {
        S3Client.Builder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(provider);

        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint));
            log.info("[S3] Using custom endpoint: {}", endpoint);
        }

        log.info("[S3] Region={}, Bucket={}", region, bucket);
        return builder.build();
    }

    /** 다른 빈에서 주입받아 사용하기 위한 버킷명 */
    @Bean(name = "s3BucketName")
    public String s3BucketName() {
        return bucket;
    }
}
