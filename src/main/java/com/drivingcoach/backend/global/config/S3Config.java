package com.drivingcoach.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;   // ✅ v2 빌더 타입
import software.amazon.awssdk.services.s3.S3Configuration;   // (선택) path-style 설정

import java.net.URI;

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
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider provider) {
        // ✅ Builder 타입 수정
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(provider);

        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder
                    .endpointOverride(URI.create(endpoint))
                    // Localstack/MinIO 등은 path-style을 요구하는 경우가 많음
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
            log.info("[S3] Using custom endpoint: {}", endpoint);
        }

        log.info("[S3] Region={}, Bucket={}", region, bucket);
        return builder.build();
    }

    @Bean(name = "s3BucketName")
    public String s3BucketName() {
        return bucket;
    }
}
