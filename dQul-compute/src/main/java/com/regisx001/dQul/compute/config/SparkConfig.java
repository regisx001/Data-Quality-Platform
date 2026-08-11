package com.regisx001.dQul.compute.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
@ConditionalOnProperty(name = "spark.enabled", havingValue = "true", matchIfMissing = true)
public class SparkConfig {

    private static final Logger log = LoggerFactory.getLogger(SparkConfig.class);

    private final SparkProperties properties;

    public SparkConfig(SparkProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession() {
        log.info("Initializing SparkSession: master={}, appName={}", properties.getMaster(), properties.getAppName());

        SparkConf conf = new SparkConf()
                .setMaster(properties.getMaster())
                .setAppName(properties.getAppName())
                .set("spark.ui.enabled", String.valueOf(properties.isUiEnabled()))
                .set("spark.driver.host", properties.getDriverHost())
                .set("spark.driver.bindAddress", properties.getDriverBindAddress())
                .set("spark.driver.memory", properties.getDriverMemory())
                .set("spark.executor.memory", properties.getExecutorMemory())
                .set("spark.executor.cores", String.valueOf(properties.getExecutorCores()))
                .set("spark.default.parallelism", String.valueOf(properties.getDefaultParallelism()))
                .set("spark.sql.shuffle.partitions", String.valueOf(properties.getShufflePartitions()));

        if (properties.getJarsPackages() != null && !properties.getJarsPackages().isBlank()) {
            log.info("Configuring Spark jars packages for cluster workers: {}", properties.getJarsPackages());
            conf.set("spark.jars.packages", properties.getJarsPackages());
        }

        // --- S3A (MinIO / S3) Hadoop FileSystem Configuration ---
        SparkProperties.S3 s3 = properties.getS3();
        if (s3 != null) {
            log.info("Configuring Hadoop S3A FileSystem: endpoint={}", s3.getEndpoint());

            conf.set("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .set("spark.hadoop.fs.s3a.endpoint", s3.getEndpoint())
                .set("spark.hadoop.fs.s3a.access.key", s3.getAccessKey())
                .set("spark.hadoop.fs.s3a.secret.key", s3.getSecretKey())
                .set("spark.hadoop.fs.s3a.path.style.access", "true")
                .set("spark.hadoop.fs.s3a.connection.ssl.enabled", "false")
                .set("spark.hadoop.fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
        }

        SparkSession spark = SparkSession.builder()
                .config(conf)
                .getOrCreate();

        // --- Synchronize Hadoop Configuration directly on Driver SparkContext ---
        if (s3 != null) {
            Configuration hadoopConf = spark.sparkContext().hadoopConfiguration();
            hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
            hadoopConf.set("fs.s3a.endpoint", s3.getEndpoint());
            hadoopConf.set("fs.s3a.access.key", s3.getAccessKey());
            hadoopConf.set("fs.s3a.secret.key", s3.getSecretKey());
            hadoopConf.set("fs.s3a.path.style.access", "true");
            hadoopConf.set("fs.s3a.connection.ssl.enabled", "false");
            hadoopConf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");

            log.info("ACTUAL S3A endpoint: {}",
                    hadoopConf.get("fs.s3a.endpoint"));

            log.info("ACTUAL S3A access key configured: {}",
                    hadoopConf.get("fs.s3a.access.key") != null);

            log.info("ACTUAL S3A path style: {}",
                    hadoopConf.get("fs.s3a.path.style.access"));

            log.info("ACTUAL S3A provider: {}",
                    hadoopConf.get("fs.s3a.aws.credentials.provider"));
        }

        return spark;
    }

    @Bean
    public JavaSparkContext javaSparkContext(SparkSession sparkSession) {
        return new JavaSparkContext(sparkSession.sparkContext());
    }
}
