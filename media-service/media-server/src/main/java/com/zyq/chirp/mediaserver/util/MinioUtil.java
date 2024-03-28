package com.zyq.chirp.mediaserver.util;

import com.zyq.chirp.mediaserver.domain.entity.CustomMinioClient;
import com.zyq.chirp.mediaserver.domain.properties.MinioProperties;
import io.minio.CreateMultipartUploadResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MinioUtil {
    // @Resource
    MinioProperties minioProperties;
    // @Resource
    CustomMinioClient minioClient;

    public String upload(MultipartFile file, String path) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .contentType(file.getContentType())
                .object(path)
                .stream(file.getInputStream(), file.getInputStream().available(), -1)
                .build();
        minioClient.putObject(putObjectArgs);
        return STR."\{minioProperties.getEndpoint()}/\{minioProperties.getBucket()}/\{path}";
    }

    public String initMultiPartUpload(String objectName) throws InsufficientDataException, IOException, NoSuchAlgorithmException, XmlParserException {
        return minioClient.createMultipartUpload(minioProperties.getBucket(), minioProperties.getRegion(), objectName, null, null).result().uploadId();
    }

    public String getUploadUrl(String bucketName, String objectName, Map<String, String> queryParams) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(1, TimeUnit.DAYS)
                        .extraQueryParams(queryParams)
                        .build()
        );
    }
}
