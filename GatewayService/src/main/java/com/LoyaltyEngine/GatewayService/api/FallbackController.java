package com.LoyaltyEngine.GatewayService.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<?>> fallbackEndpoint() {
        return Mono.just(ResponseEntity.status(503).body("Service is unavailable now. Try to refresh the page or wait some minutes."));
    }
}
