package com.example.mediaid.dal;

import com.example.mediaid.dal.user_medical_history.RiskFactorEnums.*;
import com.example.mediaid.security.encryption.EncryptedStringAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID userId;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(nullable = false)
    private String username;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @Column(name = "height")
    private Float height;

    @Column(name = "weight")
    private Float weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "smoking_status")
    private SmokingStatus smokingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcohol_consumption")
    private AlcoholConsumption alcoholConsumption;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical_activity")
    private PhysicalActivity physicalActivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_heart_disease")
    private FamilyHeartDisease familyHeartDisease;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_cancer")
    private FamilyCancer familyCancer;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_pressure")
    private BloodPressure bloodPressure;

    @Enumerated(EnumType.STRING)
    @Column(name = "stress_level")
    private StressLevel stressLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group")
    private AgeGroup ageGroup;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // המתודות שלך ללא שינוי
    public double calculateOverallRiskScore() {
        // הקוד הקיים שלך...
        double totalRisk = 0.0;
        int factorCount = 0;

        if (smokingStatus != null) {
            totalRisk += smokingStatus.getWeight();
            factorCount++;
        }
        if (alcoholConsumption != null) {
            totalRisk += alcoholConsumption.getWeight();
            factorCount++;
        }
        if (physicalActivity != null) {
            totalRisk += physicalActivity.getWeight();
            factorCount++;
        }
        if (familyHeartDisease != null) {
            totalRisk += familyHeartDisease.getWeight();
            factorCount++;
        }
        if (familyCancer != null) {
            totalRisk += familyCancer.getWeight();
            factorCount++;
        }
        if (bloodPressure != null) {
            totalRisk += bloodPressure.getWeight();
            factorCount++;
        }
        if (stressLevel != null) {
            totalRisk += stressLevel.getWeight();
            factorCount++;
        }
        if (ageGroup != null) {
            totalRisk += ageGroup.getWeight();
            factorCount++;
        }

        return factorCount > 0 ? totalRisk / factorCount : 0.0;
    }

    public Double calculateBMI() {
        if (height != null && weight != null && height > 0 && weight > 0) {
            double heightInMeters = height / 100.0;
            return weight / (heightInMeters * heightInMeters);
        }
        return null;
    }
}