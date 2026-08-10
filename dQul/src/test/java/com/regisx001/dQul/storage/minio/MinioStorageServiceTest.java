package com.regisx001.dQul.storage.minio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.regisx001.dQul.storage.exception.EmptyFileException;

import io.minio.MinioClient;

class MinioStorageServiceTest {

    @Test
    @DisplayName("uploadCsvFile saves local file and attempts MinIO upload")
    void uploadCsvFile_success() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        MinioConfig.Properties props = new MinioConfig.Properties();
        props.setBucket("test-bucket");

        MinioStorageService service = new MinioStorageService(minioClient, props);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.csv",
                "text/csv",
                "id,name,age\n1,Alice,30\n2,Bob,25".getBytes()
        );

        MinioStorageService.FileUploadResult result = service.uploadCsvFile(file);

        assertNotNull(result);
        assertEquals("sample.csv", result.fileName());
        assertEquals(file.getSize(), result.fileSize());
        assertTrue(result.filePath().endsWith("sample.csv"));
        assertTrue(Files.exists(Paths.get(result.filePath())));

        // Clean up created file
        Files.deleteIfExists(Paths.get(result.filePath()));
    }

    @Test
    @DisplayName("uploadCsvFile throws exception for null or empty file")
    void uploadCsvFile_emptyFile() {
        MinioClient minioClient = mock(MinioClient.class);
        MinioConfig.Properties props = new MinioConfig.Properties();
        MinioStorageService service = new MinioStorageService(minioClient, props);

        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/csv", new byte[0]);

        assertThrows(EmptyFileException.class, () -> service.uploadCsvFile(emptyFile));
    }
}
