package com.example.mediaid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskFactorResponseDTO {
    private String message;
    private Double overallRiskScore;
    private String riskLevel;
    private Double bmi;
    private String bmiCategory;
}
