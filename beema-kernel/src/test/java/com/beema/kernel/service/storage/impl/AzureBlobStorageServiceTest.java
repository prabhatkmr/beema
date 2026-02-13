package com.beema.kernel.service.storage.impl;

import com.beema.kernel.config.StorageProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class AzureBlobStorageServiceTest {

    // Azurite well-known credentials
    private static final String AZURITE_ACCOUNT_NAME = "devstoreaccount1";
    private static final String AZURITE_ACCOUNT_KEY =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private static final String TEST_CONTAINER = "beema-test-container";

    @Container
    static GenericContainer<?> azuriteContainer = new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite:latest")
            .withExposedPorts(10000)
            .withCommand("azurite-blob --blobHost 0.0.0.0 --blobPort 10000")
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofSeconds(30)));

    private AzureBlobStorageService storageService;

    private static String connectionString() {
        int port = azuriteContainer.getMappedPort(10000);
        return String.format(
                "DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=http://127.0.0.1:%d/%s;",
                AZURITE_ACCOUNT_NAME, AZURITE_ACCOUNT_KEY, port, AZURITE_ACCOUNT_NAME);
    }

    @BeforeAll
    static void createContainer() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString())
                .buildClient();
        serviceClient.createBlobContainer(TEST_CONTAINER);
    }

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setType("azure");
        properties.getAzure().setConnectionString(connectionString());
        properties.getAzure().setContainerName(TEST_CONTAINER);

        storageService = new AzureBlobStorageService(properties);
    }

    @Test
    void shouldUploadAndDownloadBlob() throws IOException {
        // Given
        String path = "test/hello.txt";
        String content = "Hello from Azure!";
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
    void shouldGenerateSasUrl() throws IOException {
        // Given
        String path = "sas/test.txt";
        storageService.upload(path, new ByteArrayInputStream("sas content".getBytes(StandardCharsets.UTF_8)));

        // When
        String url = storageService.getSignedUrl(path, Duration.ofMinutes(15));

        // Then
        assertThat(url).isNotBlank();
        assertThat(url).contains(TEST_CONTAINER);
        assertThat(url).contains("sas/test.txt");
        // SAS tokens contain signature parameters
        assertThat(url).contains("sig=");
        assertThat(url).contains("se=");
    }

    @Test
    void shouldThrowOnDownloadNonExistentBlob() {
        assertThatThrownBy(() -> storageService.download("does-not-exist.txt"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to download from Azure");
    }

    @Test
    void shouldHandleNestedPaths() throws IOException {
        // Given
        String path = "a/b/c/d/deep-file.txt";
        String content = "deeply nested in azure";
        storageService.upload(path, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        // When/Then
        try (InputStream downloaded = storageService.download(path)) {
            String result = new String(downloaded.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(result).isEqualTo(content);
        }
    }

    @Test
    void shouldHandleEmptyBlob() throws IOException {
        // Given
        String path = "empty.dat";
        storageService.upload(path, new ByteArrayInputStream(new byte[0]));

        // Then
        try (InputStream downloaded = storageService.download(path)) {
            assertThat(downloaded.readAllBytes()).isEmpty();
        }
    }
}
