package com.regisx001.dQul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
