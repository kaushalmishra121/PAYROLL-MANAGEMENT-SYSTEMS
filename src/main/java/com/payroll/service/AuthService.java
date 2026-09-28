package com.payroll.service;

import com.payroll.dao.AuditDao;
import com.payroll.dao.AuditDaoImpl;
import com.payroll.dao.UserDao;
import com.payroll.dao.UserDaoImpl;
import com.payroll.exception.AuthenticationException;
import com.payroll.exception.DuplicateRecordException;
import com.payroll.exception.ValidationException;
import com.payroll.model.AuditLog;
import com.payroll.model.User;
import com.payroll.util.PasswordHasher;
import com.payroll.util.UserSession;
import com.payroll.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final AuditDao auditDao;

    public AuthService() {
        this.userDao = new UserDaoImpl();
        this.auditDao = new AuditDaoImpl();
    }

    public AuthService(UserDao userDao, AuditDao auditDao) {
        this.userDao = userDao;
        this.auditDao = auditDao;
    }

    public User login(String identifier, String password) {
        ValidationUtils.requireNonBlank(identifier, "Username/Email");
        ValidationUtils.requireNonBlank(password, "Password");

        String cleanIdentifier = identifier.trim();
        Optional<User> userOpt = userDao.findByUsername(cleanIdentifier);
        if (userOpt.isEmpty()) {
            userOpt = userDao.findByEmail(cleanIdentifier);
        }

        if (userOpt.isEmpty()) {
            logger.warn("Failed login attempt for identifier: {}", cleanIdentifier);
            throw new AuthenticationException("Invalid username/email or password.");
        }

        User user = userOpt.get();
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            logger.warn("Login attempt for inactive user account: {}", user.getUsername());
            throw new AuthenticationException("Account is deactivated. Please contact an administrator.");
        }

        if (!PasswordHasher.check(password, user.getPasswordHash())) {
            logger.warn("Password mismatch for user: {}", user.getUsername());
            throw new AuthenticationException("Invalid username/email or password.");
        }

        UserSession.setCurrentUser(user);
        auditDao.log(new AuditLog(user.getId(), user.getUsername(), "LOGIN", "USER", user.getId(), "User logged in successfully", "127.0.0.1"));
        logger.info("User {} logged in successfully with role {}", user.getUsername(), user.getRole());
        return user;
    }

    public void logout() {
        User current = UserSession.getCurrentUser();
        if (current != null) {
            auditDao.log(new AuditLog(current.getId(), current.getUsername(), "LOGOUT", "USER", current.getId(), "User logged out", "127.0.0.1"));
            logger.info("User {} logged out", current.getUsername());
        }
        UserSession.clear();
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserSession.requireLoggedIn();
        User currentUser = UserSession.getCurrentUser();
        
        // Only allow changing self password or admin changing any
        if (!currentUser.getId().equals(userId) && !currentUser.isAdmin()) {
            throw new ValidationException("You do not have permission to change this user's password.");
        }

        ValidationUtils.requireNonBlank(newPassword, "New Password");
        if (newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long.");
        }

        User targetUser = userDao.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        if (!currentUser.isAdmin() || currentUser.getId().equals(userId)) {
            if (!PasswordHasher.check(currentPassword, targetUser.getPasswordHash())) {
                throw new ValidationException("Current password does not match.");
            }
        }

        String newHash = PasswordHasher.hash(newPassword);
        userDao.updatePassword(userId, newHash);
        auditDao.log(new AuditLog(currentUser.getId(), currentUser.getUsername(), "CHANGE_PASSWORD", "USER", userId, "Password updated successfully", "127.0.0.1"));
        logger.info("Password changed for user id: {}", userId);
    }

    public User createUser(User user, String plainPassword) {
        UserSession.requireAdmin();
        ValidationUtils.requireNonBlank(user.getUsername(), "Username");
        ValidationUtils.validateEmail(user.getEmail());
        ValidationUtils.requireNonBlank(user.getFullName(), "Full Name");
        ValidationUtils.requireNonBlank(plainPassword, "Password");

        if (plainPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }

        if (userDao.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateRecordException("Username '" + user.getUsername() + "' is already registered.");
        }

        if (userDao.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateRecordException("Email '" + user.getEmail() + "' is already registered.");
        }

        user.setPasswordHash(PasswordHasher.hash(plainPassword));
        User saved = userDao.save(user);
        
        User current = UserSession.getCurrentUser();
        auditDao.log(new AuditLog(current.getId(), current.getUsername(), "CREATE_USER", "USER", saved.getId(), "Created user account: " + saved.getUsername() + " (" + saved.getRole() + ")", "127.0.0.1"));
        return saved;
    }

    public void updateUserStatus(Long userId, User.UserStatus status) {
        UserSession.requireAdmin();
        User currentUser = UserSession.getCurrentUser();
        if (currentUser.getId().equals(userId) && status == User.UserStatus.INACTIVE) {
            throw new ValidationException("You cannot deactivate your own administrative account.");
        }

        userDao.updateStatus(userId, status);
        auditDao.log(new AuditLog(currentUser.getId(), currentUser.getUsername(), "UPDATE_USER_STATUS", "USER", userId, "Set status to: " + status, "127.0.0.1"));
    }

    public List<User> getAllUsers() {
        UserSession.requireAdmin();
        return userDao.findAll();
    }
}
