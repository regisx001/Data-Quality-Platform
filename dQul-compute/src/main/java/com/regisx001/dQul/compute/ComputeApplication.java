package com.regisx001.dQul.compute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ComputeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComputeApplication.class, args);
    }
}
