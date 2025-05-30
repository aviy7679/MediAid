// UserProfileController.java - קובץ חדש שצריך ליצור
package com.example.mediaid.api;

import com.example.mediaid.bl.UserService;
import com.example.mediaid.bl.RiskFactorService;
import com.example.mediaid.dal.User;
import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private RiskFactorService riskFactorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // GET /api/user/profile
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("User not found"));
            }

            // בנה את תגובת הפרופיל
            Map<String, Object> profileData = new HashMap<>();

            // Basic Info
            Map<String, Object> basicInfo = new HashMap<>();
            basicInfo.put("username", user.getUsername());
            basicInfo.put("email", user.getEmail());
            basicInfo.put("dateOfBirth", user.getDateOfBirth());
            basicInfo.put("gender", user.getGender());
            basicInfo.put("height", user.getHeight());
            basicInfo.put("weight", user.getWeight());

            // חישוב BMI
            if (user.getHeight() != null && user.getWeight() != null && user.getHeight() > 0 && user.getWeight() > 0) {
                double heightInMeters = user.getHeight() / 100.0;
                double bmi = user.getWeight() / (heightInMeters * heightInMeters);
                basicInfo.put("bmi", Math.round(bmi * 100.0) / 100.0);

                // קטגוריית BMI
                String bmiCategory = getBmiCategory(bmi);
                basicInfo.put("bmiCategory", bmiCategory);
            }

            profileData.put("basicInfo", basicInfo);

            // Risk Factors
            Map<String, Object> riskFactors = new HashMap<>();
            riskFactors.put("smokingStatus", user.getSmokingStatus());
            riskFactors.put("alcoholConsumption", user.getAlcoholConsumption());
            riskFactors.put("physicalActivity", user.getPhysicalActivity());
            riskFactors.put("bloodPressure", user.getBloodPressure());
            riskFactors.put("stressLevel", user.getStressLevel());
            riskFactors.put("ageGroup", user.getAgeGroup());
            riskFactors.put("familyHeartDisease", user.getFamilyHeartDisease());
            riskFactors.put("familyCancer", user.getFamilyCancer());

            // חישוב ציון סיכון כולל
            double overallRiskScore = user.calculateOverallRiskScore();
            riskFactors.put("overallRiskScore", overallRiskScore);
            riskFactors.put("riskLevel", calculateRiskLevel(overallRiskScore));

            profileData.put("riskFactors", riskFactors);

            // Stats - נתונים בסיסיים (כרגע ריקים, אפשר להוסיף בהמשך)
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeMedications", 0); // TODO: חישוב אמיתי
            stats.put("activeDiseases", 0); // TODO: חישוב אמיתי
            stats.put("profileCompleteness", calculateProfileCompleteness(user));
            profileData.put("stats", stats);

            // Medications & Diseases - ריקים כרגע
            profileData.put("medications", new ArrayList<>());
            profileData.put("diseases", new ArrayList<>());

            return ResponseEntity.ok(profileData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving profile: " + e.getMessage()));
        }
    }

    // POST /api/user/create-account (חלופה ל-signUp)
    @PostMapping("/create-account")
    public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> signupData) {
        try {
            // יצירת אובייקט User
            User user = new User();
            user.setEmail((String) signupData.get("email"));
            user.setPasswordHash((String) signupData.get("password"));
            user.setUsername((String) signupData.get("username"));
            user.setGender((String) signupData.get("gender"));

            if (signupData.get("dateOfBirth") != null) {
                user.setDateOfBirth(java.time.LocalDate.parse((String) signupData.get("dateOfBirth")));
            }

            if (signupData.get("height") != null) {
                user.setHeight(Float.valueOf(signupData.get("height").toString()));
            }

            if (signupData.get("weight") != null) {
                user.setWeight(Float.valueOf(signupData.get("weight").toString()));
            }

            UserService.Result result = userService.createUser(user);

            switch (result) {
                case SUCCESS:
                    User savedUser = userService.findByEmail(user.getEmail());
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
                default:
                    return ResponseEntity.status(500).body(createErrorResponse("An unexpected error occurred"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("Server error: " + e.getMessage()));
        }
    }

    // Placeholder endpoints for medications and diseases
    @PostMapping("/medications")
    public ResponseEntity<?> addUserMedications(@RequestBody Map<String, Object> medicationData, HttpServletRequest request) {
        // TODO: Implement medication management
        return ResponseEntity.ok(Map.of("message", "Medications endpoint - not implemented yet"));
    }

    @PostMapping("/diseases")
    public ResponseEntity<?> addUserDiseases(@RequestBody Map<String, Object> diseaseData, HttpServletRequest request) {
        // TODO: Implement disease management
        return ResponseEntity.ok(Map.of("message", "Diseases endpoint - not implemented yet"));
    }

    // Helper methods
    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.isValid(token)) {
                    return jwtUtil.extractUserId(token);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "USER_PROFILE_ERROR");
        error.put("message", message);
        return error;
    }

    private String getBmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        if (bmi < 35) return "Obese class 1";
        if (bmi < 40) return "Obese class 2";
        return "Obese class 3";
    }

    private String calculateRiskLevel(double riskScore) {
        if (riskScore < 0.2) return "Low Risk";
        if (riskScore < 0.5) return "Moderate Risk";
        if (riskScore < 0.7) return "High Risk";
        return "Very High Risk";
    }

    private double calculateProfileCompleteness(User user) {
        int totalFields = 10;
        int completedFields = 0;

        if (user.getUsername() != null) completedFields++;
        if (user.getEmail() != null) completedFields++;
        if (user.getDateOfBirth() != null) completedFields++;
        if (user.getGender() != null) completedFields++;
        if (user.getHeight() != null) completedFields++;
        if (user.getWeight() != null) completedFields++;
        if (user.getSmokingStatus() != null) completedFields++;
        if (user.getAlcoholConsumption() != null) completedFields++;
        if (user.getPhysicalActivity() != null) completedFields++;
        if (user.getBloodPressure() != null) completedFields++;

        return (double) completedFields / totalFields * 100;
    }
}