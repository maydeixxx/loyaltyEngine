package com.LoyaltyEngine.WalletService;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Disabled
@TestPropertySource(properties = {
		"eureka.client.enabled=false"
})
class WalletServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
