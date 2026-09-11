package com.LoyaltyEngine.TransactionService.servicesTests;

import com.LoyaltyEngine.TransactionService.models.enums.Status;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.TransactionService.services.TransactionService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
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

    private static final Currency currency = Currency.getInstance("USD");
    private static final List<ConsumerRecord<UUID, TransactionCreatedEvent>> receivedRecords = new CopyOnWriteArrayList<>();
    private final List<TransactionItemDomain> items = List.of(
            TransactionItemDomain.createTransactionItem("ELECTRONICS", "LAPTOP", new BigDecimal("102.2"), currency)
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

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEvent> transactionCreatedEventConcurrentKafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
            props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
            factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new UUIDDeserializer(), new JacksonJsonDeserializer<>(TransactionCreatedEvent.class, false)));
            return factory;
        }
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
        UUID userId = UUID.randomUUID();
        TransactionDomain transaction = transactionService.createTransaction(userId, new BigDecimal("102.2"), currency, items, UUID.randomUUID(), false);

        //when
        Awaitility.await().atMost(7, TimeUnit.SECONDS)
                .until(() -> !receivedRecords.isEmpty());

        ConsumerRecord<UUID, TransactionCreatedEvent> record = receivedRecords.getFirst();
        UUID id = record.key();
        TransactionCreatedEvent createdTransaction = record.value();

        //then
        Assertions.assertEquals(transaction.getId().value(), id);
        Assertions.assertEquals(transaction.getUserId().value(), createdTransaction.getUserId());
        Assertions.assertEquals(transaction.getAmount().amount(), createdTransaction.getAmount());
        Assertions.assertEquals(Status.NEW, transaction.getStatus());
    }
}
