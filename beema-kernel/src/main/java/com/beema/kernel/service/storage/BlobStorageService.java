package com.beema.kernel.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * Abstraction for blob/object storage operations.
 *
 * Implementations:
 * - S3 (AWS)
 * - Azure Blob Storage
 * - Local filesystem (default, for dev/testing)
 */
public interface BlobStorageService {

    /**
     * Upload a blob to the given path.
     *
     * @param path   the storage path/key
     * @param stream the data to upload
     * @throws IOException if the upload fails
     */
    void upload(String path, InputStream stream) throws IOException;

    /**
     * Download a blob from the given path.
     *
     * @param path the storage path/key
     * @return an InputStream of the blob contents
     * @throws IOException if the download fails
     */
    InputStream download(String path) throws IOException;

    /**
     * Generate a time-limited signed URL for direct access to a blob.
     *
     * @param path       the storage path/key
     * @param expiration how long the URL should remain valid
     * @return a signed URL string
     * @throws IOException if URL generation fails
     */
    String getSignedUrl(String path, Duration expiration) throws IOException;
}
