package com.example.mediaid.dal.user_medical_history;

public class RiskFactorEnums {

    // מייצג מצב עישון ומשקלו כגורם סיכון
    public enum SmokingStatus {
        NEVER(0.0, "Never smoked"),
        FORMER_LIGHT(0.2, "Former smoker - light"),
        FORMER_HEAVY(0.4, "Former smoker - heavy"),
        CURRENT_LIGHT(0.7, "Current smoker - light"),
        CURRENT_HEAVY(1.0, "Current smoker - heavy");

        private final double weight;
        private final String description;

        SmokingStatus(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    // מייצג צריכת אלכוהול ומשקלה כגורם סיכון
    public enum AlcoholConsumption {
        NEVER(0.0, "Never drinks"),
        LIGHT(0.1, "Drinks lightly"),
        MODERATE(0.3, "Drinks moderately"),
        HEAVY(0.6, "Drinks heavily"),
        EXCESSIVE(0.8, "Drinks excessively");

        private final double weight;
        private final String description;

        AlcoholConsumption(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    // מייצג רמת פעילות גופנית ומשקלה כגורם סיכון
    public enum PhysicalActivity {
        VERY_ACTIVE(0.0, "Very active"),
        ACTIVE(0.1, "Active"),
        MODERATE(0.3, "Moderate"),
        LOW(0.6, "Low activity"),
        SEDENTARY(0.8, "Sedentary");

        private final double weight;
        private final String description;

        PhysicalActivity(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    // מייצג רמות לחץ דם ומשקלן כגורם סיכון
    public enum BloodPressure {
        NORMAL(0.0, "Normal"),
        ELEVATED(0.2, "Elevated"),
        STAGE_1(0.5, "Stage 1 Hypertension"),
        STAGE_2(0.8, "Stage 2 Hypertension"),
        CRISIS(1.0, "Hypertensive crisis");

        private final double weight;
        private final String description;

        BloodPressure(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    // מייצג רמות לחץ נפשי (סטרס) ומשקלן כגורם סיכון
    public enum StressLevel {
        LOW(0.0, "Low"),
        MODERATE(0.2, "Moderate"),
        HIGH(0.5, "High"),
        VERY_HIGH(0.7, "Very high");

        private final double weight;
        private final String description;

        StressLevel(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    // מייצג קבוצות גיל ומשקלן כגורם סיכון
    public enum AgeGroup {
        UNDER_30(0.0, "Under 30"),
        AGE_30_40(0.1, "30-40"),
        AGE_40_50(0.2, "40-50"),
        AGE_50_60(0.4, "50-60"),
        AGE_60_70(0.6, "60-70"),
        OVER_70(0.8, "Over 70");

        private final double weight;
        private final String description;

        AgeGroup(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    //הסטוריית מחלות לב במשפחה

    public enum FamilyHeartDisease {
        NONE(0.0, "No known history"),
        DISTANT(0.3, "Distant relative"),
        SIBLING(0.6, "Sibling"),
        PARENT(0.8, "Parent"),
        MULTIPLE(1.0, "Multiple family members");

        private final double weight;
        private final String description;

        FamilyHeartDisease(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    //הסטוריית סרטן במשפחה
    public enum FamilyCancer {
        NONE(0.0, "No known history"),
        DISTANT(0.4, "Distant relative"),
        SIBLING(0.7, "Sibling"),
        PARENT(0.9, "Parent"),
        MULTIPLE(1.0, "Multiple family members");

        private final double weight;
        private final String description;

        FamilyCancer(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }
    }

    // מייצג קטגוריות BMI ומשקלן כגורם סיכון
    public enum BMICategory {
        UNDERWEIGHT(0.2, "Underweight"),
        NORMAL(0.0, "Normal"),
        OVERWEIGHT(0.3, "Overweight"),
        OBESE_1(0.6, "Obese class 1"),
        OBESE_2(0.8, "Obese class 2"),
        OBESE_3(1.0, "Obese class 3");

        private final double weight;
        private final String description;

        BMICategory(double weight, String description) {
            this.weight = weight;
            this.description = description;
        }

        public double getWeight() { return weight; }
        public String getDescription() { return description; }

        // פונקציה שמחזירה קטגוריית BMI לפי ערך מספרי
        public static BMICategory fromBMI(double bmi) {
            if (bmi < 18.5) return UNDERWEIGHT;
            if (bmi < 25) return NORMAL;
            if (bmi < 30) return OVERWEIGHT;
            if (bmi < 35) return OBESE_1;
            if (bmi < 40) return OBESE_2;
            return OBESE_3;
        }
    }
}
