package com.regisx001.dQul.storage.minio;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final MinioConfig.Properties minioProperties;

    public MinioStorageService(MinioClient minioClient, MinioConfig.Properties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public FileUploadResult uploadCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty or null file");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "dataset.csv";
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueFilename = UUID.randomUUID() + "_" + sanitizedFilename;
        String bucketName = minioProperties.getBucket() != null ? minioProperties.getBucket() : "csv-uploads";

        // 1. Save local copy for Spark and local file system access
        Path uploadDir = Paths.get("uploads", "csv").toAbsolutePath().normalize();
        Path targetPath;
        try {
            Files.createDirectories(uploadDir);
            targetPath = uploadDir.resolve(uniqueFilename);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("Failed to save local file copy: {}", e.getMessage(), e);
            throw new RuntimeException("Could not save uploaded CSV file locally: " + e.getMessage(), e);
        }

        // 2. Upload to MinIO object storage
        boolean storedInMinio = false;
        String objectName = "csv/" + uniqueFilename;
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }

            try (InputStream is = Files.newInputStream(targetPath)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(is, Files.size(targetPath), -1)
                                .contentType("text/csv")
                                .build());
                storedInMinio = true;
                log.info("Successfully uploaded CSV file to MinIO bucket '{}' object '{}'", bucketName, objectName);
            }
        } catch (Exception e) {
            log.warn("Could not upload file to MinIO (using local file backup at '{}'): {}",
                    targetPath, e.getMessage());
        }

        return new FileUploadResult(
                targetPath.toString(),
                objectName,
                bucketName,
                sanitizedFilename,
                file.getSize(),
                storedInMinio);
    }

    public void deleteCsvFile(String filePath, String objectName, String bucketName) {
        // 1. Delete local file copy from uploads/
        if (filePath != null && !filePath.isBlank()) {
            try {
                Path path = Paths.get(filePath).toAbsolutePath().normalize();
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("Deleted local CSV file copy at '{}'", path);
                }
            } catch (Exception e) {
                log.warn("Could not delete local CSV file copy at '{}': {}", filePath, e.getMessage());
            }
        }

        // 2. Delete object from MinIO
        if (objectName != null && !objectName.isBlank()) {
            String bucket = (bucketName != null && !bucketName.isBlank())
                    ? bucketName
                    : (minioProperties.getBucket() != null ? minioProperties.getBucket() : "csv-uploads");
            try {
                minioClient.removeObject(
                        io.minio.RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .build());
                log.info("Successfully deleted MinIO object '{}' from bucket '{}'", objectName, bucket);
            } catch (Exception e) {
                log.warn("Could not delete object '{}' from MinIO bucket '{}': {}", objectName, bucket, e.getMessage());
            }
        } else if (filePath != null && filePath.contains("uploads")) {
            try {
                Path path = Paths.get(filePath).toAbsolutePath().normalize();
                String fileName = path.getFileName().toString();
                String inferredObjectName = "csv/" + fileName;
                String bucket = minioProperties.getBucket() != null ? minioProperties.getBucket() : "csv-uploads";
                minioClient.removeObject(
                        io.minio.RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(inferredObjectName)
                                .build());
                log.info("Successfully deleted inferred MinIO object '{}' from bucket '{}'", inferredObjectName, bucket);
            } catch (Exception e) {
                log.warn("Could not delete inferred MinIO object for '{}': {}", filePath, e.getMessage());
            }
        }
    }

    public record FileUploadResult(
            String filePath,
            String objectName,
            String bucket,
            String fileName,
            long fileSize,
            boolean storedInMinio) {
    }
}
