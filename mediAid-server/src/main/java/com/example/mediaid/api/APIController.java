package com.example.mediaid.api;

import com.example.mediaid.bl.UserService;
import com.example.mediaid.dal.UserEntity;
import org.springframework.http.ResponseEntity;
import com.example.mediaid.bl.UserService.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class APIController {
    private UserService userService;

    @PostMapping("/signIn")
    public ResponseEntity<?> signIn(@RequestBody UserEntity user) {
        Result result = userService.createUser(user);
        switch (result) {
            case SUCCESS:
                return ResponseEntity.ok("User created successfully");
            case EMAIL_ALREADY_EXISTS:
                return ResponseEntity.status(409).body("Email already exists");
            case INVALID_PASSWORD:
                return ResponseEntity.status(400).body("Invalid password length");
            case ERROR:
            default:
                return ResponseEntity.status(500).body("An unexpected error occurred");
        }
    }
}
