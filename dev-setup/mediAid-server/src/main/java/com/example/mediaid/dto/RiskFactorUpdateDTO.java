package com.example.mediaid.dto;

import com.example.mediaid.dal.user_medical_history.RiskFactorEnums.*;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO מעודכן לעדכון גורמי סיכון - כולל את כל הגורמים מהשאלון
 */
@Data
public class RiskFactorUpdateDTO {

    // גורמי סיכון בסיסיים
    private SmokingStatus smokingStatus;
    private AlcoholConsumption alcoholConsumption;
    private PhysicalActivity physicalActivity;
    private BloodPressure bloodPressure;
    private StressLevel stressLevel;
    private AgeGroup ageGroup;

    // היסטוריה משפחתית
    private FamilyHeartDisease familyHeartDisease;
    private FamilyCancer familyCancer;

    // מדדים פיזיים
    private Float height;
    private Float weight;
    private Double bmi;

    // מדדים רפואיים נוספים (לעתיד)
    private Double bloodGlucose;
    private Integer systolicBloodPressure;
    private Integer diastolicBloodPressure;
    private Double cholesterolLevel;

    // גורמי סיכון נוספים (בוליאניים)
    private Boolean hasChronicKidneyDisease;
    private Boolean hasLiverDisease;
    private Boolean hasThyroidDisease;
    private Boolean hasAutoimmuneDiseases;

    // תרופות משמעותיות (השפעה על גורמי סיכון)
    private Boolean takesBloodThinners;
    private Boolean takesHypertensionMedication;
    private Boolean takesDiabetesMedication;
    private Boolean takesStatins;

    // נתונים סביבתיים
    private String occupationalHazards; // חשיפה תעסוקתית
    private String livingEnvironment;   // סביבת מגורים
    private Boolean exposureToToxins;   // חשיפה לרעלים

    // הרגלי חיים נוספים
    private Integer sleepHours;         // שעות שינה
    private String sleepQuality;        // איכות שינה
    private String dietaryPattern;      // דפוס תזונתי
    private Boolean regularMedicalCheckups; // בדיקות רפואיות קבועות

    // מידע נוסף
    private String notes;               // הערות כלליות
    private String updateReason;        // סיבת העדכון

    /**
     * חישוב BMI מגובה ומשקל
     */
    public Double calculateBMI() {
        if (height != null && weight != null && height > 0 && weight > 0) {
            double heightInMeters = height / 100.0;
            double calculatedBmi = weight / (heightInMeters * heightInMeters);
            return Math.round(calculatedBmi * 100.0) / 100.0;
        }
        return bmi;
    }

    /**
     * קבלת BMI (מחושב או קיים)
     */
    public Double getBMI() {
        if (bmi != null) {
            return bmi;
        }
        return calculateBMI();
    }

    /**
     * בדיקת תקינות הנתונים
     */
    public boolean isDataValid() {
        // בדיקות בסיסיות
        if (height != null && (height < 50 || height > 250)) {
            return false;
        }

        if (weight != null && (weight < 20 || weight > 300)) {
            return false;
        }

        if (bloodGlucose != null && (bloodGlucose < 50 || bloodGlucose > 500)) {
            return false;
        }

        if (systolicBloodPressure != null && (systolicBloodPressure < 70 || systolicBloodPressure > 250)) {
            return false;
        }

        if (diastolicBloodPressure != null && (diastolicBloodPressure < 40 || diastolicBloodPressure > 150)) {
            return false;
        }

        if (sleepHours != null && (sleepHours < 3 || sleepHours > 16)) {
            return false;
        }

        return true;
    }

    /**
     * קבלת רשימת שדות שהשתנו
     */
    public Map<String, Object> getUpdatedFields() {
        Map<String, Object> updatedFields = new HashMap<>();

        if (smokingStatus != null) updatedFields.put("smokingStatus", smokingStatus);
        if (alcoholConsumption != null) updatedFields.put("alcoholConsumption", alcoholConsumption);
        if (physicalActivity != null) updatedFields.put("physicalActivity", physicalActivity);
        if (bloodPressure != null) updatedFields.put("bloodPressure", bloodPressure);
        if (stressLevel != null) updatedFields.put("stressLevel", stressLevel);
        if (ageGroup != null) updatedFields.put("ageGroup", ageGroup);
        if (familyHeartDisease != null) updatedFields.put("familyHeartDisease", familyHeartDisease);
        if (familyCancer != null) updatedFields.put("familyCancer", familyCancer);
        if (height != null) updatedFields.put("height", height);
        if (weight != null) updatedFields.put("weight", weight);
        if (bloodGlucose != null) updatedFields.put("bloodGlucose", bloodGlucose);
        if (systolicBloodPressure != null) updatedFields.put("systolicBloodPressure", systolicBloodPressure);
        if (diastolicBloodPressure != null) updatedFields.put("diastolicBloodPressure", diastolicBloodPressure);
        if (cholesterolLevel != null) updatedFields.put("cholesterolLevel", cholesterolLevel);

        return updatedFields;
    }

    /**
     * חישוב ציון סיכון מהיר
     */
    public double calculateQuickRiskScore() {
        double score = 0.0;
        int factors = 0;

        if (smokingStatus != null) {
            score += smokingStatus.getWeight();
            factors++;
        }
        if (alcoholConsumption != null) {
            score += alcoholConsumption.getWeight();
            factors++;
        }
        if (physicalActivity != null) {
            score += physicalActivity.getWeight();
            factors++;
        }
        if (bloodPressure != null) {
            score += bloodPressure.getWeight();
            factors++;
        }
        if (stressLevel != null) {
            score += stressLevel.getWeight();
            factors++;
        }
        if (ageGroup != null) {
            score += ageGroup.getWeight();
            factors++;
        }
        if (familyHeartDisease != null) {
            score += familyHeartDisease.getWeight();
            factors++;
        }
        if (familyCancer != null) {
            score += familyCancer.getWeight();
            factors++;
        }

        // הוספת BMI
        Double calculatedBMI = getBMI();
        if (calculatedBMI != null) {
            BMICategory bmiCategory = BMICategory.fromBMI(calculatedBMI);
            score += bmiCategory.getWeight();
            factors++;
        }

        return factors > 0 ? score / factors : 0.0;
    }

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
                ", bmi=" + getBMI() +
                ", quickRiskScore=" + calculateQuickRiskScore() +
                '}';
    }
}