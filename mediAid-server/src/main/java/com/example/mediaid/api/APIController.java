package com.example.mediaid.api;

import com.example.mediaid.bl.UserService;
import com.example.mediaid.bl.extract_data_from_EHR.Text_from_image;
import com.example.mediaid.dal.User;
import com.example.mediaid.dto.DiagnosisData;
import com.example.mediaid.dto.LoginRequest;
import com.example.mediaid.dto.SignupRequest;
import com.example.mediaid.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.mediaid.bl.UserService.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RestController
public class APIController {
    private final UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

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

            User user = new User();
            user.setEmail(signupRequest.getEmail());
            user.setPasswordHash(signupRequest.getPassword());
            user.setUsername(signupRequest.getUsername());
            user.setGender(signupRequest.getGender());

            String dateOfBirth = signupRequest.getDateOfBirth();
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                try {
                    user.setDateOfBirth(LocalDate.parse(dateOfBirth));
                } catch (Exception e) {
                    return ResponseEntity.status(400).body(createErrorResponse("Invalid date format for dateOfBirth"));
                }
            }

            if (signupRequest.getHeight() != null) {
                user.setHeight(Float.valueOf(signupRequest.getHeight()));
            }

            if (signupRequest.getWeight() != null) {
                user.setWeight(Float.valueOf(signupRequest.getWeight()));
            }

            Result result = userService.createUser(user);

            switch (result) {
                case SUCCESS:
                    // Get the saved user to generate JWT
                    User savedUser = userService.findByEmail(signupRequest.getEmail());
                    String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getEmail(), savedUser.getUserId());

                    Map<String, Object> response = new HashMap<>();
                    response.put("token", token);
                    response.put("username", savedUser.getUsername());
                    response.put("email", savedUser.getEmail());
                    response.put("message", "User created successfully");

                    return ResponseEntity.ok(response);
                case EMAIL_ALREADY_EXISTS:
                    return ResponseEntity.status(409).body(createErrorResponse("Email already exists"));
                case INVALID_PASSWORD:
                    return ResponseEntity.status(400).body(createErrorResponse("Invalid password length"));
                case ERROR:
                default:
                    return ResponseEntity.status(500).body(createErrorResponse("An unexpected error occurred"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(createErrorResponse("Server error: " + e.getMessage()));
        }
    }

    @PostMapping("/logIn")
    public ResponseEntity<?> logIn(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("LogIn request received for email: " + loginRequest.getMail());
            Result result = userService.checkEntry(loginRequest.getMail(), loginRequest.getPassword());

            switch (result) {
                case SUCCESS:
                    User user = userService.findByEmail(loginRequest.getMail());
                    String token = jwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getUserId());

                    Map<String, Object> response = new HashMap<>();
                    response.put("token", token);
                    response.put("username", user.getUsername());
                    response.put("email", user.getEmail());
                    response.put("message", "User logged in successfully");

                    return ResponseEntity.ok(response);
                case NOT_EXISTS:
                    return ResponseEntity.status(404).body(createErrorResponse("User does not exist"));
                default:
                    return ResponseEntity.status(401).body(createErrorResponse("Wrong password"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(createErrorResponse("Server error: " + e.getMessage()));
        }
    }

    @PostMapping("/validateToken")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtUtil.isValid(token)) {
                    String username = jwtUtil.extractUsername(token);
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("username", username);
                    return ResponseEntity.ok(response);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.status(401).body(response);
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "AUTHENTICATION_ERROR");
        error.put("message", message);
        return error;
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
