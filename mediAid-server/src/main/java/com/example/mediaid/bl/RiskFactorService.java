package com.example.mediaid.bl;


import com.example.mediaid.dal.User;
import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.dal.user_medical_history.RiskFactorEnums;
import com.example.mediaid.dto.RiskFactorResponseDTO;
import com.example.mediaid.dto.RiskFactorUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskFactorService {

    private final UserRepository userRepository;

    private final Logger logger = LoggerFactory.getLogger(RiskFactorService.class);

    @Transactional
    public RiskFactorResponseDTO updateUserRiskFactors(UUID userId, RiskFactorUpdateDTO dto){

        logger.info("Starting update for user: {} risk factors", userId);

        User user = userRepository.findById(userId).orElse(null);
        if(user == null){
            throw new RuntimeException("user not found with id:{}" + userId);
        }

        logger.info("Updating user: {} risk factors", userId);
        if(dto.getSmokingStatus()!=null){
            user.setSmokingStatus(dto.getSmokingStatus());
            logger.info("Updating user: {} smoking status", userId);
        }
        if (dto.getAlcoholConsumption()!=null){
            user.setAlcoholConsumption(dto.getAlcoholConsumption());
            logger.info("Updating user: {} alcohol consumption", userId);
        }
        if(dto.getPhysicalActivity()!=null){
            user.setPhysicalActivity(dto.getPhysicalActivity());
            logger.info("Updating user: {} physical activity", userId);
        }
        if(dto.getBloodPressure()!=null){
            user.setBloodPressure(dto.getBloodPressure());
            logger.info("Updating user: {} blood pressure", userId);
        }
        if(dto.getStressLevel()!=null){
            user.setStressLevel(dto.getStressLevel());
            logger.info("Updating user: {} stress level", userId);
        }
        if(dto.getAgeGroup()!=null){
            user.setAgeGroup(dto.getAgeGroup());
            logger.info("Updating user: {} age group", userId);
        }
        if(dto.getFamilyCancer()!=null){
            user.setFamilyCancer(dto.getFamilyCancer());
            logger.info("Updating user: {} family anccer", userId);
        }
        if(dto.getHeight()!=null && dto.getHeight()>0){
            user.setHeight(dto.getHeight());
            logger.info("Updating user: {} height", userId);
        }
        if(dto.getWeight()!=null && dto.getWeight()>0){
            user.setWeight(dto.getWeight());
            logger.info("Updating user: {} weight", userId);
        }

        User savedUser = userRepository.save(user);
        logger.info("User saved successfully");

        double overallRiskScore = savedUser.calculateOverallRiskScore();
        String riskLevel = calculateRiskLevel(overallRiskScore);

        Double bmi = dto.getBmi();
        String bmiCategory = "";
        if (bmi != null && bmi > 0) {
            bmiCategory = RiskFactorEnums.BMICategory.fromBMI(bmi).getDescription();
        }

        System.out.println("Calculated risk score: " + overallRiskScore + ", level: " + riskLevel);

        return new RiskFactorResponseDTO(
                "Risk factors updated successfully",
                overallRiskScore,
                riskLevel,
                bmi,
                bmiCategory
        );
    }

    private String calculateRiskLevel(double riskScore) {
        if (riskScore < 0.2) return "Low Risk";
        if (riskScore < 0.5) return "Moderate Risk";
        if (riskScore < 0.7) return "High Risk";
        return "Very High Risk";
    }

    public RiskFactorResponseDTO getUserRiskFactors(UUID userId) {
        System.out.println("RiskFactorService: Getting risk factors for user " + userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        double overallRiskScore = user.calculateOverallRiskScore();
        String riskLevel = calculateRiskLevel(overallRiskScore);

        // חישוב BMI אם יש גובה ומשקל
        Double bmi = null;
        String bmiCategory = "";
        if (user.getHeight() != null && user.getWeight() != null &&
                user.getHeight() > 0 && user.getWeight() > 0) {
            double heightInMeters = user.getHeight() / 100.0;
            bmi = user.getWeight() / (heightInMeters * heightInMeters);
            bmi = Math.round(bmi * 100.0) / 100.0; // עיגול לשני מקומות
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
    }
