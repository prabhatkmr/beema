package com.beema.kernel.service.storage.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.beema.kernel.config.StorageProperties;
import com.beema.kernel.service.storage.BlobStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@ConditionalOnProperty(name = "beema.storage.type", havingValue = "azure")
public class AzureBlobStorageService implements BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);

    private final BlobContainerClient containerClient;

    public AzureBlobStorageService(StorageProperties properties) {
        StorageProperties.AzureProperties azureProps = properties.getAzure();
        this.containerClient = new BlobServiceClientBuilder()
                .connectionString(azureProps.getConnectionString())
                .buildClient()
                .getBlobContainerClient(azureProps.getContainerName());
        log.info("Azure blob storage initialized for container: {}", azureProps.getContainerName());
    }

    @Override
    public void upload(String path, InputStream stream) throws IOException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(path);
            blobClient.upload(stream, true);
            log.debug("Uploaded blob to Azure: {}", path);
        } catch (Exception e) {
            throw new IOException("Failed to upload to Azure: " + path, e);
        }
    }

    @Override
    public InputStream download(String path) throws IOException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(path);
            return blobClient.openInputStream();
        } catch (Exception e) {
            throw new IOException("Failed to download from Azure: " + path, e);
        }
    }

    @Override
    public String getSignedUrl(String path, Duration expiration) throws IOException {
        try {
            BlobClient blobClient = containerClient.getBlobClient(path);
            BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            OffsetDateTime expiryTime = OffsetDateTime.now().plus(expiration);
            BlobServiceSasSignatureValues sasValues =
                    new BlobServiceSasSignatureValues(expiryTime, permission);
            String sasToken = blobClient.generateSas(sasValues);
            return blobClient.getBlobUrl() + "?" + sasToken;
        } catch (Exception e) {
            throw new IOException("Failed to generate signed URL for Azure: " + path, e);
        }
    }
}
