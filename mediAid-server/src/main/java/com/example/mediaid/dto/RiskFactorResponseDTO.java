package com.example.mediaid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RiskFactorResponseDTO {
    private String message;
    private Double overallRiskScore;
    private String riskLevel;
    private Double bmi;
    private String bmiCategory;
}
