package com.example.mediaid.api;

import com.example.mediaid.bl.UserService;
import com.example.mediaid.bl.emergency.RiskFactorService;
import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import com.example.mediaid.dal.User;
import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.dal.user_medical_history.UserDisease;
import com.example.mediaid.dal.user_medical_history.UserDiseaseRepository;
import com.example.mediaid.dal.user_medical_history.UserMedication;
import com.example.mediaid.dal.user_medical_history.UserMedicationRepository;
import com.example.mediaid.dto.LoginRequest;
import com.example.mediaid.dto.RiskFactorResponseDTO;
import com.example.mediaid.dto.RiskFactorUpdateDTO;
import com.example.mediaid.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

import static com.example.mediaid.constants.SecurityConstants.*;
import static com.example.mediaid.constants.ApiConstants.*;

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
    @Autowired
    private MedicationRepository medicationRepository;
    @Autowired
    private UserMedicationRepository userMedicationRepository;
    @Autowired
    private UserDiseaseRepository userDiseaseRepository;
    @Autowired
    private DiseaseRepository diseaseRepository;

    Logger logger = LoggerFactory.getLogger(UserService.class);

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

            return ResponseEntity.ok(profileData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving profile: " + e.getMessage()));
        }
    }

    @PostMapping("/logIn")
    public ResponseEntity<?> logIn(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("LogIn request received for email: " + loginRequest.getMail());
            UserService.Result result = userService.checkEntry(loginRequest.getMail(), loginRequest.getPassword());

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
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(createErrorResponse("User does not exist"));
                default:
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(createErrorResponse("Wrong password"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Server error: " + e.getMessage()));
        }
    }

    // POST /api/user/create-account
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
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(createErrorResponse("Email already exists"));
                case INVALID_PASSWORD:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(createErrorResponse("Invalid password length"));
                default:
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(createErrorResponse("An unexpected error occurred"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Server error: " + e.getMessage()));
        }
    }

    /**
     * עדכון גורמי סיכון - מסונכרן לכל המערכות
     */
    @PostMapping("/risk-factors")
    public ResponseEntity<?> updateRiskFactors(@RequestBody RiskFactorUpdateDTO dto, HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Updating comprehensive risk factors for user: {}", userId);

            // עדכון בPostgreSQL + Neo4j
            RiskFactorResponseDTO response = riskFactorService.updateUserRiskFactors(userId, dto);

            logger.info("Risk factors updated successfully for user: {}", userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating risk factors for user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating risk factors: " + e.getMessage()));
        }
    }

    // Placeholder endpoints for medications and diseases
    @PostMapping("/medications")
    public ResponseEntity<?> addUserMedications(@RequestBody Map<String, Object> medicationData, HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authentication token"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> medications = (List<Map<String, Object>>) medicationData.get("medications");

            if (medications == null || medications.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No medications found"));
            }

            List<UserMedication> savedMedications = new ArrayList<>();
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not found"));
            }

            for (Map<String, Object> med : medications) {
                String cui = (String) med.get("cui");
                String name = (String) med.get("name");

                Medication medication = medicationRepository.findByCui(cui);
                if (medication == null) {
                    medication = new Medication();
                    medication.setCui(cui);
                    medication.setName(name);
                    medication = medicationRepository.save(medication);
                }

                //צור קישור
                UserMedication userMedication = new UserMedication();
                userMedication.setUser(user);
                userMedication.setMedication(medication);
                userMedication.setDosage((String)med.get("dosage"));
                userMedication.setFrequency((String)med.get("frequency"));
                userMedication.setAdministrationRoute((String)med.get("administrationRoute"));
                userMedication.setIsActive((Boolean)med.getOrDefault("isActive", true));
                userMedication.setNotes((String)med.get("notes"));

                if(med.get("startDate") != null) {
                    userMedication.setStartDate(LocalDate.parse((String)med.get("startDate")));
                }
                if(med.get("endDate") != null) {
                    userMedication.setEndDate(LocalDate.parse((String)med.get("endDate")));
                }
                UserMedication saved = userMedicationRepository.save(userMedication);
                savedMedications.add(saved);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Medications added successfully",
                    "medications", savedMedications.stream().map(med->Map.of(
                            "id",med.getId(),
                            "name", med.getMedication().getName(),
                            "dosage", med.getDosage(),
                            "frequency", med.getFrequency()
                    )).toList()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error saving medications: " + e.getMessage()));
        }
    }

    @PostMapping("/diseases")
    public ResponseEntity<?> addUserDiseases(@RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diseasesData = (List<Map<String, Object>>) requestData.get("diseases");

            if (diseasesData == null || diseasesData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("No diseases provided"));
            }

            List<UserDisease> savedDiseases = new ArrayList<>();
            User user = userRepository.findById(userId).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("User not found"));
            }

            for (Map<String, Object> diseaseData : diseasesData) {
                String cui = (String) diseaseData.get("cui");
                String name = (String) diseaseData.get("name");

                // מצא או צור Disease
                Disease disease = diseaseRepository.findByCui(cui);
                if (disease == null) {
                    disease = new Disease();
                    disease.setCui(cui);
                    disease.setName(name);
                    disease = diseaseRepository.save(disease);
                }

                // צור UserDisease
                UserDisease userDisease = new UserDisease();
                userDisease.setUser(user);
                userDisease.setDisease(disease);
                userDisease.setStatus((String) diseaseData.getOrDefault("status", "active"));
                userDisease.setSeverity((String) diseaseData.get("severity"));
                userDisease.setNotes((String) diseaseData.get("notes"));

                // תאריכים
                if (diseaseData.get("diagnosisDate") != null) {
                    userDisease.setDiagnosisDate(LocalDate.parse((String) diseaseData.get("diagnosisDate")));
                }
                if (diseaseData.get("endDate") != null) {
                    userDisease.setEndDate(LocalDate.parse((String) diseaseData.get("endDate")));
                }

                UserDisease saved = userDiseaseRepository.save(userDisease);
                savedDiseases.add(saved);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Diseases saved successfully",
                    "count", savedDiseases.size(),
                    "diseases", savedDiseases.stream().map(disease -> Map.of(
                            "id", disease.getId(),
                            "name", disease.getDisease().getName(),
                            "status", disease.getStatus(),
                            "severity", disease.getSeverity()
                    )).toList()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error saving diseases: " + e.getMessage()));
        }
    }

    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(JWT_PREFIX_LENGTH);
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
}