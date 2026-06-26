package com.LoyaltyEngine.TransactionService.servicesTests;

import com.LoyaltyEngine.TransactionService.models.domain.Status;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.TransactionService.services.TransactionService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Testcontainers
public class OutboxEventPublisherTests {
    @Autowired
    TransactionService transactionService;

    private static final List<ConsumerRecord<UUID, TransactionCreatedEvent>> receivedRecords = new CopyOnWriteArrayList<>();
    private final List<TransactionItemDomain> items = List.of(
            TransactionItemDomain.createTransactionItem("ELECTRONICS", "LAPTOP", new BigDecimal("102.2"))
    );

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka"));

    @DynamicPropertySource
    static void configProperties(DynamicPropertyRegistry registry) {
        //postgres
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        //kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setup() {
        receivedRecords.clear();
    }

    @BeforeEach()
    void setupTopic() {
        try (AdminClient adminClient = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            adminClient.createTopics(List.of(
                    new NewTopic("transaction_created", 1, (short) 1)
            ));
        }
    }

    @KafkaListener(
            topicPartitions = @TopicPartition(topic = "transaction_created", partitions = {"0"}),
            groupId = "transaction_service",
            containerFactory = "transactionCreatedEventConcurrentKafkaListenerContainerFactory"
    )
    void listen(ConsumerRecord<UUID, TransactionCreatedEvent> record) {
        receivedRecords.add(record);
    }

    @Test
    @DisplayName("Успешная отправка кафка события")
    void successfulKafkaSend() {
        //given
        TransactionDomain transaction = transactionService.createTransaction(1L, new BigDecimal("129.43"), items, UUID.randomUUID(), false);

        //when
        Awaitility.await().atMost(7, TimeUnit.SECONDS)
                .until(() -> !receivedRecords.isEmpty());

        ConsumerRecord<UUID, TransactionCreatedEvent> record = receivedRecords.getFirst();
        UUID id = record.key();
        TransactionCreatedEvent createdTransaction = record.value();

        //then
        Assertions.assertEquals(transaction.getId(), id);
        Assertions.assertEquals(transaction.getUserId(), createdTransaction.getUserId());
        Assertions.assertEquals(transaction.getAmount(), createdTransaction.getAmount());
        Assertions.assertEquals(Status.NEW, transaction.getStatus());
    }
}
