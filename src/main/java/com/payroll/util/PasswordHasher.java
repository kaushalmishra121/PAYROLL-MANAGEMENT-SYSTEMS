package com.payroll.util;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordHasher {
    private static final Logger logger = LoggerFactory.getLogger(PasswordHasher.class);
    private static final int LOG_ROUNDS = 12;

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean check(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            logger.warn("PasswordHasher.check called with null/empty argument: plainPassword={}, hashedPassword={}",
                    plainPassword == null ? "null" : "<provided>",
                    hashedPassword == null ? "null" : (hashedPassword.isEmpty() ? "empty" : hashedPassword.substring(0, Math.min(7, hashedPassword.length())) + "..."));
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            logger.warn("BCrypt.checkpw threw exception — hash in DB may be corrupted or from a different algorithm. " +
                    "Hash prefix='{}', Error='{}'",
                    hashedPassword.substring(0, Math.min(7, hashedPassword.length())),
                    e.getMessage());
            return false;
        }
    }
}
