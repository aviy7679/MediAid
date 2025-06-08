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


    private Double calculateBMI(User user) {
        if (user.getHeight() != null && user.getWeight() != null &&
                user.getHeight() > 0 && user.getWeight() > 0) {
            double heightInMeters = user.getHeight() / 100.0;
            double bmi = user.getWeight() / (heightInMeters * heightInMeters);
            return Math.round(bmi * 100.0) / 100.0;
        }
        return null;
    }


}