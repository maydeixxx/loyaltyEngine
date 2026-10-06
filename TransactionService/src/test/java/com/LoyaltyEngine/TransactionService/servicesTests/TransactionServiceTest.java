package com.LoyaltyEngine.TransactionService.servicesTests;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionRepositoryException;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.entity.OutboxEvent;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.models.enums.OutboxStatus;
import com.LoyaltyEngine.TransactionService.models.enums.Status;
import com.LoyaltyEngine.TransactionService.services.TransactionMapper;
import com.LoyaltyEngine.TransactionService.services.TransactionService;
import com.LoyaltyEngine.TransactionService.services.interfaces.OutboxEventRepository;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private ObjectMapper mapper;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final Tracer tracer = Tracer.NOOP;

    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private final UUID transactionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID idempotencyKey = UUID.randomUUID();
    private final List<TransactionItemDomain> items = List.of(
            TransactionItemDomain.createTransactionItem("ELECTRONICS", "Headphones", new BigDecimal("150.00"))
    );

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                mapper,
                transactionRepository,
                transactionMapper,
                outboxEventRepository,
                registry,
                tracer
        );
        ReflectionTestUtils.setField(transactionService, "transactionCreatedTopic", "transaction-created");
        ReflectionTestUtils.setField(transactionService, "cancelTransactionTopic", "transaction-cancel-topic");
    }

    @Test
    @DisplayName("createTransaction: returns existing transaction when idempotency key already exists without saving")
    void createTransaction_idempotencyKeyExists_returnsExistingTransactionWithoutSaving() {
        //given
        Transaction existingEntity = new Transaction();
        existingEntity.setId(transactionId);
        TransactionDomain existingDomain = TransactionDomain.create(userId, idempotencyKey, new BigDecimal("150.00"), items, false);

        when(transactionRepository.getTransactionByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingEntity));
        when(transactionMapper.transactionEntityToDomain(existingEntity)).thenReturn(existingDomain);

        //when
        TransactionDomain result = transactionService.createTransaction(userId, new BigDecimal("150.00"), items, idempotencyKey, false);

        //then
        assertEquals(existingDomain, result);
        verify(transactionRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction: successfully saves new transaction and outbox event")
    void createTransaction_newTransaction_savesTransactionAndOutboxEvent() throws JsonProcessingException {
        //given
        Transaction entity = new Transaction();
        entity.setId(transactionId);
        TransactionDomain domain = TransactionDomain.create(userId, idempotencyKey, new BigDecimal("150.00"), items, false);

        when(transactionRepository.getTransactionByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(transactionMapper.transactionDomainToEntity(any(TransactionDomain.class))).thenAnswer(invocation -> {
            TransactionDomain d = invocation.getArgument(0);
            entity.setId(d.getId().value());
            return entity;
        });
        when(transactionRepository.save(entity)).thenReturn(entity);
        when(mapper.writeValueAsString(any())).thenReturn("{\"transactionId\":\"" + transactionId + "\"}");
        when(transactionMapper.transactionEntityToDomain(entity)).thenAnswer(invocation -> {
            return TransactionDomain.restoreFromExisting(
                    entity.getId(), userId, idempotencyKey, new BigDecimal("150.00"), items, LocalDateTime.now(), Status.NEW, false
            );
        });

        //when
        TransactionDomain result = transactionService.createTransaction(userId, new BigDecimal("150.00"), items, idempotencyKey, false);

        //then
        assertNotNull(result);
        assertEquals(userId, result.getUserId().value());
        verify(transactionRepository).save(entity);
        verify(outboxEventRepository).save(outboxEventCaptor.capture());

        OutboxEvent capturedOutbox = outboxEventCaptor.getValue();
        assertEquals(result.getId().value(), capturedOutbox.getAggregateId());
        assertEquals("transaction-created", capturedOutbox.getEventType());
    }

    @Test
    @DisplayName("createTransaction: throws TransactionRepositoryException on database error")
    void createTransaction_repositoryThrowsDataException_throwsTransactionRepositoryException() {
        //given
        Transaction entity = new Transaction();
        when(transactionRepository.getTransactionByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(transactionMapper.transactionDomainToEntity(any())).thenReturn(entity);
        when(transactionRepository.save(entity)).thenThrow(new DataException("DB error", new SQLException("Connection lost")));

        //when & then
        assertThrows(TransactionRepositoryException.class, () ->
                transactionService.createTransaction(userId, new BigDecimal("150.00"), items, idempotencyKey, false)
        );
    }

    @Test
    @DisplayName("getTransactionById: returns transaction domain when found by ID")
    void getTransactionById_existingId_returnsTransaction() {
        //given
        Transaction entity = new Transaction();
        entity.setId(transactionId);
        TransactionDomain domain = TransactionDomain.create(userId, idempotencyKey, new BigDecimal("150.00"), items, false);

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(domain);

        //when
        TransactionDomain result = transactionService.getTransactionById(transactionId);

        //then
        assertEquals(domain, result);
    }

    @Test
    @DisplayName("getTransactionById: throws TransactionNotFoundException when transaction not found")
    void getTransactionById_missingId_throwsTransactionNotFoundException() {
        //given
        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.empty());

        //when & then
        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.getTransactionById(transactionId)
        );
    }

    @Test
    @DisplayName("getTransactionByIdempotencyKey: returns optional containing transaction when key exists")
    void getTransactionByIdempotencyKey_existingKey_returnsOptionalTransaction() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain domain = TransactionDomain.create(userId, idempotencyKey, new BigDecimal("150.00"), items, false);

        when(transactionRepository.getTransactionByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(domain);

        //when
        Optional<TransactionDomain> result = transactionService.getTransactionByIdempotencyKey(idempotencyKey);

        //then
        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    @DisplayName("getTransactionByUserId: returns list of user transactions")
    void getTransactionByUserId_existingUserId_returnsListOfTransactions() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain domain = TransactionDomain.create(userId, idempotencyKey, new BigDecimal("150.00"), items, false);

        when(transactionRepository.getTransactionsByUserId(userId)).thenReturn(List.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(domain);

        //when
        List<TransactionDomain> result = transactionService.getTransactionByUserId(userId);

        //then
        assertEquals(1, result.size());
        assertEquals(domain, result.getFirst());
    }

    @Test
    @DisplayName("updateStatus: transitions to PROCESSED and saves completed transaction")
    void updateStatus_statusProcessed_completesTransactionAndSaves() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain domain = TransactionDomain.create(userId, idempotencyKey, new BigDecimal("150.00"), items, false);

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(domain);
        when(transactionMapper.transactionDomainToEntity(domain)).thenReturn(entity);

        //when
        transactionService.updateStatus(Status.PROCESSED, transactionId);

        //then
        assertEquals(Status.PROCESSED, domain.getStatus());
        verify(transactionRepository).save(entity);
    }

    @Test
    @DisplayName("updateStatus: transitions to REJECTED and saves rejected transaction")
    void updateStatus_statusRejected_rejectsTransactionAndSaves() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain domain = TransactionDomain.create(userId, idempotencyKey, new BigDecimal("150.00"), items, false);

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(domain);
        when(transactionMapper.transactionDomainToEntity(domain)).thenReturn(entity);

        //when
        transactionService.updateStatus(Status.REJECTED, transactionId);

        //then
        assertEquals(Status.REJECTED, domain.getStatus());
        verify(transactionRepository).save(entity);
    }

    @Test
    @DisplayName("updateStatus: throws TransactionNotFoundException when transaction not found")
    void updateStatus_missingTransactionId_throwsTransactionNotFoundException() {
        //given
        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.empty());

        //when & then
        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.updateStatus(Status.PROCESSED, transactionId)
        );
    }

    @Test
    @DisplayName("updateStatus: throws IllegalArgumentException on invalid status transition")
    void updateStatus_illegalStateTransition_throwsIllegalArgumentException() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain rejectedDomain = TransactionDomain.restoreFromExisting(
                transactionId, userId, idempotencyKey, new BigDecimal("150.00"), items, LocalDateTime.now(), Status.REJECTED, false
        );

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(rejectedDomain);

        //when & then: transition from REJECTED to PROCESSED is illegal
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.updateStatus(Status.PROCESSED, transactionId)
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelTransaction: saves outbox event when transaction is in PROCESSED status")
    void cancelTransaction_processedStatus_savesOutboxEvent() throws JsonProcessingException {
        //given
        Transaction entity = new Transaction();
        TransactionDomain processedDomain = TransactionDomain.restoreFromExisting(
                transactionId, userId, idempotencyKey, new BigDecimal("150.00"), items, LocalDateTime.now(), Status.PROCESSED, false
        );

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(processedDomain);
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        //when
        transactionService.cancelTransaction(transactionId);

        //then
        verify(outboxEventRepository).save(outboxEventCaptor.capture());
        OutboxEvent captured = outboxEventCaptor.getValue();
        assertEquals(transactionId, captured.getAggregateId());
        assertEquals("transaction-cancel-topic", captured.getEventType());
        assertEquals(OutboxStatus.NEW, captured.getStatus());
    }

    @Test
    @DisplayName("cancelTransaction: throws IllegalArgumentException when transaction is already CANCELLED")
    void cancelTransaction_alreadyCancelled_throwsIllegalArgumentException() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain cancelledDomain = TransactionDomain.restoreFromExisting(
                transactionId, userId, idempotencyKey, new BigDecimal("150.00"), items, LocalDateTime.now(), Status.CANCELLED, false
        );

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(cancelledDomain);

        //when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                transactionService.cancelTransaction(transactionId)
        );
        assertEquals("Status already cancelled", ex.getMessage());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelTransaction: throws IllegalArgumentException when transaction is in NEW status")
    void cancelTransaction_newStatus_throwsIllegalArgumentException() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain newDomain = TransactionDomain.restoreFromExisting(
                transactionId, userId, idempotencyKey, new BigDecimal("150.00"), items, LocalDateTime.now(), Status.NEW, false
        );

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(newDomain);

        //when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                transactionService.cancelTransaction(transactionId)
        );
        assertEquals("Cant change status not from processed", ex.getMessage());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelTransaction: throws TransactionNotFoundException when transaction does not exist")
    void cancelTransaction_missingTransaction_throwsTransactionNotFoundException() {
        //given
        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.empty());

        //when & then
        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.cancelTransaction(transactionId)
        );
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: successfully updates status to CANCELLED from PROCESSED")
    void updateStatus_fromProcessedToCancelled_updatesStatusAndSaves() {
        //given
        Transaction entity = new Transaction();
        TransactionDomain processedDomain = TransactionDomain.restoreFromExisting(
                transactionId, userId, idempotencyKey, new BigDecimal("150.00"), items, LocalDateTime.now(), Status.PROCESSED, false
        );

        when(transactionRepository.getTransactionById(transactionId)).thenReturn(Optional.of(entity));
        when(transactionMapper.transactionEntityToDomain(entity)).thenReturn(processedDomain);
        when(transactionMapper.transactionDomainToEntity(processedDomain)).thenReturn(entity);

        //when
        transactionService.updateStatus(Status.CANCELLED, transactionId);

        //then
        assertEquals(Status.CANCELLED, processedDomain.getStatus());
        verify(transactionRepository).save(entity);
    }
}
