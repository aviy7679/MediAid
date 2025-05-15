package com.example.mediaid.api;

import com.example.mediaid.bl.UserService;
import com.example.mediaid.bl.extract_data_from_EHR.Text_from_image;
import com.example.mediaid.dal.User;
import com.example.mediaid.dto.DiagnosisData;
import com.example.mediaid.dto.LoginRequest;
import com.example.mediaid.dto.SignupRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.mediaid.bl.UserService.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RestController
public class APIController {
    private final UserService userService;

    @Autowired
    private Text_from_image textFromImage;

    @Autowired
    public APIController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signUp")
    public ResponseEntity<?> signUp(@RequestBody SignupRequest signupRequest) {
        try {
            System.out.println("SignUp request received for email: " + signupRequest.getEmail());

            // Map the DTO to User entity
            User user = new User();
            user.setEmail(signupRequest.getEmail());
            // Note: we're setting the plain text password which will be hashed in the service
            user.setPasswordHash(signupRequest.getPassword());
            user.setUsername(signupRequest.getUsername());
            user.setGender(signupRequest.getGender());

            // Handle date of birth if it's missing or in incorrect format
            String dateOfBirth = signupRequest.getDateOfBirth();
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                try {
                    user.setDateOfBirth(LocalDate.parse(dateOfBirth));
                } catch (Exception e) {
                    return ResponseEntity.status(400).body("Invalid date format for dateOfBirth");
                }
            }

            // Handle optional fields
            if (signupRequest.getHeight() != null) {
                user.setHeight(Float.valueOf(signupRequest.getHeight()));
            }

            if (signupRequest.getWeight() != null) {
                user.setWeight(Float.valueOf(signupRequest.getWeight()));
            }

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
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    @PostMapping("/logIn")
    public ResponseEntity<?> logIn(@RequestBody LoginRequest user) {
        try {
            System.out.println("LogIn request received for email: " + user.getMail());
            Result result = userService.checkEntry(user.getMail(), user.getPassword());
            return switch (result) {
                case SUCCESS -> ResponseEntity.ok("User logged in successfully");
                case NOT_EXISTS -> ResponseEntity.status(404).body("User does not exist");
                default -> ResponseEntity.status(401).body("Wrong password");
            };
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
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

                // Call OCR processing
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