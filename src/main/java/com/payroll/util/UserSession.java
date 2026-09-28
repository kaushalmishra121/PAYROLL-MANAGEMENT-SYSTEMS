package com.payroll.util;

import com.payroll.exception.UnauthorizedException;
import com.payroll.model.User;

public class UserSession {
    private static User currentUser;

    private UserSession() {
    }

    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized void clear() {
        currentUser = null;
    }

    public static synchronized boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static synchronized boolean isHr() {
        return currentUser != null && (currentUser.getRole() == User.Role.HR || currentUser.isAdmin());
    }

    public static synchronized void requireLoggedIn() {
        if (!isLoggedIn()) {
            throw new UnauthorizedException("User is not authenticated. Please log in.");
        }
    }

    public static synchronized void requireAdmin() {
        requireLoggedIn();
        if (!isAdmin()) {
            throw new UnauthorizedException("Action forbidden: Administrative privileges required.");
        }
    }

    public static synchronized void requireHrOrAdmin() {
        requireLoggedIn();
        if (!isHr()) {
            throw new UnauthorizedException("Action forbidden: HR or Admin privileges required.");
        }
    }
}
