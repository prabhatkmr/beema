package com.beema.kernel.service.storage.impl;

import com.beema.kernel.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileSystemStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.getFilesystem().setBasePath(tempDir.toString());
        storageService = new FileSystemStorageService(properties);
    }

    @Test
    void shouldUploadAndDownloadBlob() throws IOException {
        // Given
        String path = "test/hello.txt";
        String content = "Hello, blob storage!";
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
    void shouldCreateDirectoriesOnUpload() throws IOException {
        // Given
        String path = "deep/nested/directory/file.txt";
        InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

        // When
        storageService.upload(path, input);

        // Then
        assertThat(tempDir.resolve(path)).exists();
        assertThat(tempDir.resolve("deep/nested/directory")).isDirectory();
    }

    @Test
    void shouldOverwriteExistingBlobOnUpload() throws IOException {
        // Given
        String path = "overwrite.txt";
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
    void shouldThrowOnDownloadNonExistentBlob() {
        assertThatThrownBy(() -> storageService.download("nonexistent.txt"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Blob not found");
    }

    @Test
    void shouldReturnFileUriAsSignedUrl() throws IOException {
        // Given
        String path = "signed-url-test.txt";
        storageService.upload(path, new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

        // When
        String url = storageService.getSignedUrl(path, Duration.ofMinutes(5));

        // Then
        assertThat(url).startsWith("file:");
        assertThat(url).contains("signed-url-test.txt");
    }

    @Test
    void shouldThrowOnSignedUrlForNonExistentBlob() {
        assertThatThrownBy(() -> storageService.getSignedUrl("missing.txt", Duration.ofMinutes(5)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Blob not found");
    }

    @Test
    void shouldRejectPathTraversal() {
        InputStream input = new ByteArrayInputStream("malicious".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storageService.upload("../../etc/passwd", input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal not allowed");
    }

    @Test
    void shouldRejectPathTraversalOnDownload() {
        assertThatThrownBy(() -> storageService.download("../../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal not allowed");
    }

    @Test
    void shouldHandleBinaryData() throws IOException {
        // Given
        String path = "binary.dat";
        byte[] binaryData = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
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
    void shouldHandleEmptyFile() throws IOException {
        // Given
        String path = "empty.txt";
        InputStream input = new ByteArrayInputStream(new byte[0]);

        // When
        storageService.upload(path, input);

        // Then
        try (InputStream downloaded = storageService.download(path)) {
            assertThat(downloaded.readAllBytes()).isEmpty();
        }
    }
}
