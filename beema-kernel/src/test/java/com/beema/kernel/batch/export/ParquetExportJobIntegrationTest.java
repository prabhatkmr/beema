package com.beema.kernel.batch.export;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.service.agreement.AgreementService;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the Parquet export batch pipeline.
 *
 * Test flow:
 * 1. Start PostgreSQL (Testcontainers JDBC) + MinIO containers
 * 2. Insert test agreements with JSONB attributes
 * 3. Launch universalParquetExport batch job
 * 4. Verify job completes successfully
 * 5. Verify Parquet file in MinIO (correct path, record count, schema, data)
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ParquetExportJobIntegrationTest {

    private static final String BUCKET = "test-exports";
    private static final String TENANT_ID = "default";
    private static final String MINIO_USER = "minioadmin";
    private static final String MINIO_PASSWORD = "minioadmin";
    private static final int TEST_RECORD_COUNT = 5;

    @Container
    static GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", MINIO_USER)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD)
            .withCommand("server /data");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        String minioEndpoint = "http://localhost:" + minioContainer.getMappedPort(9000);

        registry.add("beema.storage.type", () -> "s3");
        registry.add("beema.storage.s3.bucket", () -> BUCKET);
        registry.add("beema.storage.s3.region", () -> "us-east-1");
        registry.add("beema.storage.s3.endpoint", () -> minioEndpoint);

        // Set AWS credentials for DefaultCredentialsProvider used by S3BlobStorageService
        System.setProperty("aws.accessKeyId", MINIO_USER);
        System.setProperty("aws.secretAccessKey", MINIO_PASSWORD);
    }

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job universalParquetExportJob;

    @Autowired
    private AgreementService agreementService;

    private S3Client s3Client;

    @BeforeEach
    void setup() {
        s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(
                        "http://localhost:" + minioContainer.getMappedPort(9000)))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO_USER, MINIO_PASSWORD)))
                .forcePathStyle(true)
                .build();

        // Create bucket (ignore if already exists)
        try {
            s3Client.createBucket(b -> b.bucket(BUCKET));
        } catch (Exception ignored) {
        }

        // Insert test agreement data
        insertTestAgreements();
    }

    @Test
    void shouldExportAgreementsToParquetInMinio() throws Exception {
        // Given: Test agreements already inserted in @BeforeEach

        // When: Launch the batch export job
        JobExecution execution = jobLauncher.run(universalParquetExportJob,
                new JobParametersBuilder()
                        .addString("tenantId", TENANT_ID)
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters());

        // Then: Job completes successfully
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // And: Parquet file exists in MinIO at the correct path
        ListObjectsV2Response listResponse = s3Client.listObjectsV2(
                b -> b.bucket(BUCKET).prefix("tenant=" + TENANT_ID));
        List<S3Object> parquetFiles = listResponse.contents().stream()
                .filter(o -> o.key().endsWith(".parquet"))
                .toList();

        assertThat(parquetFiles)
                .as("At least one Parquet file should exist in MinIO")
                .isNotEmpty();

        // And: Path follows the expected pattern
        String key = parquetFiles.get(0).key();
        assertThat(key).matches(
                "tenant=" + TENANT_ID + "/object=agreement/date=\\d{4}-\\d{2}-\\d{2}/[0-9a-f-]+\\.parquet");

        // And: Parquet file contains correct records
        File tempFile = File.createTempFile("test-parquet-", ".parquet");
        try {
            // Download from MinIO
            s3Client.getObject(b -> b.bucket(BUCKET).key(key), tempFile.toPath());

            // Read Parquet file
            List<GenericRecord> records = readParquetFile(tempFile);

            // Assert record count matches inserted data
            assertThat(records)
                    .as("Parquet file should contain all %d test agreements", TEST_RECORD_COUNT)
                    .hasSize(TEST_RECORD_COUNT);

            // Assert schema includes all fixed agreement fields
            org.apache.avro.Schema schema = records.get(0).getSchema();
            assertThat(schema.getField("id")).isNotNull();
            assertThat(schema.getField("agreement_number")).isNotNull();
            assertThat(schema.getField("agreement_type_code")).isNotNull();
            assertThat(schema.getField("market_context")).isNotNull();
            assertThat(schema.getField("status")).isNotNull();
            assertThat(schema.getField("valid_from")).isNotNull();
            assertThat(schema.getField("valid_to")).isNotNull();
            assertThat(schema.getField("transaction_time")).isNotNull();
            assertThat(schema.getField("tenant_id")).isNotNull();
            assertThat(schema.getField("created_by")).isNotNull();
            assertThat(schema.getField("updated_by")).isNotNull();
            assertThat(schema.getField("version")).isNotNull();

            // Assert flattened JSONB attributes are present in schema
            assertThat(schema.getField("attr_vehicle_vin")).isNotNull();
            assertThat(schema.getField("attr_vehicle_year")).isNotNull();
            assertThat(schema.getField("attr_vehicle_make")).isNotNull();
            assertThat(schema.getField("attr_vehicle_model")).isNotNull();
            assertThat(schema.getField("attr_primary_driver_age")).isNotNull();

            // Assert data values are correct
            GenericRecord firstRecord = records.get(0);
            assertThat(firstRecord.get("tenant_id").toString()).isEqualTo(TENANT_ID);
            assertThat(firstRecord.get("market_context").toString()).isEqualTo("RETAIL");
            assertThat(firstRecord.get("agreement_type_code").toString()).isEqualTo("AUTO_POLICY");
            assertThat(firstRecord.get("status").toString()).isEqualTo("DRAFT");
            assertThat(firstRecord.get("attr_vehicle_vin").toString()).isNotBlank();

            // Assert all records have correct tenant
            assertThat(records)
                    .allSatisfy(r -> assertThat(r.get("tenant_id").toString()).isEqualTo(TENANT_ID));
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void insertTestAgreements() {
        String[] makes = {"Honda", "Toyota", "Ford", "BMW", "Tesla"};
        String[] models = {"Accord", "Camry", "Mustang", "X5", "Model 3"};
        int[] years = {2024, 2023, 2022, 2024, 2025};
        int[] driverAges = {35, 28, 45, 52, 30};

        for (int i = 0; i < TEST_RECORD_COUNT; i++) {
            Agreement agreement = new Agreement();
            agreement.setAgreementNumber("TEST-EXPORT-" + String.format("%03d", i + 1));
            agreement.setAgreementTypeCode("AUTO_POLICY");
            agreement.setMarketContext(MarketContext.RETAIL);
            agreement.setStatus(AgreementStatus.DRAFT);
            agreement.setTenantId(TENANT_ID);
            agreement.setDataResidencyRegion("US");
            agreement.setCreatedBy("test-user");
            agreement.setUpdatedBy("test-user");
            agreement.setAttributes(Map.of(
                    "vehicle_vin", "1HGCM82633A" + String.format("%06d", i + 1),
                    "vehicle_year", years[i],
                    "vehicle_make", makes[i],
                    "vehicle_model", models[i],
                    "primary_driver_age", driverAges[i],
                    "annual_mileage", 10000 + (i * 2000)
            ));

            agreementService.createAgreement(agreement);
        }
    }

    @SuppressWarnings("deprecation")
    private List<GenericRecord> readParquetFile(File file) throws Exception {
        List<GenericRecord> records = new ArrayList<>();

        // Delete the downloaded file so we can re-download properly
        // (getObject already wrote to this path)
        try (ParquetReader<GenericRecord> reader = AvroParquetReader
                .<GenericRecord>builder(
                        new org.apache.hadoop.fs.Path(file.getAbsolutePath()))
                .withConf(new Configuration())
                .build()) {

            GenericRecord record;
            while ((record = reader.read()) != null) {
                records.add(record);
            }
        }

        return records;
    }
}
