package com.example.mediaid.bl.emergency;

import com.example.mediaid.bl.neo4j.RiskFactorSer;
import com.example.mediaid.dal.User;
import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.dal.user_medical_history.RiskFactorEnums;
import com.example.mediaid.dto.RiskFactorResponseDTO;
import com.example.mediaid.dto.RiskFactorUpdateDTO;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RiskFactorService {

    private final UserRepository userRepository;

    @Autowired
    private RiskFactorSer riskFactorSer; // Neo4j service

    private final Logger logger = LoggerFactory.getLogger(RiskFactorService.class);

    @Transactional
    public RiskFactorResponseDTO updateUserRiskFactors(UUID userId, RiskFactorUpdateDTO dto) {
        logger.info("Starting update for user: {} risk factors", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));


        logger.info("Updating user: {} risk factors", userId);

        // עדכון גורמי הסיכון בUser entity
        updateUserRiskFactorsInDB(user, dto);

        // שמירה במסד הנתונים
        User savedUser = userRepository.save(user);
        logger.info("User saved successfully");

        // חישוב ציון סיכון כולל
        double overallRiskScore = savedUser.calculateOverallRiskScore();
        String riskLevel = calculateRiskLevel(overallRiskScore);

        // עדכון גורמי סיכון דינמיים ב-Neo4j
        updateDynamicRiskFactorsInNeo4j(savedUser);

        // חישוב BMI
        Double bmi = calculateBMI(savedUser);
        String bmiCategory = "";
        if (bmi != null && bmi > 0) {
            bmiCategory = RiskFactorEnums.BMICategory.fromBMI(bmi).getDescription();
        }

        logger.info("Calculated risk score: {}, level: {}", overallRiskScore, riskLevel);

        return new RiskFactorResponseDTO(
                "Risk factors updated successfully",
                overallRiskScore,
                riskLevel,
                bmi,
                bmiCategory
        );
    }

    private void updateUserRiskFactorsInDB(User user, RiskFactorUpdateDTO dto) {
        if (dto.getSmokingStatus() != null) {
            user.setSmokingStatus(dto.getSmokingStatus());
            logger.info("Updated smoking status for user: {}", user.getUserId());
        }
        if (dto.getAlcoholConsumption() != null) {
            user.setAlcoholConsumption(dto.getAlcoholConsumption());
            logger.info("Updated alcohol consumption for user: {}", user.getUserId());
        }
        if (dto.getPhysicalActivity() != null) {
            user.setPhysicalActivity(dto.getPhysicalActivity());
            logger.info("Updated physical activity for user: {}", user.getUserId());
        }
        if (dto.getBloodPressure() != null) {
            user.setBloodPressure(dto.getBloodPressure());
            logger.info("Updated blood pressure for user: {}", user.getUserId());
        }
        if (dto.getStressLevel() != null) {
            user.setStressLevel(dto.getStressLevel());
            logger.info("Updated stress level for user: {}", user.getUserId());
        }
        if (dto.getAgeGroup() != null) {
            user.setAgeGroup(dto.getAgeGroup());
            logger.info("Updated age group for user: {}", user.getUserId());
        }
        if (dto.getFamilyCancer() != null) {
            user.setFamilyCancer(dto.getFamilyCancer());
            logger.info("Updated family cancer history for user: {}", user.getUserId());
        }
        if (dto.getFamilyHeartDisease() != null) {
            user.setFamilyHeartDisease(dto.getFamilyHeartDisease());
            logger.info("Updated family heart disease history for user: {}", user.getUserId());
        }
        if (dto.getHeight() != null && dto.getHeight() > 0) {
            user.setHeight(dto.getHeight());
            logger.info("Updated height for user: {}", user.getUserId());
        }
        if (dto.getWeight() != null && dto.getWeight() > 0) {
            user.setWeight(dto.getWeight());
            logger.info("Updated weight for user: {}", user.getUserId());
        }
    }

    /**
     * עדכון גורמי סיכון דינמיים ב-Neo4j
     */
    private void updateDynamicRiskFactorsInNeo4j(User user) {
        try {
            // עדכון גיל
            if (user.getDateOfBirth() != null) {
                int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
                riskFactorSer.createOrUpdateRiskFactor("AGE", age);
                riskFactorSer.updateRiskFactorRelationships("AGE", age);
            }

            // עדכון BMI
            Double bmi = calculateBMI(user);
            if (bmi != null) {
                riskFactorSer.createOrUpdateRiskFactor("BMI", bmi);
                riskFactorSer.updateRiskFactorRelationships("BMI", bmi);
            }

            // עדכון לחץ דם (נמיר מ-enum לערך מספרי)
            if (user.getBloodPressure() != null) {
                double bpValue = convertBloodPressureToNumeric(user.getBloodPressure());
                riskFactorSer.createOrUpdateRiskFactor("BLOOD_PRESSURE_SYSTOLIC", bpValue);
                riskFactorSer.updateRiskFactorRelationships("BLOOD_PRESSURE_SYSTOLIC", bpValue);
            }

            logger.info("Updated dynamic risk factors in Neo4j for user: {}", user.getUserId());

        } catch (Exception e) {
            logger.error("Error updating dynamic risk factors in Neo4j for user: {}", user.getUserId(), e);
        }
    }

    /**
     * המרת enum של לחץ דם לערך מספרי
     */
    private double convertBloodPressureToNumeric(RiskFactorEnums.BloodPressure bp) {
        return switch (bp) {
            case NORMAL -> 120;
            case ELEVATED -> 130;
            case STAGE_1 -> 140;
            case STAGE_2 -> 160;
            case CRISIS -> 180;
        };
    }

    public RiskFactorResponseDTO getUserRiskFactors(UUID userId) {
        logger.info("Getting risk factors for user: {}", userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        double overallRiskScore = user.calculateOverallRiskScore();
        String riskLevel = calculateRiskLevel(overallRiskScore);

        Double bmi = calculateBMI(user);
        String bmiCategory = "";
        if (bmi != null) {
            bmiCategory = RiskFactorEnums.BMICategory.fromBMI(bmi).getDescription();
        }

        return new RiskFactorResponseDTO(
                "Risk factors retrieved successfully",
                overallRiskScore,
                riskLevel,
                bmi,
                bmiCategory
        );
    }

    /**
     * יצירת רשימת גורמי סיכון פעילים למשתמש - לשימוש בניתוח רפואי
     */
    public List<UserMedicalEntity> getUserActiveRiskFactors(UUID userId) {
        logger.info("Loading active risk factors for user: {}", userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("User not found: {}", userId);
            return new ArrayList<>();
        }

        List<UserMedicalEntity> riskFactors = new ArrayList<>();

        // עישון
        if (user.getSmokingStatus() != null && user.getSmokingStatus().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("SMOKING", user.getSmokingStatus().getDescription(),
                    user.getSmokingStatus().getWeight()));
        }

        // אלכוהול
        if (user.getAlcoholConsumption() != null && user.getAlcoholConsumption().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("ALCOHOL", user.getAlcoholConsumption().getDescription(),
                    user.getAlcoholConsumption().getWeight()));
        }

        // פעילות גופנית
        if (user.getPhysicalActivity() != null && user.getPhysicalActivity().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("PHYSICAL_ACTIVITY", user.getPhysicalActivity().getDescription(),
                    user.getPhysicalActivity().getWeight()));
        }

        // לחץ דם
        if (user.getBloodPressure() != null && user.getBloodPressure().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("BLOOD_PRESSURE", user.getBloodPressure().getDescription(),
                    user.getBloodPressure().getWeight()));
        }

        // סטרס
        if (user.getStressLevel() != null && user.getStressLevel().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("STRESS", user.getStressLevel().getDescription(),
                    user.getStressLevel().getWeight()));
        }

        // גיל
        if (user.getAgeGroup() != null && user.getAgeGroup().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("AGE_GROUP", user.getAgeGroup().getDescription(),
                    user.getAgeGroup().getWeight()));
        }

        // היסטוריה משפחתית - מחלות לב
        if (user.getFamilyHeartDisease() != null && user.getFamilyHeartDisease().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("FAMILY_HEART_DISEASE", user.getFamilyHeartDisease().getDescription(),
                    user.getFamilyHeartDisease().getWeight()));
        }

        // היסטוריה משפחתית - סרטן
        if (user.getFamilyCancer() != null && user.getFamilyCancer().getWeight() > 0.1) {
            riskFactors.add(createRiskFactorEntity("FAMILY_CANCER", user.getFamilyCancer().getDescription(),
                    user.getFamilyCancer().getWeight()));
        }

        // BMI
        Double bmi = calculateBMI(user);
        if (bmi != null) {
            RiskFactorEnums.BMICategory bmiCategory = RiskFactorEnums.BMICategory.fromBMI(bmi);
            if (bmiCategory.getWeight() > 0.1) {
                riskFactors.add(createRiskFactorEntity("BMI", bmiCategory.getDescription(),
                        bmiCategory.getWeight()));
            }
        }

        logger.info("Found {} active risk factors for user: {}", riskFactors.size(), userId);
        return riskFactors;
    }

    private UserMedicalEntity createRiskFactorEntity(String type, String description, double weight) {
        UserMedicalEntity entity = new UserMedicalEntity();
        entity.setCui("RF_" + type); // סימון מיוחד לגורמי סיכון
        entity.setName(description);
        entity.setType("risk_factor");
        entity.setStatus("active");
        entity.setSeverity(weight > 0.7 ? "high" : weight > 0.4 ? "medium" : "low");

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("weight", weight);
        additionalData.put("risk_type", type);
        entity.setAdditionalData(additionalData);

        return entity;
    }

    private String calculateRiskLevel(double riskScore) {
        if (riskScore < 0.2) return "Low Risk";
        if (riskScore < 0.5) return "Moderate Risk";
        if (riskScore < 0.7) return "High Risk";
        return "Very High Risk";
    }

    private Double calculateBMI(User user) {
        if (user.getHeight() != null && user.getWeight() != null &&
                user.getHeight() > 0 && user.getWeight() > 0) {
            double heightInMeters = user.getHeight() / 100.0;
            double bmi = user.getWeight() / (heightInMeters * heightInMeters);
            return Math.round(bmi * 100.0) / 100.0;
        }
        return null;
    }

    /**
     * עדכון גורמי סיכון ספציפיים
     */
    public void updateSpecificRiskFactor(UUID userId, String riskFactorType, Object value) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        switch (riskFactorType.toUpperCase()) {
            case "SMOKING" -> {
                if (value instanceof RiskFactorEnums.SmokingStatus) {
                    user.setSmokingStatus((RiskFactorEnums.SmokingStatus) value);
                }
            }
            case "ALCOHOL" -> {
                if (value instanceof RiskFactorEnums.AlcoholConsumption) {
                    user.setAlcoholConsumption((RiskFactorEnums.AlcoholConsumption) value);
                }
            }
            case "PHYSICAL_ACTIVITY" -> {
                if (value instanceof RiskFactorEnums.PhysicalActivity) {
                    user.setPhysicalActivity((RiskFactorEnums.PhysicalActivity) value);
                }
            }
            case "BLOOD_PRESSURE" -> {
                if (value instanceof RiskFactorEnums.BloodPressure) {
                    user.setBloodPressure((RiskFactorEnums.BloodPressure) value);
                }
            }
            case "STRESS" -> {
                if (value instanceof RiskFactorEnums.StressLevel) {
                    user.setStressLevel((RiskFactorEnums.StressLevel) value);
                }
            }
            default -> throw new IllegalArgumentException("Unknown risk factor type: " + riskFactorType);
        }

        userRepository.save(user);
        updateDynamicRiskFactorsInNeo4j(user);

        logger.info("Updated specific risk factor {} for user: {}", riskFactorType, userId);
    }
}