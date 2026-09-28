package com.payroll.dao;

import com.payroll.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    User save(User user);
    boolean update(User user);
    boolean updatePassword(Long userId, String newPasswordHash);
    boolean updateStatus(Long userId, User.UserStatus status);
    int countActiveUsers();
}
