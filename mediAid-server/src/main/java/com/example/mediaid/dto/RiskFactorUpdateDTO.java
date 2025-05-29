package com.example.mediaid.dto;

import com.example.mediaid.dal.user_medical_history.RiskFactorEnums.*;
import lombok.Data;

@Data
public class RiskFactorUpdateDTO {
    private SmokingStatus smokingStatus;
    private AlcoholConsumption alcoholConsumption;
    private PhysicalActivity physicalActivity;
    private BloodPressure bloodPressure;
    private StressLevel stressLevel;
    private AgeGroup ageGroup;
    private FamilyHeartDisease familyHeartDisease;
    private FamilyCancer familyCancer;
    private Float height;
    private Float weight;
    private Double bmi;

    @Override
    public String toString() {
        return "RiskFactorUpdateDTO{" +
                "smokingStatus=" + smokingStatus +
                ", alcoholConsumption=" + alcoholConsumption +
                ", physicalActivity=" + physicalActivity +
                ", bloodPressure=" + bloodPressure +
                ", stressLevel=" + stressLevel +
                ", ageGroup=" + ageGroup +
                ", familyHeartDisease=" + familyHeartDisease +
                ", familyCancer=" + familyCancer +
                ", height=" + height +
                ", weight=" + weight +
                ", bmi=" + bmi +
                '}';
    }
}