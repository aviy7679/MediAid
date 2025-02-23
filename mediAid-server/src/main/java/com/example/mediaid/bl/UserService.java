package com.example.mediaid.bl;

import com.example.mediaid.dal.UserEntity;
import com.example.mediaid.dal.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

//public enum Result{
//    SUCCESS,NOT_EXISTS, WRONG_PASSWORD,EMAIL_ALREADY_EXISTS,INVALID_PASSWORD,ERROR
//}

@Service
public class UserService {
 private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    public Result check_entry(String email, String password) {
        UserEntity user = userRepository.findByEmail(email);
        if (user == null) {
            return Result.NOT_EXISTS;
        }
        if(user.getPassword().equals(password)) {
            return Result.SUCCESS;
        }
        return Result.WRONG_PASSWORD;
    }
    public Result createUser(UserEntity user) {
        UserEntity userEntity = userRepository.findByEmail(user.getEmail());
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return Result.EMAIL_ALREADY_EXISTS;
        }
        try {
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
            return Result.ERROR;
        }
    }

    public static enum Result {
        SUCCESS, NOT_EXISTS, WRONG_PASSWORD, EMAIL_ALREADY_EXISTS, INVALID_PASSWORD, ERROR
    }
}
