package com.regisx001.dQul.compute.spark;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SparkConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SparkConfiguration.class));

    @Test
    void sparkPropertiesBeanIsRegisteredWhenEnabled() {
        this.contextRunner
                .withPropertyValues("spark.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SparkConfiguration.class);
                    assertThat(context).doesNotHaveBean(SparkProperties.class);
                });
    }
}
