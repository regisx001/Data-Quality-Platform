package com.regisx001.dQul.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;

@Configuration
public class MinioConfig {
    @Getter
    @Setter
    public static class Properties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }

    @Bean
    @ConfigurationProperties(prefix = "minio")
    public Properties minioProperties() {
        return new Properties();
    }

    @Bean
    public MinioClient minioClient(Properties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(
                        properties.getAccessKey(),
                        properties.getSecretKey())
                .build();
    }
}
