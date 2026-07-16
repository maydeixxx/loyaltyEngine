package com.LoyaltyEngine.UserService;

import com.LoyaltyEngine.UserService.models.User;
import com.LoyaltyEngine.UserService.models.domain.UserStatus;
import com.LoyaltyEngine.UserService.models.eventModels.UserResponseEventModel;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserConsumer {
    private final UserRepository userRepository;
    private final KafkaTemplate<UUID, UserResponseEventModel> userResponseEventModelKafkaTemplate;

    public UserConsumer(
            @Autowired
            UserRepository userRepository,
            @Qualifier("userResponseEventModelKafkaTemplate")
            KafkaTemplate<UUID, UserResponseEventModel> userResponseEventModelKafkaTemplate
    ) {
        this.userRepository = userRepository;
        this.userResponseEventModelKafkaTemplate = userResponseEventModelKafkaTemplate;
    }

    @KafkaListener(
            topics = "get_user_status",
            groupId = "user_service",
            containerFactory = "longConcurrentKafkaListenerContainerFactory"
    )
    public void handleWalletServiceRequest(ConsumerRecord<UUID, Long> record) {
        UUID requestId = record.key();
        Long userId = record.value();
        Optional<User> userById = userRepository.findUserById(userId);
        UserStatus userStatus = userById.isPresent() ? UserStatus.EXISTS : UserStatus.NOT_FOUND;
        UserResponseEventModel response = UserResponseEventModel.builder()
                .userId(userId)
                .userStatus(userStatus)
                .build();

        userResponseEventModelKafkaTemplate.send("response_user_status", requestId, response).whenComplete(
                (_, ex) -> {
                    if (ex != null) {
                        log.error("Error sending response to wallet service: {}", ex.getMessage());
                    }

                    log.info("Response successfully sent to wallet service");
                }
        );
    }
}
