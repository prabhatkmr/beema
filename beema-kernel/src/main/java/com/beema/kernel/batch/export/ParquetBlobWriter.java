package com.beema.kernel.batch.export;

import com.beema.kernel.service.storage.BlobStorageService;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Writes Avro GenericRecords to Parquet format and uploads to blob storage.
 *
 * Output path pattern: tenant={tenantId}/object=agreement/date={yyyy-MM-dd}/{uuid}.parquet
 *
 * Process:
 * 1. Write records to a local temp Parquet file (Parquet requires random access)
 * 2. Read the temp file into bytes
 * 3. Upload to BlobStorageService
 * 4. Clean up temp file
 */
public class ParquetBlobWriter implements ItemWriter<GenericRecord> {

    private static final Logger log = LoggerFactory.getLogger(ParquetBlobWriter.class);

    private final BlobStorageService blobStorageService;
    private final String tenantId;
    private final Schema schema;

    public ParquetBlobWriter(BlobStorageService blobStorageService, String tenantId, Schema schema) {
        this.blobStorageService = blobStorageService;
        this.tenantId = tenantId;
        this.schema = schema;
    }

    @Override
    public void write(Chunk<? extends GenericRecord> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        File tempFile = File.createTempFile("parquet-export-", ".parquet");
        try {
            writeParquetFile(tempFile, chunk);
            String blobPath = buildBlobPath();
            uploadToStorage(tempFile, blobPath);
            log.info("Exported {} records to {}", chunk.size(), blobPath);
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    private void writeParquetFile(File outputFile, Chunk<? extends GenericRecord> chunk) throws IOException {
        Configuration conf = new Configuration();
        Path path = new Path(outputFile.getAbsolutePath());

        // Delete the temp file so ParquetWriter can create it fresh
        if (outputFile.exists() && !outputFile.delete()) {
            throw new IOException("Cannot delete temp file for ParquetWriter: " + outputFile);
        }

        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(path)
                .withSchema(schema)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withPageSize(1024 * 1024)       // 1MB page size
                .withRowGroupSize(128L * 1024 * 1024) // 128MB row group
                .build()) {

            for (GenericRecord record : chunk) {
                writer.write(record);
            }
        }
    }

    private String buildBlobPath() {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fileId = UUID.randomUUID().toString();
        return String.format("tenant=%s/object=agreement/date=%s/%s.parquet",
                tenantId, date, fileId);
    }

    private void uploadToStorage(File file, String blobPath) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            blobStorageService.upload(blobPath, stream);
        }
    }
}
