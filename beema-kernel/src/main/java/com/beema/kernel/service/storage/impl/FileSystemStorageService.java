package com.beema.kernel.service.storage.impl;

import com.beema.kernel.config.StorageProperties;
import com.beema.kernel.service.storage.BlobStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "beema.storage.type", havingValue = "filesystem", matchIfMissing = true)
public class FileSystemStorageService implements BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);

    private final Path basePath;

    public FileSystemStorageService(StorageProperties properties) {
        this.basePath = Paths.get(properties.getFilesystem().getBasePath()).toAbsolutePath();
        log.info("Filesystem blob storage initialized at: {}", this.basePath);
    }

    @Override
    public void upload(String path, InputStream stream) throws IOException {
        Path target = resolve(path);
        Files.createDirectories(target.getParent());
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Uploaded blob to filesystem: {}", target);
    }

    @Override
    public InputStream download(String path) throws IOException {
        Path target = resolve(path);
        if (!Files.exists(target)) {
            throw new IOException("Blob not found: " + path);
        }
        return new FileInputStream(target.toFile());
    }

    @Override
    public String getSignedUrl(String path, Duration expiration) throws IOException {
        Path target = resolve(path);
        if (!Files.exists(target)) {
            throw new IOException("Blob not found: " + path);
        }
        return target.toUri().toString();
    }

    private Path resolve(String path) {
        Path resolved = basePath.resolve(path).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal not allowed: " + path);
        }
        return resolved;
    }
}
