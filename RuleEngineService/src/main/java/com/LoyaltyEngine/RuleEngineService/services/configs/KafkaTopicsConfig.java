package com.LoyaltyEngine.RuleEngineService.services.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "kafka.topics")
@Data
@Component
public class KafkaTopicsConfig {
    private String pointsCalculated;
    private String dlqSuffix;
}

