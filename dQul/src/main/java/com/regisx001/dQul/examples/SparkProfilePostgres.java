package com.regisx001.dQul.examples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.regisx001.dQul.compute.spark.SparkProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(scanBasePackages = { "com.regisx001.dQul.compute.spark" }, excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.orm.jpa.hibernate.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "org.springframework.boot.devtools.autoconfigure.DevToolsDataSourceAutoConfiguration"
})
@Slf4j
@RequiredArgsConstructor
public class SparkProfilePostgres implements CommandLineRunner {
    private final SparkProvider sparkProvider;
    private List<String> tables = new ArrayList<>();;

    public static void main(String[] args) {
        log.info("Starting Spark  Command Line Application (Web environment disabled)...");
        new SpringApplicationBuilder(SparkProfilePostgres.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("\n[1/4] Testing SparkSession via SparkProvider...");
            if (!sparkProvider.isSparkActive()) {
                System.err.println("[ERROR] SparkProvider reports that SparkSession is NOT active!");
                return;
            }

            SparkSession spark = sparkProvider.getSparkSession();
            System.out.println(" -> SparkProvider active status : " + sparkProvider.isSparkActive());
            System.out.println(" -> Spark App Name             : " + spark.sparkContext().appName());
            System.out.println(" -> Spark Version              : " + spark.version());
            System.out.println(" -> Spark Master               : " + spark.sparkContext().master());

            String jdbcUrl = "jdbc:postgresql://localhost:3452/dqul";
            String user = "postgres";
            String password = "postgres";

            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {

                String sql = """
                            SELECT table_schema,
                                   table_name
                            FROM information_schema.tables
                            WHERE table_schema = 'public'
                            AND table_type = 'BASE TABLE'
                            ORDER BY table_schema, table_name
                        """;

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                        ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        String schema = rs.getString("table_schema");
                        String table = rs.getString("table_name");
                        tables.add(table);
                        System.out.println(schema + "." + table);
                    }
                }
            }
            tables.forEach(table -> System.out.println(table));

            for (String table : tables) {

                Dataset<Row> df = spark.read()
                        .format("jdbc")
                        .option("url", jdbcUrl)
                        .option("dbtable", "public." + table)
                        .option("user", user)
                        .option("password", password)
                        .load();

                String schema = df.schema().treeString();
                System.out.println(schema);
                // Process schema
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Error during dataset validation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
