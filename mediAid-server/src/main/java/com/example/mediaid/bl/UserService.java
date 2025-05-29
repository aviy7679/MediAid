package com.example.mediaid.bl;

import com.example.mediaid.dal.User;
import com.example.mediaid.dal.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        System.out.println("Finding user by email: " + email);
        return userRepository.findByEmail(email);
    }

    // Use REQUIRES_NEW to create a new transaction, so if one operation fails,
    // it doesn't affect others
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result createUser(User user) {
        // Check if user exists in a separate transaction
        if (emailExists(user.getEmail())) {
            return Result.EMAIL_ALREADY_EXISTS;
        }

        try {
            // Hash the password before saving the user
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
            userRepository.save(user);
            return Result.SUCCESS;
        } catch (DataIntegrityViolationException e) {
            Throwable cause = e.getRootCause();
            if (cause != null && cause.getMessage().contains("EMAIL")) {
                return Result.EMAIL_ALREADY_EXISTS;
            } else if (cause != null && cause.getMessage().contains("password")) {
                return Result.INVALID_PASSWORD;
            }
            return Result.ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return Result.ERROR;
        }
    }

    // Separate method with its own transaction
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }

    @Transactional(readOnly = true)
    public Result checkEntry(String email, String password) {
        System.out.println("Checking entry for email: " + email);
        User user = findByEmail(email);
        if (user == null) {
            System.out.println("User not found");
            return Result.NOT_EXISTS;
        }
        // Check password hash match
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            System.out.println("Password matches");
            return Result.SUCCESS;
        }
        System.out.println("Wrong password");
        return Result.WRONG_PASSWORD;
    }

    @Transactional
    public Result changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password is correct
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return Result.WRONG_PASSWORD;
        }

        // Hash the new password and save
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return Result.SUCCESS;
    }

    public enum Result {
        SUCCESS, NOT_EXISTS, WRONG_PASSWORD, EMAIL_ALREADY_EXISTS, INVALID_PASSWORD, ERROR
    }
}