package com.payroll.service;

import com.payroll.dao.AuditDao;
import com.payroll.dao.UserDao;
import com.payroll.exception.AuthenticationException;
import com.payroll.exception.ValidationException;
import com.payroll.model.AuditLog;
import com.payroll.model.User;
import com.payroll.util.PasswordHasher;
import com.payroll.util.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private UserDao mockUserDao;
    private AuditDao mockAuditDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        UserSession.clear();

        // In-memory test mock
        mockUserDao = new UserDao() {
            private final List<User> users = new ArrayList<>();
            {
                User admin = new User(1L, "admin", "admin@enterprise.com", PasswordHasher.hash("Admin@123"), "System Administrator", User.Role.ADMIN, User.UserStatus.ACTIVE);
                User inactive = new User(2L, "inactive_user", "inactive@enterprise.com", PasswordHasher.hash("Pass@123"), "Inactive User", User.Role.HR, User.UserStatus.INACTIVE);
                users.add(admin);
                users.add(inactive);
            }

            @Override
            public Optional<User> findById(Long id) {
                return users.stream().filter(u -> u.getId().equals(id)).findFirst();
            }

            @Override
            public Optional<User> findByUsername(String username) {
                return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
            }

            @Override
            public Optional<User> findByEmail(String email) {
                return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
            }

            @Override
            public List<User> findAll() {
                return users;
            }

            @Override
            public User save(User user) {
                user.setId((long) (users.size() + 1));
                users.add(user);
                return user;
            }

            @Override
            public boolean update(User user) {
                return true;
            }

            @Override
            public boolean updatePassword(Long userId, String newPasswordHash) {
                findById(userId).ifPresent(u -> u.setPasswordHash(newPasswordHash));
                return true;
            }

            @Override
            public boolean updateStatus(Long userId, User.UserStatus status) {
                findById(userId).ifPresent(u -> u.setStatus(status));
                return true;
            }

            @Override
            public int countActiveUsers() {
                return (int) users.stream().filter(u -> u.getStatus() == User.UserStatus.ACTIVE).count();
            }
        };

        mockAuditDao = new AuditDao() {
            @Override public void log(AuditLog log) {}
            @Override public List<AuditLog> findRecentLogs(int limit) { return List.of(); }
            @Override public List<AuditLog> findLogsByEntity(String entityType, Long entityId) { return List.of(); }
        };

        authService = new AuthService(mockUserDao, mockAuditDao);
    }

    @Test
    @DisplayName("Valid credentials should authenticate user and set active session")
    void testSuccessfulLogin() {
        User user = authService.login("admin", "Admin@123");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertTrue(UserSession.isLoggedIn());
        assertTrue(UserSession.isAdmin());
    }

    @Test
    @DisplayName("Invalid password should throw AuthenticationException")
    void testInvalidPassword() {
        assertThrows(AuthenticationException.class, () -> authService.login("admin", "WrongPassword"));
        assertFalse(UserSession.isLoggedIn());
    }

    @Test
    @DisplayName("Deactivated user account should be rejected")
    void testInactiveAccountLogin() {
        assertThrows(AuthenticationException.class, () -> authService.login("inactive_user", "Pass@123"));
    }

    @Test
    @DisplayName("Password hashing with BCrypt must verify correctly")
    void testPasswordHasher() {
        String plain = "SecretSecurePass123!";
        String hash = PasswordHasher.hash(plain);
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$"));
        assertTrue(PasswordHasher.check(plain, hash));
        assertFalse(PasswordHasher.check("WrongPass", hash));
    }
}
