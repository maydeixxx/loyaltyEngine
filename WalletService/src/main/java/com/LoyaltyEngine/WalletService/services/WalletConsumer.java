package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.InsufficientFundsException;
import com.LoyaltyEngine.WalletService.exceptions.UserNotExistsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.models.domain.UserStatus;
import com.LoyaltyEngine.WalletService.models.events.CalculatedCashbackEventModel;
import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import com.LoyaltyEngine.WalletService.models.events.UserResponseEventModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("ALL")
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletConsumer {
    private final WalletService walletService;
    private final WalletProducer walletProducer;
    private final Map<String, Object> requests;

    @KafkaListener(
            topics = "${kafka.topics.points-calculated}",
            groupId = "wallet_service",
            containerFactory = "calculatedCashbackEventModelConcurrentKafkaListenerContainerFactory"
    )
    private void handlePointsCalculatedEvent(ConsumerRecord<UUID, CalculatedCashbackEventModel> record, Acknowledgment ack) {
        UUID transactionId = record.key();
        CalculatedCashbackEventModel model = record.value();
        Long userId = model.getUserId();

        UUID requestKey = UUID.randomUUID();
        CompletableFuture<UserResponseEventModel> userEventModel = new CompletableFuture<>();
        requests.put(requestKey.toString(), userEventModel);
        walletProducer.sendUserRequest(requestKey, userId);

        try {
            UserStatus userStatus = userEventModel.get(15, TimeUnit.SECONDS).getUserStatus();
            if (userStatus == UserStatus.NOT_FOUND) {
                log.error("User not found");
                requests.remove(requestKey);
                throw new UserNotExistsException(String.format("User %s wasnt found", userId));
            }
            requests.remove(requestKey);

            walletService.creditPoints(userId, transactionId, model.getAmount(), model.getUseCashback(), model.getAmountOfTransaction(), model.getTotalItemPrice());
            ack.acknowledge();
            walletProducer.sendHandledTransaction(transactionId, userId);
        } catch (UserNotExistsException e) {
            PointsFailedEvent pointsFailedEvent = PointsFailedEvent.builder()
                    .transactionId(transactionId)
                    .userId(userId)
                    .cause("User not exists")
                    .amount(model.getAmount())
                    .failedAt(LocalDateTime.now())
                    .build();

            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailedEvent);
            ack.acknowledge();
        } catch (WalletBlockedException e) {
            PointsFailedEvent pointsFailed = PointsFailedEvent.builder()
                    .transactionId(transactionId)
                    .userId(userId)
                    .cause("Wallet is blocked")
                    .amount(model.getAmount())
                    .failedAt(LocalDateTime.now())
                    .build();

            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailed);
            ack.acknowledge();
        } catch (InsufficientFundsException e) {
            PointsFailedEvent pointsFailed = PointsFailedEvent.builder()
                    .transactionId(transactionId)
                    .userId(userId)
                    .cause("Insufficient funds")
                    .amount(model.getAmount())
                    .failedAt(LocalDateTime.now())
                    .build();

            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailed);
            ack.acknowledge();
        } catch (TimeoutException e) {
            log.error("Timeout waiting for user status response for transaction {}", transactionId);
            PointsFailedEvent pointsFailed = PointsFailedEvent.builder()
                    .transactionId(transactionId)
                    .userId(userId)
                    .cause("User status check timeout")
                    .amount(model.getAmount())
                    .failedAt(LocalDateTime.now())
                    .build();
            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailed);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing cashback for transaction {} : {}", transactionId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "response_user_status",
            groupId = "wallet_service",
            containerFactory = "userResponseEventModelConcurrentKafkaListenerContainerFactory"
    )
    public void handleUserStatusResponse(ConsumerRecord<UUID, UserResponseEventModel> record) {
        UUID requestKey = record.key();
        UserResponseEventModel model = record.value();

        if (requestKey == null || model == null) {
            log.error("Request key or user model is null");
            throw new NullPointerException();
        }

        CompletableFuture<UserResponseEventModel> requestModel = (CompletableFuture<UserResponseEventModel>) requests.get(requestKey.toString());
        requestModel.complete(model);
    }
}
