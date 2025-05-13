package com.example.mediaid.api;

import com.example.mediaid.bl.UserService;
import com.example.mediaid.bl.extract_data_from_EHR.Text_from_image;
import com.example.mediaid.dal.Users;
import com.example.mediaid.dto.DiagnosisData;
import com.example.mediaid.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.mediaid.bl.UserService.Result;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
public class APIController {
    private UserService userService;
    @Autowired
    private Text_from_image textFromImage;

    public APIController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signIn")
    public ResponseEntity<?> signIn(@RequestBody Users user) {
        System.out.println("SignIn request received for email: " + user.getEmail());
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

    @PostMapping("/logIn")
    public ResponseEntity<?> logIn(@RequestBody LoginRequest user) {
        System.out.println("LogIn request received for email: " + user.getMail());
        Result result = userService.check_entry(user.getMail(), user.getPassword());
        return switch (result) {
            case SUCCESS -> ResponseEntity.ok("User logged in successfully");
            case NOT_EXISTS -> ResponseEntity.status(409).body("user not exists");
            default -> ResponseEntity.status(400).body("Wrong password");
        };
    }

    @PostMapping("/uploadData")
    public ResponseEntity<?> uploadData(@ModelAttribute DiagnosisData diagnosisData) {
        try {
            StringBuilder responseMessage = new StringBuilder("Data uploaded successfully.\n");

            if (diagnosisData.getText() != null) {
                responseMessage.append("Text: ").append(diagnosisData.getText()).append("\n");
            }
            if (diagnosisData.getImage() != null) {
                responseMessage.append("Image received: ").append(diagnosisData.getImage().getOriginalFilename()).append("\n");

                // קריאה לעיבוד OCR
                String ocrResult = textFromImage.processOCR(diagnosisData.getImage());
                System.out.println("OCR Result: " + ocrResult);
                responseMessage.append("OCR Result: ").append(ocrResult).append("\n");
            }
            if (diagnosisData.getAudio() != null) {
                responseMessage.append("Audio received: ").append(diagnosisData.getAudio().getOriginalFilename()).append("\n");
            }

            return new ResponseEntity<>(responseMessage.toString(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error processing upload: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}