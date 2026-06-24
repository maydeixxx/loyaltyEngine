package com.LoyaltyEngine.RuleEngineService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RuleEngineServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RuleEngineServiceApplication.class, args);
	}

}
