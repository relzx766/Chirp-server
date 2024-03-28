import com.zyq.chirp.mediaserver.MediaServerApplication;
import com.zyq.chirp.mediaserver.domain.properties.MinioProperties;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.credentials.AssumeRoleProvider;
import io.minio.credentials.Credentials;
import io.minio.credentials.StaticProvider;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest(classes = MediaServerApplication.class)
public class MediaServerTest {
    @Resource
    MinioProperties minioProperties;

    @Test
    public void minioTest() {
        String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Action": [
                                "s3:GetBucketLocation",
                                "s3:GetObject",
                                "s3:PutObject"
                            ],
                            "Resource": [
                                "arn:aws:s3:::*"
                            ]
                        }
                    ]
                }
                """;
        try {
            AssumeRoleProvider provider = new AssumeRoleProvider(minioProperties.getEndpoint(),
                    minioProperties.getAccessKey(),
                    minioProperties.getSecretKey(),
                    3600,
                    policy,
                    minioProperties.getRegion(),
                    minioProperties.getRoleArn(),
                    minioProperties.getRoleSessionName(),
                    null,
                    null);
            Credentials credentials = provider.fetch();
            StaticProvider staticProvider = new StaticProvider(credentials.accessKey(), credentials.secretKey(), credentials.sessionToken());
            MinioClient minioClient = MinioClient.builder().endpoint(minioProperties.getEndpoint()).credentialsProvider(staticProvider).build();
            File file = new File("D:\\下载\\220px-Donald_Trump_50548265318.png");
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder().bucket(minioProperties.getBucket()).object(file.getName()).contentType("image/png").stream(fileInputStream, fileInputStream.available(), -1).build());
            System.out.println(response.etag());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
