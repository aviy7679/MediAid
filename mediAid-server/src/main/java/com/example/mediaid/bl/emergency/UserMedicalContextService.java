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

//טעינת המידע הרפואי מהמסד
@Service
public class UserMedicalContextService {

    private static final Logger logger = LoggerFactory.getLogger(UserMedicalContextService.class);

    private UserRepository userRepository;

    @Autowired
    private UserMedicationRepository userMedicationRepository;

    @Autowired
    private UserDiseaseRepository userDiseaseRepository;

    @Autowired
    private UserSymptomRepository userSymptomRepository;

    @Autowired
    private UserRiskFactorRepository userRiskFactorRepository;

    //טעינת המידע הרפואי המלא של המשתמש
    public UserMedicalContext getUserMedicalContext(UUID userId) {
        logger.info("Loading medical context for user {}", userId);

        try{
            User user = userRepository.findById(userId).orElseThrow(()->new RuntimeException("User not found: " + userId));

            UserMedicalContext context = new UserMedicalContext();

            // טעינת תרופות פעילות
            context.setCurrentMedications(loadActiveMedications(userId));
            logger.debug("Loaded {} active medications", context.getCurrentMedications().size());

            // טעינת מחלות פעילות
            context.setActiveDiseases(loadActiveDiseases(userId));
            logger.debug("Loaded {} active diseases", context.getActiveDiseases().size());

            // טעינת גורמי סיכון
            context.setRiskFactors(loadActiveRiskFactors(userId));
            logger.debug("Loaded {} risk factors", context.getRiskFactors().size());

            // טעינת אלרגיות (כרגע ריק)
            context.setAllergies(new ArrayList<>());


            return context;
        } catch (Exception e) {
            logger.error("Error loading medical context for user {}: {}", userId,e.getMessage(), e);
            throw new RuntimeException("Failed to load medical context", e);
        }
    }

