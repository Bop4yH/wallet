package com.example.wallet.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transferTopic() {
        return TopicBuilder.name("transfer-notifications")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
