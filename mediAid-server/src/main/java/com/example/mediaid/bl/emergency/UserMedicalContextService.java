package com.example.mediaid.bl.emergency;

import com.example.mediaid.dal.User;
import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.dal.user_medical_history.*;
import com.example.mediaid.dto.emergency.UserMedicalContext;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * שירות לטעינת המידע הרפואי מהמסד
 */
@Service
public class UserMedicalContextService {

    private static final Logger logger = LoggerFactory.getLogger(UserMedicalContextService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMedicationRepository userMedicationRepository;

    @Autowired
    private UserDiseaseRepository userDiseaseRepository;



    @Autowired
    private RiskFactorService riskFactorService;

    /**
     * טעינת המידע הרפואי המלא של המשתמש
     */
    public UserMedicalContext getUserMedicalContext(UUID userId) {
        logger.info("Loading comprehensive medical context for user {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            UserMedicalContext context = new UserMedicalContext();

            // טעינת תרופות פעילות
            context.setCurrentMedications(loadActiveMedications(userId));
            logger.debug("Loaded {} active medications", context.getCurrentMedications().size());

            // טעינת מחלות פעילות
            context.setActiveDiseases(loadActiveDiseases(userId));
            logger.debug("Loaded {} active diseases", context.getActiveDiseases().size());

            // טעינת גורמי סיכון מקיפים (מהUser entity )
            context.setRiskFactors(loadComprehensiveRiskFactors(userId));
            logger.debug("Loaded {} risk factors", context.getRiskFactors().size());

            // טעינת אלרגיות (כרגע ריק)
            context.setAllergies(new ArrayList<>());

            // מידע בסיסי
            context.setBasicInfo(buildBasicInfo(user));

            // חישוב ציון סיכון כולל
            double overallRiskScore = user.calculateOverallRiskScore();
            context.setOverallRiskScore(overallRiskScore);
            context.setRiskLevel(calculateRiskLevel(overallRiskScore));

            logger.info("Medical context loaded successfully for user {} - {} medications, {} diseases, {} risk factors",
                    userId, context.getCurrentMedications().size(),
                    context.getActiveDiseases().size(), context.getRiskFactors().size());

            return context;

        } catch (Exception e) {
            logger.error("Error loading medical context for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to load medical context", e);
        }
    }

    /**
     * טעינת גורמי סיכון מקיפים
     */
    private List<UserMedicalEntity> loadComprehensiveRiskFactors(UUID userId) {
        List<UserMedicalEntity> allRiskFactors = new ArrayList<>();

        try {
            // גורמי סיכון מהUser entity (הממוזגים מהשאלון)
            List<UserMedicalEntity> userProfileRiskFactors = riskFactorService.getUserActiveRiskFactors(userId);
            allRiskFactors.addAll(userProfileRiskFactors);
            logger.debug("Loaded {} risk factors from user profile", userProfileRiskFactors.size());

        } catch (Exception e) {
            logger.error("Error loading comprehensive risk factors for user {}: {}", userId, e.getMessage());
        }

        return allRiskFactors;
    }


    /**
     * טעינת תרופות פעילות
     */
    private List<UserMedicalEntity> loadActiveMedications(UUID userId) {
        List<UserMedicalEntity> medications = new ArrayList<>();

        try {
            List<UserMedication> userMedications = userMedicationRepository.findActiveMedicationsForUser(userId);
            for (UserMedication userMed : userMedications) {
                UserMedicalEntity entity = new UserMedicalEntity();
                entity.setCui(userMed.getMedication().getCui());
                entity.setName(userMed.getMedication().getName());
                entity.setType("medication");
                entity.setStatus(userMed.getIsActive() ? "active" : "inactive");
                entity.setStartDate(userMed.getStartDate());
                entity.setEndDate(userMed.getEndDate());
                entity.setDosage(userMed.getDosage());
                entity.setFrequency(userMed.getFrequency());

                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("administrationRoute", userMed.getAdministrationRoute());
                additionalData.put("notes", userMed.getNotes());
                entity.setAdditionalData(additionalData);

                medications.add(entity);
            }
        } catch (Exception e) {
            logger.error("Error loading medications for user {}: {}", userId, e.getMessage());
        }

        return medications;
    }

    /**
     * טעינת מחלות פעילות
     */
    private List<UserMedicalEntity> loadActiveDiseases(UUID userId) {
        List<UserMedicalEntity> diseases = new ArrayList<>();

        try {
            List<UserDisease> userDiseases = userDiseaseRepository.findActiveDiseasesForUser(userId);

            for (UserDisease userDisease : userDiseases) {
                UserMedicalEntity entity = new UserMedicalEntity();
                entity.setCui(userDisease.getDisease().getCui());
                entity.setName(userDisease.getDisease().getName());
                entity.setType("disease");
                entity.setStatus(userDisease.getStatus());
                entity.setStartDate(userDisease.getDiagnosisDate());
                entity.setEndDate(userDisease.getEndDate());
                entity.setSeverity(userDisease.getSeverity());

                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("notes", userDisease.getNotes());
                entity.setAdditionalData(additionalData);

                diseases.add(entity);
            }

        } catch (Exception e) {
            logger.error("Error loading diseases for user {}: {}", userId, e.getMessage());
        }

        return diseases;
    }

    /**
     * בניית מידע בסיסי על המשתמש
     */
    private Map<String, Object> buildBasicInfo(User user) {
        Map<String, Object> basicInfo = new HashMap<>();

        try {
            basicInfo.put("username", user.getUsername());
            basicInfo.put("email", user.getEmail());
            basicInfo.put("gender", user.getGender());

            // חישוב גיל
            if (user.getDateOfBirth() != null) {
                int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
                basicInfo.put("age", age);
                basicInfo.put("dateOfBirth", user.getDateOfBirth());
            }

            if (user.getHeight() != null) {
                basicInfo.put("height", user.getHeight());
            }

            if (user.getWeight() != null) {
                basicInfo.put("weight", user.getWeight());
            }

            // BMI
            Double bmi = user.calculateBMI();
            if (bmi != null) {
                basicInfo.put("bmi", Math.round(bmi * 100.0) / 100.0);
                basicInfo.put("bmiCategory", getBmiCategory(bmi));
            }

            // גורמי סיכון בסיסיים מהפרופיל
            addRiskFactorInfoToBasicInfo(basicInfo, user);

        } catch (Exception e) {
            logger.error("Error building basic info for user: {}", e.getMessage());
        }

        return basicInfo;
    }

    /**
     * הוספת מידע גורמי סיכון למידע הבסיסי
     */
    private void addRiskFactorInfoToBasicInfo(Map<String, Object> basicInfo, User user) {
        if (user.getSmokingStatus() != null) {
            basicInfo.put("smokingStatus", user.getSmokingStatus().getDescription());
            basicInfo.put("smokingRiskWeight", user.getSmokingStatus().getWeight());
        }

        if (user.getAlcoholConsumption() != null) {
            basicInfo.put("alcoholConsumption", user.getAlcoholConsumption().getDescription());
            basicInfo.put("alcoholRiskWeight", user.getAlcoholConsumption().getWeight());
        }

        if (user.getPhysicalActivity() != null) {
            basicInfo.put("physicalActivity", user.getPhysicalActivity().getDescription());
            basicInfo.put("physicalActivityRiskWeight", user.getPhysicalActivity().getWeight());
        }

        if (user.getBloodPressure() != null) {
            basicInfo.put("bloodPressure", user.getBloodPressure().getDescription());
            basicInfo.put("bloodPressureRiskWeight", user.getBloodPressure().getWeight());
        }

        if (user.getStressLevel() != null) {
            basicInfo.put("stressLevel", user.getStressLevel().getDescription());
            basicInfo.put("stressRiskWeight", user.getStressLevel().getWeight());
        }

        if (user.getAgeGroup() != null) {
            basicInfo.put("ageGroup", user.getAgeGroup().getDescription());
            basicInfo.put("ageGroupRiskWeight", user.getAgeGroup().getWeight());
        }

        // הסטוריה משפחתית
        if (user.getFamilyHeartDisease() != null) {
            basicInfo.put("familyHeartDisease", user.getFamilyHeartDisease().getDescription());
            basicInfo.put("familyHeartDiseaseRiskWeight", user.getFamilyHeartDisease().getWeight());
        }

        if (user.getFamilyCancer() != null) {
            basicInfo.put("familyCancer", user.getFamilyCancer().getDescription());
            basicInfo.put("familyCancerRiskWeight", user.getFamilyCancer().getWeight());
        }
    }

    //פונקציות עזר
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