    public UserMedicalContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    //טעינת תרופות פעילות
    private List<UserMedicalEntity> loadActiveMedications(UUID userId) {
        List<UserMedicalEntity> medications = new ArrayList<>();

        try{
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
                // מידע נוסף
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("administrationRoute", userMed.getAdministrationRoute());
                additionalData.put("notes", userMed.getNotes());
                entity.setAdditionalData(additionalData);

                medications.add(entity);
            }
        } catch (Exception e) {
            logger.error("Error loading medical context for user {}: {}", userId,e.getMessage(), e);
        }
        return medications;
    }

    //טעינת מחלות פעילות
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

                // מידע נוסף
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

    // טעינת גורמי סיכון פעילים
    private List<UserMedicalEntity> loadActiveRiskFactors(UUID userId) {
        List<UserMedicalEntity> riskFactors = new ArrayList<>();

        try {
            List<UserRiskFactor> userRiskFactors = userRiskFactorRepository.findActiveRiskFactorsForUser(userId);

            for (UserRiskFactor userRiskFactor : userRiskFactors) {
                UserMedicalEntity entity = new UserMedicalEntity();
                entity.setCui(userRiskFactor.getRiskFactor().getCui());
                entity.setName(userRiskFactor.getRiskFactor().getName());
                entity.setType("risk_factor");
                entity.setStatus(userRiskFactor.getIsActive() ? "active" : "inactive");
                entity.setStartDate(userRiskFactor.getIdentifiedDate());
                entity.setSeverity(userRiskFactor.getRiskLevel());

                // מידע נוסף
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("notes", userRiskFactor.getNotes());
                entity.setAdditionalData(additionalData);

                riskFactors.add(entity);
            }

        } catch (Exception e) {
            logger.error("Error loading risk factors for user {}: {}", userId, e.getMessage());
        }

        return riskFactors;
    }


    //קבלת הסימפטומים של המשתמש
    public List<UserMedicalEntity> getUserActiveSymptoms(UUID userId) {
        List<UserMedicalEntity> symptoms = new ArrayList<>();

        try {
            List<UserSymptom> userSymptoms = userSymptomRepository.findActiveSymptomsForUser(userId);

            for (UserSymptom userSymptom : userSymptoms) {
                UserMedicalEntity entity = new UserMedicalEntity();
                entity.setCui(userSymptom.getSymptom().getCui());
                entity.setName(userSymptom.getSymptom().getName());
                entity.setType("symptom");
                entity.setStatus(userSymptom.getIsActive() ? "active" : "inactive");
                entity.setStartDate(userSymptom.getStartDate());
                entity.setEndDate(userSymptom.getEndDate());
                entity.setSeverity(userSymptom.getSeverity());

                // מידע נוסף
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("frequency", userSymptom.getFrequency());
                additionalData.put("notes", userSymptom.getNotes());
                entity.setAdditionalData(additionalData);

                symptoms.add(entity);
            }

        } catch (Exception e) {
            logger.error("Error loading symptoms for user {}: {}", userId, e.getMessage());
        }

        return symptoms;
    }

    //מידע בסיסי על המשתמש
    private Map<String, Object> buildBasicInfo(User user) {
        Map<String, Object> basicInfo = new HashMap<>();
        try{
            basicInfo.put("username", user.getUsername());
            basicInfo.put("email", user.getEmail());
            basicInfo.put("gender", user.getGender());
            //חישוב גיל
            if(user.getDateOfBirth() != null){
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
            // גורמי סיכון בסיסיים
            if (user.getSmokingStatus() != null) {
                basicInfo.put("smokingStatus", user.getSmokingStatus());
            }

            if (user.getAlcoholConsumption() != null) {
                basicInfo.put("alcoholConsumption", user.getAlcoholConsumption());
            }

            if (user.getPhysicalActivity() != null) {
                basicInfo.put("physicalActivity", user.getPhysicalActivity());
            }

            if (user.getBloodPressure() != null) {
                basicInfo.put("bloodPressure", user.getBloodPressure());
            }

            if (user.getStressLevel() != null) {
                basicInfo.put("stressLevel", user.getStressLevel());
            }

            if (user.getAgeGroup() != null) {
                basicInfo.put("ageGroup", user.getAgeGroup());
            }

            // הסטוריה משפחתית
            if (user.getFamilyHeartDisease() != null) {
                basicInfo.put("familyHeartDisease", user.getFamilyHeartDisease());
            }

            if (user.getFamilyCancer() != null) {
                basicInfo.put("familyCancer", user.getFamilyCancer());
            }
        } catch (Exception e) {
            logger.error("Error building basic info for user: {}",e.getMessage());
        }
        return basicInfo;
    }

    //BMI במילים
    private String getBmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        if (bmi < 35) return "Obese class 1";
        if (bmi < 40) return "Obese class 2";
        return "Obese class 3";
    }

    //חישוב רמת סיכון
    private String calculateRiskLevel(double riskScore) {
        if (riskScore < 0.2) return "Low Risk";
        if (riskScore < 0.5) return "Moderate Risk";
        if (riskScore < 0.7) return "High Risk";
        return "Very High Risk";
    }

    //בדיקת משתמש ותרופה
    public boolean userTakesMedication(UUID userId, String medicationCui) {
        try{
            List<UserMedication> activeMedications = userMedicationRepository.findActiveMedicationsForUser(userId);
            return activeMedications.stream().anyMatch(medication-> medication.getMedication().getCui().equals(medicationCui));
        } catch (Exception e) {
            logger.error("Error checking if user takes medication: {}", e.getMessage());
            return false;
        }
    }

    //בדיקת משתמש ומחלה
    public boolean userHasDisease(UUID userId, String diseaseCui) {
        try {
            List<UserDisease> activeDiseases = userDiseaseRepository.findActiveDiseasesForUser(userId);
            return activeDiseases.stream()
                    .anyMatch(disease -> disease.getDisease().getCui().equals(diseaseCui));
        } catch (Exception e) {
            logger.error("Error checking if user has disease: {}", e.getMessage());
            return false;
        }
    }
}

