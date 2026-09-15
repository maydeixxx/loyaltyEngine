package com.LoyaltyEngine.UserService.models;

import com.LoyaltyEngine.UserService.exceptions.UserUpdateException;
import com.LoyaltyEngine.UserService.exceptions.UserValidationException;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.domain.enums.Role;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDomainTests {

    @Test
    @DisplayName("Создание userDomain")
    void successfulCreateUserDomain() {
        //given
        String email = "test@gmail.com";
        String firstName = "John";
        String lastName = "Doe";
        String password = "hashed_password";

        //when
        UserDomain user = UserDomain.createUser(email, firstName, lastName, password);

        //then
        Assertions.assertNotNull(user.getId());
        Assertions.assertEquals(email, user.getEmail());
        Assertions.assertEquals(firstName, user.getFirstName());
        Assertions.assertEquals(lastName, user.getLastName());
        Assertions.assertEquals(password, user.getPasswordHash().value());
        Assertions.assertEquals(Role.USER, user.getRole());
        Assertions.assertNotNull(user.getCreatedAt());
        Assertions.assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Невалидный email при создании")
    void notValidEmailCreateUserDomain() {
        //when && then
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser(null, "John", "Doe", "pass"), "Email cant be null or empty");
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser("", "John", "Doe", "pass"), "Email cant be null or empty");
    }

    @Test
    @DisplayName("Невалидный firstName при создании")
    void notValidFirstNameCreateUserDomain() {
        //when && then
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser("test@gmail.com", null, "Doe", "pass"), "firstName cant be null or empty");
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser("test@gmail.com", "", "Doe", "pass"), "firstName cant be null or empty");
    }

    @Test
    @DisplayName("Невалидный lastName при создании")
    void notValidLastNameCreateUserDomain() {
        //when && then
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser("test@gmail.com", "John", null, "pass"), "lastName cant be null or empty");
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser("test@gmail.com", "John", "", "pass"), "lastName cant be null or empty");
    }

    @Test
    @DisplayName("Невалидный password при создании")
    void notValidPasswordCreateUserDomain() {
        //when && then
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser("test@gmail.com", "John", "Doe", null), "Password cant be null or blank");
        Assertions.assertThrows(UserValidationException.class, () -> UserDomain.createUser("test@gmail.com", "John", "Doe", "   "), "Password cant be null or blank");
    }

    @Test
    @DisplayName("Восстановление пользователя")
    void restoreFromExistingUserDomain() {
        //given
        UUID id = UuidCreator.getTimeOrderedEpoch();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        //when
        UserDomain user = UserDomain.restoreFromExisting(id, "test@gmail.com", "John", "Doe", "hash", Role.ADMIN, createdAt, updatedAt);

        //then
        Assertions.assertEquals(id, user.getId().value());
        Assertions.assertEquals("test@gmail.com", user.getEmail());
        Assertions.assertEquals("John", user.getFirstName());
        Assertions.assertEquals("Doe", user.getLastName());
        Assertions.assertEquals("hash", user.getPasswordHash().value());
        Assertions.assertEquals(Role.ADMIN, user.getRole());
        Assertions.assertEquals(createdAt, user.getCreatedAt());
        Assertions.assertEquals(updatedAt, user.getUpdatedAt());
    }

    @Test
    @DisplayName("Успешное обновление email")
    void successfulUpdateEmail() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when
        user.updateEmail("new@gmail.com");

        //then
        Assertions.assertEquals("new@gmail.com", user.getEmail());
    }

    @Test
    @DisplayName("Неуспешное обновление email (null)")
    void unsuccessfulUpdateEmailNull() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateEmail(null), "New email cant be null");
    }

    @Test
    @DisplayName("Неуспешное обновление на тот же email")
    void unsuccessfulUpdateEmailSame() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateEmail("test@gmail.com"), "You cant enter the same email");
    }

    @Test
    @DisplayName("Успешное обновление firstName")
    void successfulUpdateFirstName() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when
        user.updateFirstName("Jane");

        //then
        Assertions.assertEquals("Jane", user.getFirstName());
    }

    @Test
    @DisplayName("Неуспешное обновление firstName (null)")
    void unsuccessfulUpdateFirstNameNull() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateFirstName(null), "New first name cant be null");
    }

    @Test
    @DisplayName("Неуспешное обновление на тот же firstName")
    void unsuccessfulUpdateFirstNameSame() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateFirstName("John"), "You already have this first name");
    }

    @Test
    @DisplayName("Успешное обновление lastName")
    void successfulUpdateLastName() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when
        user.updateLastName("Smith");

        //then
        Assertions.assertEquals("Smith", user.getLastName());
    }

    @Test
    @DisplayName("Неуспешное обновление lastName (null)")
    void unsuccessfulUpdateLastNameNull() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateLastName(null), "New last name cant be null");
    }

    @Test
    @DisplayName("Неуспешное обновление на тот же lastName")
    void unsuccessfulUpdateLastNameSame() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateLastName("Doe"), "You already have this last name");
    }

    @Test
    @DisplayName("Успешное обновление пароля")
    void successfulUpdatePassword() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when
        user.updatePassword("new_pass");

        //then
        Assertions.assertEquals("new_pass", user.getPasswordHash().value());
    }

    @Test
    @DisplayName("Неуспешное обновление пароля (null или blank)")
    void unsuccessfulUpdatePasswordNullOrBlank() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserValidationException.class, () -> user.updatePassword(null), "Password cant be null or blank");
        Assertions.assertThrows(UserValidationException.class, () -> user.updatePassword("   "), "Password cant be null or blank");
    }

    @Test
    @DisplayName("Неуспешное обновление на тот же пароль")
    void unsuccessfulUpdatePasswordSame() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updatePassword("pass"), "You already have this password");
    }

    @Test
    @DisplayName("Успешное обновление роли")
    void successfulUpdateRole() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when
        user.updateRole(Role.ADMIN);

        //then
        Assertions.assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("Неуспешное обновление роли (null)")
    void unsuccessfulUpdateRoleNull() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateRole(null), "New role cant be null");
    }

    @Test
    @DisplayName("Неуспешное обновление на ту же роль")
    void unsuccessfulUpdateRoleSame() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> user.updateRole(Role.USER), "User is already [USER]");
    }

    @Test
    @DisplayName("Обновление таймстампа")
    void updateUpdatedAt() {
        //given
        UserDomain user = UserDomain.createUser("test@gmail.com", "John", "Doe", "pass");
        LocalDateTime newTime = LocalDateTime.now().plusHours(1);

        //when
        user.updateUpdatedAt(newTime);

        //then
        Assertions.assertEquals(newTime, user.getUpdatedAt());
    }

    @Test
    @DisplayName("Сравнение пользователей")
    void userEqualsAndHashCode() {
        //given
        UUID id = UuidCreator.getTimeOrderedEpoch();
        LocalDateTime now = LocalDateTime.now();

        UserDomain u1 = UserDomain.restoreFromExisting(id, "a@b.com", "A", "B", "pass", Role.USER, now, now);
        UserDomain u2 = UserDomain.restoreFromExisting(id, "diff@b.com", "Diff", "Diff", "diffpass", Role.ADMIN, now, now);
        UserDomain u3 = UserDomain.restoreFromExisting(UuidCreator.getTimeOrderedEpoch(), "a@b.com", "A", "B", "pass", Role.USER, now, now);

        //when && then
        Assertions.assertEquals(u1, u2);
        Assertions.assertEquals(u1.hashCode(), u2.hashCode());
        Assertions.assertNotEquals(u1, u3);
    }
}
