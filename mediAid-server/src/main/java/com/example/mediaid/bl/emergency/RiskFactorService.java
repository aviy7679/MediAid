package com.example.mediaid.bl.emergency;

import com.example.mediaid.neo4j.RiskFactorSer;
import com.example.mediaid.dal.User;
import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.dal.user_medical_history.RiskFactorEnums;
import com.example.mediaid.dto.RiskFactorResponseDTO;
import com.example.mediaid.dto.RiskFactorUpdateDTO;
import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.example.mediaid.dto.emergency.MedicalConnection;
import com.example.mediaid.dto.emergency.UserMedicalContext;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RiskFactorService {

    private final UserRepository userRepository;

    @Autowired
    private RiskFactorSer riskFactorSer; // Neo4j service

    private final Logger logger = LoggerFactory.getLogger(RiskFactorService.class);

    //הכנסת גורמי סיכון משתמש
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

    private String calculateRiskLevel(double riskScore) {
        if (riskScore < 0.2) return "Low Risk";
        if (riskScore < 0.5) return "Moderate Risk";
        if (riskScore < 0.7) return "High Risk";
        return "Very High Risk";
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

    /**
     * עדכון גורמי סיכון של המשתמש ב-Neo4j
     */
    public void updateUserRiskFactorsInNeo4j(UUID userId, UserMedicalContext userContext) {
        try {
            Map<String, Double> userRiskFactors = new HashMap<>();

            // המרת גורמי סיכון לערכים מספריים
            if (userContext.getBasicInfo() != null) {
                // גיל
                if (userContext.getBasicInfo().get("age") != null) {
                    userRiskFactors.put("AGE", ((Integer) userContext.getBasicInfo().get("age")).doubleValue());
                }

                // BMI
                if (userContext.getBasicInfo().get("bmi") != null) {
                    userRiskFactors.put("BMI", (Double) userContext.getBasicInfo().get("bmi"));
                }

                // עישון (המרה לציון מספרי)
                if (userContext.getBasicInfo().get("smokingRiskWeight") != null) {
                    double smokingWeight = (Double) userContext.getBasicInfo().get("smokingRiskWeight");
                    userRiskFactors.put("SMOKING_SCORE", smokingWeight * 10); // נרמול לסקלה של 0-10
                }

                // לחץ דם (נמיר מהתיאור למספר)
                if (userContext.getBasicInfo().get("bloodPressure") != null) {
                    String bpDescription = (String) userContext.getBasicInfo().get("bloodPressure");
                    double bpValue = convertBloodPressureDescriptionToValue(bpDescription);
                    userRiskFactors.put("BLOOD_PRESSURE_SYSTOLIC", bpValue);
                }
            }

            // יצירת קשרים ב-Neo4j
            if (!userRiskFactors.isEmpty()) {
                riskFactorSer.createUserRiskFactorRelationships(userId.toString(), userRiskFactors);
                logger.debug("Updated {} risk factors in Neo4j for user {}", userRiskFactors.size(), userId);
            }

        } catch (Exception e) {
            logger.warn("Could not update risk factors in Neo4j for user {}: {}", userId, e.getMessage());
        }
    }

//    /**
//     * ניתוח השפעת גורמי הסיכון על הסימפטומים
//     */
//    public List<MedicalConnection> analyzeRiskFactorImpact(UserMedicalContext userContext, Set<ExtractedSymptom> symptoms) {
//        List<MedicalConnection> riskConnections = new ArrayList<>();
//
//        try {
//            for (UserMedicalEntity riskFactor : userContext.getRiskFactors()) {
//                for (ExtractedSymptom symptom : symptoms) {
//                    MedicalConnection connection = analyzeSpecificRiskFactorImpact(riskFactor, symptom, userContext);
//                    if (connection != null) {
//                        riskConnections.add(connection);
//                    }
//                }
//            }
//
//            logger.debug("Analyzed risk factor impact: found {} connections", riskConnections.size());
//
//        } catch (Exception e) {
//            logger.error("Error analyzing risk factor impact: {}", e.getMessage());
//        }
//
//        return riskConnections;
//    }

//    /**
//     * ניתוח השפעה של גורם סיכון ספציפי על סימפטום
//     */
//    public MedicalConnection analyzeSpecificRiskFactorImpact(UserMedicalEntity riskFactor, ExtractedSymptom symptom, UserMedicalContext userContext) {
//        try {
//            // לוגיקת ניתוח לפי סוג גורם הסיכון והסימפטום
//            String riskType = getRiskFactorType(riskFactor);
//            double riskWeight = getRiskFactorWeight(riskFactor);
//
//            // מיפוי קשרים ידועים בין גורמי סיכון לסימפטומים
//            boolean isRelevant = isRiskFactorRelevantToSymptom(riskType, symptom.getName(), riskWeight);
//
//            if (isRelevant) {
//                MedicalConnection connection = new MedicalConnection();
//                connection.setType(MedicalConnection.ConnectionType.RISK_FACTOR);
//                connection.setFromEntity(riskFactor.getName());
//                connection.setToEntity(symptom.getName());
//                connection.setFromCui(riskFactor.getCui());
//                connection.setToCui(symptom.getCui());
//                connection.setConfidence(calculateRiskFactorConfidence(riskType, symptom.getName(), riskWeight));
//                connection.setExplanation(buildRiskFactorExplanation(riskFactor, symptom, userContext));
//
//                return connection;
//            }
//
//        } catch (Exception e) {
//            logger.debug("Error analyzing specific risk factor impact: {}", e.getMessage());
//        }
//
//        return null;
//    }

    /**
     * חישוב ציון גורמי סיכון ממוצע
     */
    public double calculateOverallRiskFactorScore(UserMedicalContext userContext) {
        if (userContext.getRiskFactors().isEmpty()) {
            return 0.0;
        }

        double totalWeight = 0.0;
        int factorCount = 0;

        for (UserMedicalEntity riskFactor : userContext.getRiskFactors()) {
            double weight = getRiskFactorWeight(riskFactor);
            if (weight > 0.1) { // רק גורמי סיכון משמעותיים
                totalWeight += weight;
                factorCount++;
            }
        }

        return factorCount > 0 ? totalWeight / factorCount : 0.0;
    }

//    /**
//     * קביעת רמת דחיפות כולל גורמי סיכון
//     */
//    public double calculateRiskBasedUrgencyMultiplier(UserMedicalContext userContext, List<MedicalConnection> connections) {
//        double baseMultiplier = 1.0;
//
//        // ניתוח גורמי סיכון
//        double riskFactorScore = calculateOverallRiskFactorScore(userContext);
//        logger.debug("Overall risk factor score: {}", riskFactorScore);
//
//        // קשרי גורמי סיכון
//        long riskFactorConnections = connections.stream()
//                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.RISK_FACTOR)
//                .filter(conn -> conn.getConfidence() > 0.7)
//                .count();
//
//        // חישוב מכפיל הדחיפות
//        if (riskFactorScore > 0.8 && riskFactorConnections > 2) {
//            baseMultiplier = 1.5; // העלאת דחיפות משמעותית
//        } else if (riskFactorScore > 0.6 && riskFactorConnections > 1) {
//            baseMultiplier = 1.3;
//        } else if (riskFactorScore > 0.4 || riskFactorConnections > 0) {
//            baseMultiplier = 1.1;
//        }
//
//        return baseMultiplier;
//    }
//

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
     * המרת תיאור לחץ דם לערך מספרי
     */
    private double convertBloodPressureDescriptionToValue(String description) {
        return switch (description.toLowerCase()) {
            case "normal" -> 120;
            case "elevated" -> 130;
            case "stage 1 hypertension" -> 140;
            case "stage 2 hypertension" -> 160;
            case "hypertensive crisis" -> 180;
            default -> 120;
        };
    }

//    /**
//     * בדיקה אם גורם סיכון רלוונטי לסימפטום
//     */
//    private boolean isRiskFactorRelevantToSymptom(String riskType, String symptomName, double riskWeight) {
//        Map<String, Set<String>> riskSymptomMap = Map.of(
//                "SMOKING", Set.of("chest pain", "shortness of breath", "cough", "fatigue"),
//                "BMI", Set.of("chest pain", "shortness of breath", "fatigue", "joint pain"),
//                "BLOOD_PRESSURE", Set.of("headache", "chest pain", "dizziness", "fatigue"),
//                "AGE_GROUP", Set.of("fatigue", "joint pain", "memory problems", "dizziness"),
//                "FAMILY_HEART_DISEASE", Set.of("chest pain", "shortness of breath", "palpitations"),
//                "FAMILY_CANCER", Set.of("fatigue", "weight loss", "pain"),
//                "STRESS", Set.of("headache", "chest pain", "fatigue", "insomnia"),
//                "PHYSICAL_ACTIVITY", Set.of("fatigue", "shortness of breath", "chest pain")
//        );
//
//        Set<String> relevantSymptoms = riskSymptomMap.get(riskType);
//        if (relevantSymptoms == null) return false;
//
//        return relevantSymptoms.stream()
//                .anyMatch(rs -> symptomName.toLowerCase().contains(rs.toLowerCase())) && riskWeight > 0.2;
//    }
//
//    /**
//     * חישוב רמת ביטחון לקשר גורם סיכון-סימפטום
//     */
//    private double calculateRiskFactorConfidence(String riskType, String symptomName, double riskWeight) {
//        double baseConfidence = switch (riskType) {
//            case "SMOKING" -> 0.8;
//            case "BMI" -> 0.7;
//            case "BLOOD_PRESSURE" -> 0.85;
//            case "FAMILY_HEART_DISEASE" -> 0.75;
//            case "AGE_GROUP" -> 0.6;
//            default -> 0.5;
//        };
//
//        return Math.min(0.95, baseConfidence * (1 + riskWeight));
//    }
//
//    /**
//     * בניית הסבר לקשר גורם סיכון-סימפטום
//     */
//    private String buildRiskFactorExplanation(UserMedicalEntity riskFactor, ExtractedSymptom symptom, UserMedicalContext userContext) {
//        String riskType = getRiskFactorType(riskFactor);
//
//        return switch (riskType) {
//            case "SMOKING" -> String.format("Smoking can be a contributing factor to the symptom %s due to its effects on the respiratory and cardiovascular systems.", symptom.getName());
//            case "BMI" -> String.format("Excess weight may contribute to the occurrence of %s due to the additional strain on the body.", symptom.getName());
//            case "BLOOD_PRESSURE" -> String.format("High blood pressure can lead to %s as a result of stress on the cardiovascular system.", symptom.getName());
//            case "FAMILY_HEART_DISEASE" -> String.format("A family history of heart disease increases the risk of developing %s.", symptom.getName());
//            case "AGE_GROUP" -> String.format("Age can be a contributing factor to the development of %s.", symptom.getName());
//            default -> String.format("The risk factor %s may be related to the symptom %s.", riskFactor.getName(), symptom.getName());
//        };
//    }


//    public String getRiskFactorType(UserMedicalEntity riskFactor) {
//        if (riskFactor.getAdditionalData() != null && riskFactor.getAdditionalData().get("risk_type") != null) {
//            return (String) riskFactor.getAdditionalData().get("risk_type");
//        }
//
//        String name = riskFactor.getName().toLowerCase();
//        if (name.contains("smok")) return "SMOKING";
//        if (name.contains("bmi") || name.contains("weight")) return "BMI";
//        if (name.contains("blood pressure") || name.contains("hypertension")) return "BLOOD_PRESSURE";
//        if (name.contains("age")) return "AGE_GROUP";
//        if (name.contains("family") && name.contains("heart")) return "FAMILY_HEART_DISEASE";
//        if (name.contains("family") && name.contains("cancer")) return "FAMILY_CANCER";
//        if (name.contains("stress")) return "STRESS";
//        if (name.contains("activity") || name.contains("exercise")) return "PHYSICAL_ACTIVITY";
//
//        return "OTHER";
//    }

    public double getRiskFactorWeight(UserMedicalEntity riskFactor) {
        if (riskFactor.getAdditionalData() != null && riskFactor.getAdditionalData().get("weight") != null) {
            return (Double) riskFactor.getAdditionalData().get("weight");
        }

        // ברירת מחדל לפי חומרה
        return switch (riskFactor.getSeverity() != null ? riskFactor.getSeverity().toLowerCase() : "medium") {
            case "high" -> 0.8;
            case "medium" -> 0.5;
            case "low" -> 0.3;
            default -> 0.5;
        };
    }
}