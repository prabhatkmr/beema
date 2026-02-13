package com.beema.kernel.service.storage.impl;

import com.beema.kernel.config.StorageProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class S3BlobStorageServiceTest {

    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final String TEST_BUCKET = "beema-test-bucket";

    @Container
    static GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
            .withCommand("server /data")
            .waitingFor(new HttpWaitStrategy()
                    .forPath("/minio/health/ready")
                    .forPort(9000)
                    .withStartupTimeout(Duration.ofSeconds(30)));

    private S3BlobStorageService storageService;

    @BeforeAll
    static void createBucket() {
        String endpoint = "http://localhost:" + minioContainer.getMappedPort(9000);
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)))
                .forcePathStyle(true)
                .build();
        client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
        client.close();
    }

    @BeforeEach
    void setUp() {
        // Set system properties so DefaultCredentialsProvider picks them up
        System.setProperty("aws.accessKeyId", MINIO_ACCESS_KEY);
        System.setProperty("aws.secretAccessKey", MINIO_SECRET_KEY);

        String endpoint = "http://localhost:" + minioContainer.getMappedPort(9000);

        StorageProperties properties = new StorageProperties();
        properties.setType("s3");
        properties.getS3().setBucket(TEST_BUCKET);
        properties.getS3().setRegion("us-east-1");
        properties.getS3().setEndpoint(endpoint);

        storageService = new S3BlobStorageService(properties);
    }

    @Test
    void shouldUploadAndDownloadBlob() throws IOException {
        // Given
        String path = "test/hello.txt";
        String content = "Hello from S3!";
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // When
        storageService.upload(path, input);

        // Then
        try (InputStream downloaded = storageService.download(path)) {
            String result = new String(downloaded.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo(content);
        }
    }

    @Test
    void shouldUploadAndDownloadBinaryData() throws IOException {
        // Given
        String path = "binary/data.bin";
        byte[] binaryData = new byte[1024];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) (i % 256);
        }
        InputStream input = new ByteArrayInputStream(binaryData);

        // When
        storageService.upload(path, input);

        // Then
        try (InputStream downloaded = storageService.download(path)) {
            byte[] result = downloaded.readAllBytes();
            assertThat(result).isEqualTo(binaryData);
        }
    }

    @Test
    void shouldOverwriteExistingBlob() throws IOException {
        // Given
        String path = "overwrite/test.txt";
        storageService.upload(path, new ByteArrayInputStream("original".getBytes(StandardCharsets.UTF_8)));

        // When
        storageService.upload(path, new ByteArrayInputStream("updated".getBytes(StandardCharsets.UTF_8)));

        // Then
        try (InputStream downloaded = storageService.download(path)) {
            String result = new String(downloaded.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo("updated");
        }
    }

    @Test
    void shouldGeneratePresignedUrl() throws IOException {
        // Given
        String path = "presigned/test.txt";
        storageService.upload(path, new ByteArrayInputStream("signed content".getBytes(StandardCharsets.UTF_8)));

        // When
        String url = storageService.getSignedUrl(path, Duration.ofMinutes(15));

        // Then
        assertThat(url).isNotBlank();
        assertThat(url).contains(TEST_BUCKET);
        assertThat(url).contains("presigned/test.txt");
        assertThat(url).contains("X-Amz-Signature");
    }

    @Test
    void shouldThrowOnDownloadNonExistentBlob() {
        assertThatThrownBy(() -> storageService.download("does-not-exist.txt"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to download from S3");
    }

    @Test
    void shouldHandleNestedPaths() throws IOException {
        // Given
        String path = "a/b/c/d/deep-file.txt";
        String content = "deeply nested";
        storageService.upload(path, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        // When/Then
        try (InputStream downloaded = storageService.download(path)) {
            String result = new String(downloaded.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo(content);
        }
    }
}
