package com.regisx001.dQul;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.regisx001.dQul.storage.MinioConfig;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@SpringBootApplication
public class DQulApplication {

	public static void main(String[] args) {
		SpringApplication.run(DQulApplication.class, args);
	}

	// @Bean
	// CommandLineRunner testMinio(MinioClient minioClient, MinioConfig.Properties
	// properties) {
	// return args -> {

	// String content = "Hello MinIO!";
	// byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

	// minioClient.putObject(
	// PutObjectArgs.builder()
	// .bucket(properties.getBucket())
	// .object("test.txt")
	// .stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L)
	// .contentType("text/plain")
	// .build());

	// System.out.println("Successfully uploaded test.txt to MinIO");
	// };
	// }
}
