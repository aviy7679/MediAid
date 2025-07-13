package com.example.mediaid.dto.emergency;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMedicalContext {
    private List<UserMedicalEntity> currentMedications;
    private List<UserMedicalEntity> activeDiseases;
    private List<UserMedicalEntity> riskFactors;
    private Map<String, Object> basicInfo; // גיל, מין, BMI וכו'
    private Double overallRiskScore;
    private String riskLevel;
}