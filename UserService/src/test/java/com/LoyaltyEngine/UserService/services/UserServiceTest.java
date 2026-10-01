package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.AuthenticationException;
import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
import com.LoyaltyEngine.UserService.exceptions.UserUpdateException;
import com.LoyaltyEngine.UserService.models.entity.User;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateEmailDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdatePasswordDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import com.LoyaltyEngine.UserService.models.enums.OutboxStatus;
import com.LoyaltyEngine.UserService.models.enums.Role;
import com.LoyaltyEngine.UserService.services.interfaces.OutboxEventRepository;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import com.LoyaltyEngine.UserService.services.security.JwtService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Spy
    private MeterRegistry registry = new SimpleMeterRegistry();
    @Mock
    private Tracer tracer;
    @Mock
    private ScopedSpan scopedSpan;

    @InjectMocks
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private static final String TOPIC_NAME = "user_created";
    private static final String TEST_EMAIL = "alex@example.com";
    private static final String TEST_FIRST_NAME = "Alex";
    private static final String TEST_LAST_NAME = "Smith";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHash12345";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "userCreatedTopic", TOPIC_NAME);
        lenient().when(tracer.startScopedSpan(any(String.class))).thenReturn(scopedSpan);
    }

    // ==========================================
    // createUser scenarios
    // ==========================================

    @Test
    void createUser_validUser_savesUserAndOutboxEventAndReturnsDomain() {
        //given
        CreateUserDTO dto = new CreateUserDTO(TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, RAW_PASSWORD);
        User entityToSave = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userMapper.domainToEntity(any(UserDomain.class))).thenReturn(entityToSave);

        //when
        UserDomain created = userService.createUser(dto);

        //then
        verify(userRepository).saveAndFlush(entityToSave);
        verify(outboxEventRepository).saveAndFlush(outboxEventCaptor.capture());

        OutboxEvent capturedEvent = outboxEventCaptor.getValue();
        assertAll(
                () -> assertEquals(TEST_EMAIL, created.getEmail()),
                () -> assertEquals(TEST_FIRST_NAME, created.getFirstName()),
                () -> assertEquals(TEST_LAST_NAME, created.getLastName()),
                () -> assertEquals(TOPIC_NAME, capturedEvent.getEventType()),
                () -> assertEquals(OutboxStatus.NEW, capturedEvent.getStatus()),
                () -> assertEquals(0, capturedEvent.getRetryCount()),
                () -> assertEquals(created.getId().value(), capturedEvent.getAggregateId()),
                () -> assertEquals(created.getId().value().toString(), capturedEvent.getPayload()),
                () -> assertNotNull(capturedEvent.getCreatedAt())
        );
    }

    @Test
    void createUser_emailAlreadyExists_throwsCreateUserException() {
        //given
        CreateUserDTO dto = new CreateUserDTO(TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, RAW_PASSWORD);
        User existingUser = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

        //when & then
        CreateUserException exception = assertThrows(
                CreateUserException.class,
                () -> userService.createUser(dto)
        );
        assertEquals("User with email [%s] exists".formatted(TEST_EMAIL), exception.getMessage());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void createUser_repositoryThrowsException_throwsRuntimeException() {
        //given
        CreateUserDTO dto = new CreateUserDTO(TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, RAW_PASSWORD);
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("Database down"));

        //when & then
        assertThrows(
                RuntimeException.class,
                () -> userService.createUser(dto)
        );
        verifyNoInteractions(outboxEventRepository);
    }

    // ==========================================
    // deleteUser scenarios
    // ==========================================

    @Test
    void deleteUser_existingUser_deletesUser() {
        //given
        User user = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

        //when
        userService.deleteUser(TEST_EMAIL);

        //then
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_missingUser_throwsUserNotFoundException() {
        //given
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        //when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(TEST_EMAIL)
        );
        assertEquals("User by email [%s] not found".formatted(TEST_EMAIL), exception.getMessage());
    }

    @Test
    void deleteUser_repositoryThrowsException_throwsRuntimeException() {
        //given
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("DB error"));

        //when & then
        assertThrows(
                RuntimeException.class,
                () -> userService.deleteUser(TEST_EMAIL)
        );
    }

    // ==========================================
    // findUserById scenarios
    // ==========================================

    @Test
    void findUserById_existingId_returnsUserDomain() {
        //given
        UUID id = UUID.randomUUID();
        User user = buildUserEntity(id, TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(id, TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserById(id)).thenReturn(Optional.of(user));
        when(userMapper.entityToDomain(user)).thenReturn(domain);

        //when
        UserDomain result = userService.findUserById(id);

        //then
        assertEquals(domain, result);
    }

    @Test
    void findUserById_missingId_throwsUserNotFoundException() {
        //given
        UUID id = UUID.randomUUID();
        when(userRepository.findUserById(id)).thenReturn(Optional.empty());

        //when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.findUserById(id)
        );
        assertEquals("User by id [%s] not found".formatted(id), exception.getMessage());
    }

    @Test
    void findUserById_repositoryThrowsException_throwsRuntimeException() {
        //given
        UUID id = UUID.randomUUID();
        when(userRepository.findUserById(id)).thenThrow(new RuntimeException("DB error"));

        //when & then
        assertThrows(
                RuntimeException.class,
                () -> userService.findUserById(id)
        );
    }

    // ==========================================
    // getAllUsers scenarios
    // ==========================================

    @Test
    void getAllUsers_usersExist_returnsUserDomainList() {
        //given
        User user1 = buildUserEntity(UUID.randomUUID(), "u1@test.com", "U1", "L1", "h1");
        User user2 = buildUserEntity(UUID.randomUUID(), "u2@test.com", "U2", "L2", "h2");
        UserDomain d1 = buildUserDomain(user1.getId(), user1.getEmail(), user1.getFirstName(), user1.getLastName(), "h1");
        UserDomain d2 = buildUserDomain(user2.getId(), user2.getEmail(), user2.getFirstName(), user2.getLastName(), "h2");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(userMapper.entityToDomain(user1)).thenReturn(d1);
        when(userMapper.entityToDomain(user2)).thenReturn(d2);

        //when
        List<UserDomain> result = userService.getAllUsers();

        //then
        assertEquals(2, result.size());
        assertEquals(List.of(d1, d2), result);
    }

    @Test
    void getAllUsers_repositoryThrowsException_throwsRuntimeException() {
        //given
        when(userRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        //when & then
        assertThrows(
                RuntimeException.class,
                () -> userService.getAllUsers()
        );
    }

    // ==========================================
    // updateUser scenarios
    // ==========================================

    @Test
    void updateUser_missingUser_throwsUserNotFoundException() {
        //given
        UpdateUserDTO dto = new UpdateUserDTO("Alexander", "Johnson");
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        //when & then
        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(TEST_EMAIL, dto)
        );
    }

    @Test
    void updateUser_validFirstName_updatesFirstNameAndSaves() {
        //given
        String newFirstName = "Alexander";
        UpdateUserDTO dto = new UpdateUserDTO(newFirstName, null);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(userMapper.entityToDomain(entity)).thenReturn(domain);
        when(userMapper.domainToEntity(domain)).thenReturn(entity);

        //when
        userService.updateUser(TEST_EMAIL, dto);

        //then
        assertEquals(newFirstName, domain.getFirstName());
        verify(userRepository).save(entity);
    }

    @Test
    void updateUser_sameFirstName_throwsUserUpdateException() {
        //given
        UpdateUserDTO dto = new UpdateUserDTO(TEST_FIRST_NAME, null);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(userMapper.entityToDomain(entity)).thenReturn(domain);

        //when & then
        UserUpdateException exception = assertThrows(
                UserUpdateException.class,
                () -> userService.updateUser(TEST_EMAIL, dto)
        );
        assertEquals("You already have this first name", exception.getMessage());
    }

    @Test
    void updateUser_validLastName_updatesLastNameAndSaves() {
        //given
        String newLastName = "Johnson";
        UpdateUserDTO dto = new UpdateUserDTO(null, newLastName);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(userMapper.entityToDomain(entity)).thenReturn(domain);
        when(userMapper.domainToEntity(domain)).thenReturn(entity);

        //when
        userService.updateUser(TEST_EMAIL, dto);

        //then
        assertEquals(newLastName, domain.getLastName());
        verify(userRepository).save(entity);
    }

    @Test
    void updateUser_sameLastName_throwsUserUpdateException() {
        //given
        UpdateUserDTO dto = new UpdateUserDTO(null, TEST_LAST_NAME);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(userMapper.entityToDomain(entity)).thenReturn(domain);

        //when & then
        UserUpdateException exception = assertThrows(
                UserUpdateException.class,
                () -> userService.updateUser(TEST_EMAIL, dto)
        );
        assertEquals("You already have this last name", exception.getMessage());
    }

    @Test
    void updateUser_validBothNames_updatesBothAndSaves() {
        //given
        String newFirstName = "Alexander";
        String newLastName = "Johnson";
        UpdateUserDTO dto = new UpdateUserDTO(newFirstName, newLastName);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(userMapper.entityToDomain(entity)).thenReturn(domain);
        when(userMapper.domainToEntity(domain)).thenReturn(entity);

        //when
        userService.updateUser(TEST_EMAIL, dto);

        //then
        assertAll(
                () -> assertEquals(newFirstName, domain.getFirstName()),
                () -> assertEquals(newLastName, domain.getLastName())
        );
        verify(userRepository).save(entity);
    }

    @Test
    void updateUser_repositoryThrowsException_throwsRuntimeException() {
        //given
        UpdateUserDTO dto = new UpdateUserDTO("Alexander", "Johnson");
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("DB error"));

        //when & then
        assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(TEST_EMAIL, dto)
        );
    }

    // ==========================================
    // findUserByEmail scenarios
    // ==========================================

    @Test
    void findUserByEmail_existingEmail_returnsUserDomain() {
        //given
        User user = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(user.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.entityToDomain(user)).thenReturn(domain);

        //when
        UserDomain result = userService.findUserByEmail(TEST_EMAIL);

        //then
        assertEquals(domain, result);
    }

    @Test
    void findUserByEmail_missingEmail_throwsUserNotFoundException() {
        //given
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        //when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.findUserByEmail(TEST_EMAIL)
        );
        assertEquals("User by email [%s] not found".formatted(TEST_EMAIL), exception.getMessage());
    }

    @Test
    void findUserByEmail_repositoryThrowsException_throwsRuntimeException() {
        //given
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("DB error"));

        //when & then
        assertThrows(
                RuntimeException.class,
                () -> userService.findUserByEmail(TEST_EMAIL)
        );
    }

    // ==========================================
    // login scenarios
    // ==========================================

    @Test
    void login_validCredentials_returnsJwtToken() {
        //given
        AuthUserDto authDto = new AuthUserDto(TEST_EMAIL, RAW_PASSWORD);
        User user = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        String expectedToken = "mocked.jwt.token";

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtService.generateJwtToken(user)).thenReturn(expectedToken);

        //when
        String token = authService.login(authDto);

        //then
        assertEquals(expectedToken, token);
    }

    @Test
    void login_userNotFound_throwsUserNotFoundException() {
        //given
        AuthUserDto authDto = new AuthUserDto(TEST_EMAIL, RAW_PASSWORD);
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        //when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authService.login(authDto)
        );
        assertEquals("User by email [%s] not found".formatted(TEST_EMAIL), exception.getMessage());
    }

    @Test
    void login_incorrectPassword_throwsAuthenticationException() {
        //given
        AuthUserDto authDto = new AuthUserDto(TEST_EMAIL, "WrongPass");
        User user = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", ENCODED_PASSWORD)).thenReturn(false);

        //when & then
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(authDto)
        );
        assertEquals("Password is incorrect", exception.getMessage());
    }

    @Test
    void login_repositoryThrowsException_throwsRuntimeException() {
        //given
        AuthUserDto authDto = new AuthUserDto(TEST_EMAIL, RAW_PASSWORD);
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("DB error"));

        //when & then
        assertThrows(
                RuntimeException.class,
                () -> authService.login(authDto)
        );
    }

    // ==========================================
    // updateUserPassword scenarios
    // ==========================================

    @Test
    void updateUserPassword_validCredentials_updatesPasswordAndSaves() {
        //given
        String oldPassword = RAW_PASSWORD;
        String newPassword = "NewValidPassword123!";
        String newEncodedPassword = "$2a$10$newEncodedHash9999";
        UpdatePasswordDTO dto = new UpdatePasswordDTO(oldPassword, newPassword);

        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches(oldPassword, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, ENCODED_PASSWORD)).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
        when(userMapper.entityToDomain(entity)).thenReturn(domain);
        when(userMapper.domainToEntity(domain)).thenReturn(entity);

        //when
        authService.updateUserPassword(TEST_EMAIL, dto);

        //then
        assertEquals(newEncodedPassword, domain.getPasswordHash().value());
        verify(userRepository).save(entity);
    }

    @Test
    void updateUserPassword_wrongOldPassword_throwsUserUpdateException() {
        //given
        UpdatePasswordDTO dto = new UpdatePasswordDTO("WrongPassword!", "NewValidPassword123!");
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("WrongPassword!", ENCODED_PASSWORD)).thenReturn(false);

        //when & then
        UserUpdateException exception = assertThrows(
                UserUpdateException.class,
                () -> authService.updateUserPassword(TEST_EMAIL, dto)
        );
        assertEquals("Old password is incorrect", exception.getMessage());
    }

    @Test
    void updateUserPassword_samePassword_throwsUserUpdateException() {
        //given
        UpdatePasswordDTO dto = new UpdatePasswordDTO(RAW_PASSWORD, RAW_PASSWORD);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        //when & then
        UserUpdateException exception = assertThrows(
                UserUpdateException.class,
                () -> authService.updateUserPassword(TEST_EMAIL, dto)
        );
        assertEquals("New password cant be same as old", exception.getMessage());
    }

    @Test
    void updateUserPassword_userNotFound_throwsUserNotFoundException() {
        //given
        UpdatePasswordDTO dto = new UpdatePasswordDTO(RAW_PASSWORD, "NewValidPassword123!");
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        //when & then
        assertThrows(
                UserNotFoundException.class,
                () -> authService.updateUserPassword(TEST_EMAIL, dto)
        );
    }

    // ==========================================
    // updateEmail scenarios
    // ==========================================

    @Test
    void updateEmail_validNewEmail_updatesAndSavesAndReturnsJwtToken() {
        //given
        String newEmail = "newemail@example.com";
        UpdateEmailDTO dto = new UpdateEmailDTO(newEmail);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        String expectedToken = "mocked.new.jwt.token";

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(userMapper.entityToDomain(entity)).thenReturn(domain);
        when(userRepository.findUserByEmail(newEmail)).thenReturn(Optional.empty());
        when(userMapper.domainToEntity(domain)).thenReturn(entity);
        when(jwtService.generateJwtToken(entity)).thenReturn(expectedToken);

        //when
        String resultToken = authService.updateEmail(TEST_EMAIL, dto);

        //then
        assertEquals(expectedToken, resultToken);
        assertEquals(newEmail, domain.getEmail());
        verify(userRepository).save(entity);
    }

    @Test
    void updateEmail_alreadyExists_throwsUserUpdateException() {
        //given
        String takenEmail = "taken@example.com";
        UpdateEmailDTO dto = new UpdateEmailDTO(takenEmail);
        User entity = buildUserEntity(UUID.randomUUID(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);
        UserDomain domain = buildUserDomain(entity.getId(), TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, ENCODED_PASSWORD);

        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.of(entity));
        when(userMapper.entityToDomain(entity)).thenReturn(domain);
        when(userRepository.findUserByEmail(takenEmail)).thenReturn(Optional.of(new User()));

        //when & then
        UserUpdateException exception = assertThrows(
                UserUpdateException.class,
                () -> authService.updateEmail(TEST_EMAIL, dto)
        );
        assertEquals("Account with email [%s] already exists".formatted(takenEmail), exception.getMessage());
    }

    @Test
    void updateEmail_userNotFound_throwsUserNotFoundException() {
        //given
        UpdateEmailDTO dto = new UpdateEmailDTO("new@example.com");
        when(userRepository.findUserByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        //when & then
        assertThrows(
                UserNotFoundException.class,
                () -> authService.updateEmail(TEST_EMAIL, dto)
        );
    }

    // ==========================================
    // Helper factories
    // ==========================================

    private User buildUserEntity(UUID id, String email, String firstName, String lastName, String hashedPassword) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setHashedPassword(hashedPassword);
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private UserDomain buildUserDomain(UUID id, String email, String firstName, String lastName, String rawOrHashedPassword) {
        return UserDomain.restoreFromExisting(
                id,
                email,
                firstName,
                lastName,
                rawOrHashedPassword,
                Role.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
