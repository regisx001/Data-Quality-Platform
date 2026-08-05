package com.regisx001.dQul.logs;

import com.regisx001.dQul.logs.config.KafkaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = {KafkaConfig.LOGS_TOPIC, KafkaConfig.LOGS_DLT_TOPIC})
class LogsApplicationTests {

	@Test
	void contextLoads() {
	}

}
