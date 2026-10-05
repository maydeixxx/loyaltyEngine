package com.LoyaltyEngine.WalletService.service;

import com.LoyaltyEngine.WalletService.exceptions.WalletExistsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletNotFoundException;
import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.entity.OutboxEvent;
import com.LoyaltyEngine.WalletService.models.entity.Wallet;
import com.LoyaltyEngine.WalletService.models.entity.WalletTransaction;
import com.LoyaltyEngine.WalletService.models.enums.WalletStatus;
import com.LoyaltyEngine.WalletService.services.OutboxEventService;
import com.LoyaltyEngine.WalletService.services.WalletMapper;
import com.LoyaltyEngine.WalletService.services.WalletService;
import com.LoyaltyEngine.WalletService.services.WalletTransactionMapper;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletRepository;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private WalletMapper walletMapper;
    @Mock
    private WalletTransactionMapper walletTransactionMapper;
    @Mock
    private OutboxEventService outboxEventService;
    @Mock
    private ObjectMapper mapper;

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final Tracer tracer = Tracer.NOOP;

    private WalletService walletService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private final UUID userId = UUID.randomUUID();
    private final UUID walletId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        walletService = new WalletService(
                walletRepository,
                walletTransactionRepository,
                walletMapper,
                walletTransactionMapper,
                outboxEventService,
                mapper,
                registry,
                tracer
        );
        ReflectionTestUtils.setField(walletService, "transactionHandled", "transaction-handled-topic");
        ReflectionTestUtils.setField(walletService, "pointsFailed", "points-failed-topic");
    }

    @Test
    @DisplayName("createWallet: successfully creates and saves new wallet")
    void createWallet_newUserId_createsAndSavesWallet() {
        //given
        Wallet entity = new Wallet();
        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.empty());
        when(walletMapper.domainToEntity(any(WalletDomain.class))).thenReturn(entity);

        //when
        walletService.createWallet(userId);

        //then
        verify(walletRepository).save(entity);
    }

    @Test
    @DisplayName("createWallet: throws WalletExistsException when wallet already exists")
    void createWallet_walletAlreadyExists_throwsWalletExistsException() {
        //given
        Wallet entity = new Wallet();
        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(entity));

        //when & then
        assertThrows(WalletExistsException.class, () -> walletService.createWallet(userId));
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("creditPoints: returns early when transaction is already processed")
    void creditPoints_transactionAlreadyProcessed_returnsEarly() throws JsonProcessingException {
        //given
        Wallet walletEntity = new Wallet();
        WalletDomain walletDomain = WalletDomain.createWallet(userId);

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);
        when(walletTransactionRepository.findWalletTransactionByTransactionId(transactionId))
                .thenReturn(Optional.of(new WalletTransaction()));

        //when
        walletService.creditPoints(userId, transactionId, new BigDecimal("10.00"), false, new BigDecimal("100.00"), new BigDecimal("100.00"));

        //then
        verify(outboxEventService, never()).saveNewOutboxEvent(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("creditPoints: saves PointsFailedEvent when transaction amount exceeds total item price")
    void creditPoints_amountGreaterThanItemPrice_savesPointsFailedOutboxEvent() throws JsonProcessingException {
        //given
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        //when: amountOfTransaction (150) > totalItemPrice (100)
        walletService.creditPoints(userId, transactionId, new BigDecimal("10.00"), false, new BigDecimal("150.00"), new BigDecimal("100.00"));

        //then
        verify(outboxEventService).saveNewOutboxEvent(outboxEventCaptor.capture());
        assertEquals("points-failed-topic", outboxEventCaptor.getValue().getEventType());
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("creditPoints: saves PointsFailedEvent when wallet is not found")
    void creditPoints_walletNotFound_savesPointsFailedOutboxEvent() throws JsonProcessingException {
        //given
        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.empty());
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        //when
        walletService.creditPoints(userId, transactionId, new BigDecimal("10.00"), false, new BigDecimal("100.00"), new BigDecimal("100.00"));

        //then
        verify(outboxEventService).saveNewOutboxEvent(outboxEventCaptor.capture());
        assertEquals("points-failed-topic", outboxEventCaptor.getValue().getEventType());
    }

    @Test
    @DisplayName("creditPoints: saves PointsFailedEvent when wallet is blocked")
    void creditPoints_walletBlocked_savesPointsFailedOutboxEvent() throws JsonProcessingException {
        //given
        Wallet walletEntity = new Wallet();
        WalletDomain blockedWallet = WalletDomain.restoreFromExisting(
                walletId, userId, BigDecimal.ZERO, WalletStatus.BLOCKED, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(blockedWallet);
        when(walletTransactionRepository.findWalletTransactionByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        //when
        walletService.creditPoints(userId, transactionId, new BigDecimal("10.00"), false, new BigDecimal("100.00"), new BigDecimal("100.00"));

        //then
        verify(outboxEventService).saveNewOutboxEvent(outboxEventCaptor.capture());
        assertEquals("points-failed-topic", outboxEventCaptor.getValue().getEventType());
    }

    @Test
    @DisplayName("creditPoints: saves PointsFailedEvent when wallet balance is insufficient for cashback")
    void creditPoints_insufficientFunds_savesPointsFailedOutboxEvent() throws JsonProcessingException {
        //given
        Wallet walletEntity = new Wallet();
        // Balance 10.00, but redemption requires 50.00 (100.00 - 50.00)
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(
                walletId, userId, new BigDecimal("10.00"), WalletStatus.ACTIVE, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);
        when(walletTransactionRepository.findWalletTransactionByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        //when
        walletService.creditPoints(userId, transactionId, new BigDecimal("5.00"), true, new BigDecimal("50.00"), new BigDecimal("100.00"));

        //then
        verify(outboxEventService).saveNewOutboxEvent(outboxEventCaptor.capture());
        assertEquals("points-failed-topic", outboxEventCaptor.getValue().getEventType());
    }

    @Test
    @DisplayName("creditPoints: credits points and saves TransactionHandledEvent without cashback redemption")
    void creditPoints_withoutCashback_creditsPointsAndSavesTransactionHandled() throws JsonProcessingException {
        //given
        Wallet walletEntity = new Wallet();
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(
                walletId, userId, new BigDecimal("50.00"), WalletStatus.ACTIVE, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);
        when(walletTransactionRepository.findWalletTransactionByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        //when: crediting 10.00 points
        walletService.creditPoints(userId, transactionId, new BigDecimal("10.00"), false, new BigDecimal("100.00"), new BigDecimal("100.00"));

        //then
        assertEquals(new BigDecimal("60.00"), walletDomain.getBalance().amount());
        verify(walletRepository).save(any());
        verify(walletTransactionRepository).save(any());
        verify(outboxEventService).saveNewOutboxEvent(outboxEventCaptor.capture());
        assertEquals("transaction-handled-topic", outboxEventCaptor.getValue().getEventType());
    }

    @Test
    @DisplayName("creditPoints: debits cashback and saves TransactionHandledEvent upon successful redemption")
    void creditPoints_withCashbackRedeem_debitsCashbackAndSavesTransactionHandled() throws JsonProcessingException {
        //given
        Wallet walletEntity = new Wallet();
        // Balance 50.00, debit 30.00 (100.00 - 70.00)
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(
                walletId, userId, new BigDecimal("50.00"), WalletStatus.ACTIVE, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);
        when(walletTransactionRepository.findWalletTransactionByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(mapper.writeValueAsString(any())).thenReturn("{}");

        //when
        walletService.creditPoints(userId, transactionId, new BigDecimal("5.00"), true, new BigDecimal("70.00"), new BigDecimal("100.00"));

        //then: 50.00 - 30.00 = 20.00
        assertEquals(new BigDecimal("20.00"), walletDomain.getBalance().amount());
        verify(walletRepository).save(any());
        verify(walletTransactionRepository).save(any());
        verify(outboxEventService).saveNewOutboxEvent(outboxEventCaptor.capture());
        assertEquals("transaction-handled-topic", outboxEventCaptor.getValue().getEventType());
    }

    @Test
    @DisplayName("creditPoints: throws RuntimeException on technical error without saving outbox event")
    void creditPoints_unexpectedException_throwsRuntimeException() {
        //given
        Wallet walletEntity = new Wallet();
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(
                walletId, userId, new BigDecimal("50.00"), WalletStatus.ACTIVE, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);
        when(walletTransactionRepository.findWalletTransactionByTransactionId(transactionId)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenThrow(new RuntimeException("DB Connection down"));

        //when & then
        assertThrows(RuntimeException.class, () ->
                walletService.creditPoints(userId, transactionId, new BigDecimal("10.00"), false, new BigDecimal("100.00"), new BigDecimal("100.00"))
        );
        verify(outboxEventService, never()).saveNewOutboxEvent(any());
    }

    @Test
    @DisplayName("blockWallet: sets active wallet status to BLOCKED and saves")
    void blockWallet_activeWallet_blocksAndSaves() {
        //given
        Wallet walletEntity = new Wallet();
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(
                walletId, userId, BigDecimal.ZERO, WalletStatus.ACTIVE, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);

        //when
        walletService.blockWallet(userId);

        //then
        assertEquals(WalletStatus.BLOCKED, walletDomain.getStatus());
        verify(walletRepository).save(any());
    }

    @Test
    @DisplayName("unblockWallet: sets blocked wallet status to ACTIVE and saves")
    void unblockWallet_blockedWallet_activatesAndSaves() {
        //given
        Wallet walletEntity = new Wallet();
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(
                walletId, userId, BigDecimal.ZERO, WalletStatus.BLOCKED, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);

        //when
        walletService.unblockWallet(userId);

        //then
        assertEquals(WalletStatus.ACTIVE, walletDomain.getStatus());
        verify(walletRepository).save(any());
    }

    @Test
    @DisplayName("getBalance: returns correct wallet balance for existing user")
    void getBalance_existingUser_returnsBalance() {
        //given
        Wallet walletEntity = new Wallet();
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(
                walletId, userId, new BigDecimal("150.75"), WalletStatus.ACTIVE, 1L, LocalDateTime.now(), LocalDateTime.now()
        );

        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.of(walletEntity));
        when(walletMapper.entityToDomain(walletEntity)).thenReturn(walletDomain);

        //when
        BigDecimal balance = walletService.getBalance(userId);

        //then
        assertEquals(new BigDecimal("150.75"), balance);
    }

    @Test
    @DisplayName("findWalletByUserId: throws WalletNotFoundException when wallet not found")
    void findWalletByUserId_missingUser_throwsWalletNotFoundException() {
        //given
        when(walletRepository.findWalletByUserId(userId)).thenReturn(Optional.empty());

        //when & then
        assertThrows(WalletNotFoundException.class, () -> walletService.findWalletByUserId(userId));
    }
}
