package com.regisx001.dQul.compute;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@EnableCaching
// @RequiredArgsConstructor
public class ComputeApplication {

    // private final SparkSession sparkSession;

    public static void main(String[] args) {
        SpringApplication.run(ComputeApplication.class, args);
    }

    // @Override
    // public void run(String... args) {

    // System.out.println("==================== Test SPARK s3 Read & Write
    // ============================");

    // String csvS3 =
    // "s3a://csv-uploads/csv/9b1ea306-3d46-4a31-9454-1c943905f90a_diabetes.csv";

    // System.out.println("1. Reading file from S3: " + csvS3);
    // Dataset<Row> df = sparkSession.read()
    // .option("header", "true") // Uses first row as header fields
    // .option("inferSchema", "true")
    // .csv(csvS3);

    // df.printSchema();
    // df.show(20);

    // String outputS3 = "s3a://csv-uploads/output/processed_diabetes";
    // System.out.println("2. Writing output file to S3: " + outputS3);
    // df.write()
    // .mode("overwrite")
    // .option("header", "true")
    // .csv(outputS3);

    // System.out.println("==================== Successfully wrote output file to
    // S3! ============================");

    // }

}
