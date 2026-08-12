package com.regisx001.dQul.compute;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spark.master=local[1]",
    "spark.ui.enabled=false",
    "spring.kafka.listener.auto-startup=false"
})
class ComputeApplicationTests {

    @Test
    void contextLoads() {
    }

}
