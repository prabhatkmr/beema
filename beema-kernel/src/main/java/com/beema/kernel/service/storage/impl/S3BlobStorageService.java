package com.beema.kernel.service.storage.impl;

import com.beema.kernel.config.StorageProperties;
import com.beema.kernel.service.storage.BlobStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "beema.storage.type", havingValue = "s3")
public class S3BlobStorageService implements BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3BlobStorageService.class);

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3BlobStorageService(StorageProperties properties) {
        StorageProperties.S3Properties s3Props = properties.getS3();
        this.bucket = s3Props.getBucket();

        var clientBuilder = S3Client.builder()
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (s3Props.getEndpoint() != null && !s3Props.getEndpoint().isBlank()) {
            URI endpoint = URI.create(s3Props.getEndpoint());
            clientBuilder.endpointOverride(endpoint).forcePathStyle(true);
            presignerBuilder.endpointOverride(endpoint);
        }

        this.s3Client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
        log.info("S3 blob storage initialized for bucket: {}", bucket);
    }

    @Override
    public void upload(String path, InputStream stream) throws IOException {
        try {
            byte[] bytes = stream.readAllBytes();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            log.debug("Uploaded blob to S3: s3://{}/{}", bucket, path);
        } catch (Exception e) {
            throw new IOException("Failed to upload to S3: " + path, e);
        }
    }

    @Override
    public InputStream download(String path) throws IOException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(path)
                    .build();
            return s3Client.getObject(request);
        } catch (Exception e) {
            throw new IOException("Failed to download from S3: " + path, e);
        }
    }

    @Override
    public String getSignedUrl(String path, Duration expiration) throws IOException {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(r -> r.bucket(bucket).key(path))
                    .build();
            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new IOException("Failed to generate signed URL for S3: " + path, e);
        }
    }
}
