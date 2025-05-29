package com.example.mediaid.bl;

import com.example.mediaid.dal.User;
import com.example.mediaid.dal.user_medical_history.RiskFactorEnums.BMICategory;
import com.example.mediaid.dto.RiskFactorDto;
import com.example.mediaid.dto.RiskFactorResponseDTO;
import com.example.mediaid.dal.UserRepository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskFactorService {

    private final UserRepository userRepository;

    @Transactional
    public RiskFactorResponseDTO updateUserRiskFactors(UUID userId, RiskFactorUpdateDTO dto) {
        // מציאת המשתמש
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // עדכון השדות רק אם הם לא null
        if (dto.getSmokingStatus() != null) {
            user.setSmokingStatus(dto.getSmokingStatus());
        }
        if (dto.getAlcoholConsumption() != null) {
            user.setAlcoholConsumption(dto.getAlcoholConsumption());
        }
        if (dto.getPhysicalActivity() != null) {
            user.setPhysicalActivity(dto.getPhysicalActivity());
        }
        if (dto.getBloodPressure() != null) {
            user.setBloodPressure(dto.getBloodPressure());
        }
        if (dto.getStressLevel() != null) {
            user.setStressLevel(dto.getStressLevel());
        }
        if (dto.getAgeGroup() != null) {
            user.setAgeGroup(dto.getAgeGroup());
        }
        if (dto.getFamilyHeartDisease() != null) {
            user.setFamilyHeartDisease(dto.getFamilyHeartDisease());
        }
        if (dto.getFamilyCancer() != null) {
            user.setFamilyCancer(dto.getFamilyCancer());
        }

        // שמירת המשתמש
        User savedUser = userRepository.save(user);

        // חישוב התוצאות
        double overallRiskScore = savedUser.calculateOverallRiskScore();
        String riskLevel = calculateRiskLevel(overallRiskScore);

        Double bmi = dto.getBmi();
        String bmiCategory = "";
        if (bmi != null) {
            bmiCategory = BMICategory.fromBMI(bmi).getDescription();
        }

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        double overallRiskScore = user.calculateOverallRiskScore();
        String riskLevel = calculateRiskLevel(overallRiskScore);

        // BMI לא נשמר ב-User Entity, אז נחזיר null
        // אם תרצי לשמור BMI ב-Entity, תוכלי להוסיף שדה bmi ל-User

        return new RiskFactorResponseDTO(
                "Risk factors retrieved successfully",
                overallRiskScore,
                riskLevel,
                null, // BMI לא זמין
                "" // BMI category לא זמין
        );
    }
}