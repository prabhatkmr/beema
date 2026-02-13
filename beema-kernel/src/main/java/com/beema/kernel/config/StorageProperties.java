package com.beema.kernel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "beema.storage")
public class StorageProperties {

    private String type = "filesystem";
    private S3Properties s3 = new S3Properties();
    private AzureProperties azure = new AzureProperties();
    private FileSystemProperties filesystem = new FileSystemProperties();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public S3Properties getS3() {
        return s3;
    }

    public void setS3(S3Properties s3) {
        this.s3 = s3;
    }

    public AzureProperties getAzure() {
        return azure;
    }

    public void setAzure(AzureProperties azure) {
        this.azure = azure;
    }

    public FileSystemProperties getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(FileSystemProperties filesystem) {
        this.filesystem = filesystem;
    }

    public static class S3Properties {
        private String bucket;
        private String region = "us-east-1";
        private String endpoint;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }

    public static class AzureProperties {
        private String connectionString;
        private String containerName;

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }
    }

    public static class FileSystemProperties {
        private String basePath = "data/exports";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
