package com.LoyaltyEngine.TransactionService.controllersTests;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.dto.CreateTransaction;
import com.LoyaltyEngine.TransactionService.models.dto.CreateTransactionItem;
import com.LoyaltyEngine.TransactionService.services.TransactionService;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class TransactionControllerTests {
    private final String header = "X-IDEMPOTENCY-KEY";

    @Autowired
    private TransactionRepository repository;

    @BeforeEach
    void clearDb() {
        repository.deleteAll();
    }

    @Autowired
    private TransactionService service;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private final List<CreateTransactionItem> items = List.of(new CreateTransactionItem("ELECTRONICS", "laptop", new BigDecimal("4932.3")));

    @Test
    @DisplayName("Успешное создание транзакции")
    void successfulTransactionCreating() throws Exception {
        //given
        CreateTransaction createTransaction = new CreateTransaction(1L, new BigDecimal("101.2"), items, false);
        UUID idempotencyKey = UUID.randomUUID();

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions")
                        .header(header, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransaction)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(101.2))
                .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey.toString()))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("Создание транзакции с уже существующим IK")
    void createTransactionWithExistingIK() throws Exception {
        //given
        CreateTransaction firstTransaction = new CreateTransaction(1L, new BigDecimal("121.2"), items, false);
        CreateTransaction secondTransaction = new CreateTransaction(2L, new BigDecimal("754.2"), items, false);
        UUID idempotencyKey = UUID.randomUUID();

        //when && then
        //first request
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions")
                        .header(header, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstTransaction))
                )
                .andExpect(status().isCreated());
        //second request
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions")
                        .header(header, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondTransaction))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(121.2))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("Создание транзакции с невалидным id пользователя")
    void createTransactionWithNotValidUserId() throws Exception {
        //given
        CreateTransaction transaction = new CreateTransaction(0L, new BigDecimal("493.2"), items, false);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions")
                .header(header, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transaction))
        )
                .andExpect(status().is(400));

    }

    @Test
    @DisplayName("Создание транзакции с невалидным amount")
    void createTransactionWithNotValidAmount() throws Exception {
        //given
        CreateTransaction transaction = new CreateTransaction(1L, new BigDecimal("0"), items, false);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions")
                        .header(header, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction))
                )
                .andExpect(status().is(400));

    }

    @Test
    @DisplayName("Создание транзакции с невалидным items")
    void createTransactionWithNotValidItems() throws Exception {
        //given
        CreateTransaction transaction = new CreateTransaction(1L, new BigDecimal("0"), List.of(), false);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions")
                        .header(header, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction))
                )
                .andExpect(status().is(400));

    }

    @Test
    @DisplayName("Успешный поиск транзакции по id")
    void successfulFindById() throws Exception {
        //given
        List<TransactionItemDomain> domainItems = items.stream()
                .map(transactionItem -> TransactionItemDomain.createTransactionItem(transactionItem.getCategory(), transactionItem.getName(), transactionItem.getPrice()))
                .toList();

        UUID id = service.createTransaction(1L, new BigDecimal("123.2"), domainItems, UUID.randomUUID(), false).getId();

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions/{id}", id))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.amount").value(123.2))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("Неуспешный поиск транзакции по id")
    void notSuccessfulFindById() throws Exception {
        //when && then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().is(404));
    }

    @Test
    @DisplayName("Успешный поиск транзакций по userId")
    void successfulFindByUserId() throws Exception {
        //given
        List<TransactionItemDomain> domainItems = items.stream()
                .map(transactionItem -> TransactionItemDomain.createTransactionItem(transactionItem.getCategory(), transactionItem.getName(), transactionItem.getPrice()))
                .toList();

        service.createTransaction(1L, new BigDecimal("123.2"), domainItems, UUID.randomUUID(), false);
        service.createTransaction(1L, new BigDecimal("127.2"), domainItems, UUID.randomUUID(), false);
        service.createTransaction(2L, new BigDecimal("163.2"), domainItems, UUID.randomUUID(), false);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions/user/{userId}", 1L))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].amount").value(123.2))
                .andExpect(jsonPath("$[1].amount").value(127.2));
    }

    @Test
    @DisplayName("Поиск транзакций по userId у которого нет транзакций")
    void notSuccessfulFindByUserId() throws Exception {
        //when && then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions/user/{userId}", 1L))
                .andExpect(jsonPath("$.length()").value(0));
    }



}
