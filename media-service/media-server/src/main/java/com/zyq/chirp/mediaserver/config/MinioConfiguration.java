package com.zyq.chirp.mediaserver.config;

import com.zyq.chirp.mediaserver.domain.entity.CustomMinioClient;
import com.zyq.chirp.mediaserver.domain.properties.MinioProperties;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {
    @Resource
    MinioProperties minioProperties;

    //@Bean
    public CustomMinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        return new CustomMinioClient(minioClient);
    }

}
