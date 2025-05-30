package com.example.mediaid.bl.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

/**
 *טיפול בגורמי סיכון ומשקלים דינמיים
 */
@Service
public class RiskFactorSer {

    private final Driver neo4jDriver;

    // טבלת מיפוי עבור משקלי גורמי סיכון
    private static final Map<String, RiskFactorConfig> RISK_FACTOR_CONFIGS = new HashMap<>();
    static {
        // גיל
        RISK_FACTOR_CONFIGS.put("AGE", new RiskFactorConfig(
                30, 120,
                Map.of("C0154305", 1.5),  // מחלות לב
                Map.of("60", 1.2, "70", 1.5, "80", 2.0) // משקלים לפי ערכים
        ));

        // BMI
        RISK_FACTOR_CONFIGS.put("BMI", new RiskFactorConfig(
                15, 50,
                Map.of(
                        "C0011849", 1.5,  // סוכרת
                        "C0020538", 1.7,  // לחץ דם גבוה
                        "C0018802", 1.4   // מחלת לב
                ),
                Map.of("30", 1.3, "35", 1.8, "40", 2.5) // משקלים לפי ערכים
        ));

        // לחץ דם
        RISK_FACTOR_CONFIGS.put("BLOOD_PRESSURE_SYSTOLIC", new RiskFactorConfig(
                90, 210,
                Map.of(
                        "C0018802", 1.5,  // מחלת לב
                        "C0022658", 1.6   // מחלות כליה
                ),
                Map.of("140", 1.2, "160", 1.7, "180", 2.3) // משקלים לפי ערכים
        ));

        // רמת גלוקוז בדם
        RISK_FACTOR_CONFIGS.put("BLOOD_GLUCOSE", new RiskFactorConfig(
                70, 300,
                Map.of(
                        "C0011849", 2.0,  // סוכרת
                        "C0018802", 1.3   // מחלת לב
                ),
                Map.of("126", 1.3, "180", 1.7, "250", 2.2) // משקלים לפי ערכים
        ));
    }

    @Autowired
    public RiskFactorSer(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    /**
     * יצירה או עדכון של פרמטר סיכון
     * @param type - סוג המדד (למשל "AGE", "BMI")
     * @param value - הערך המספרי
     * @return מזהה הצומת שנוצר
     */
    public long createOrUpdateRiskFactor(String type, double value) {
        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                var result = tx.run(
                        "MERGE (rf:RiskFactorSer {type: $type}) " +
                                "SET rf.value = $value " +
                                "RETURN id(rf) as nodeId",
                        Values.parameters("type", type, "value", value)
                );

                if (result.hasNext()) {
                    return result.next().get("nodeId").asLong();
                }

                return -1L;
            });
        }
    }

    /**
     * עדכון הקשרים והמשקלים המושפעים מגורם סיכון
     * @param type - סוג המדד
     * @param value - הערך המספרי
     */
    public void updateRiskFactorRelationships(String type, double value) {
        if (!RISK_FACTOR_CONFIGS.containsKey(type)) {
            System.out.println("סוג גורם סיכון לא מוגדר: " + type);
            return;
        }

        RiskFactorConfig config = RISK_FACTOR_CONFIGS.get(type);

        // בדיקה אם הערך בטווח
        if (value < config.minValue || value > config.maxValue) {
            System.out.println("ערך מחוץ לטווח עבור " + type + ": " + value);
            return;
        }

        // מציאת המשקל בהתאם לערך
        double factorWeight = calculateWeightForValue(config, value);

        try (Session session = neo4jDriver.session()) {
            // עדכון הקשרים למחלות מושפעות
            for (Map.Entry<String, Double> entry : config.diseaseCuiToWeight.entrySet()) {
                String diseaseCui = entry.getKey();
                double diseaseSpecificWeight = entry.getValue() * factorWeight;

                // יצירת או עדכון הקשר לגורם סיכון
                session.writeTransaction(tx -> {
                    tx.run(
                            "MATCH (rf:RiskFactorSer {type: $type}), (d:Disease {cui: $cui}) " +
                                    "MERGE (rf)-[r:INCREASES_RISK_OF]->(d) " +
                                    "SET r.weight = $weight",
                            Values.parameters(
                                    "type", type,
                                    "cui", diseaseCui,
                                    "weight", Math.min(0.99, diseaseSpecificWeight)
                            )
                    );
                    return null;
                });
            }

            System.out.println("עודכנו קשרי גורם סיכון עבור " + type + " עם ערך " + value);
        }
    }

    /**
     * חישוב משקל הסיכון לפי הערך הנוכחי
     */
    private double calculateWeightForValue(RiskFactorConfig config, double value) {
        // מציאת הסף הקרוב ביותר
        double closestThreshold = 0;
        double thresholdWeight = 1.0;

        for (Map.Entry<String, Double> threshold : config.valueThresholds.entrySet()) {
            double thresholdValue = Double.parseDouble(threshold.getKey());
            if (value >= thresholdValue &&
                    (closestThreshold == 0 || thresholdValue > closestThreshold)) {
                closestThreshold = thresholdValue;
                thresholdWeight = threshold.getValue();
            }
        }

        return thresholdWeight;
    }

    /**
     * מחלקה פנימית להגדרת קונפיגורציה של גורם סיכון
     */
    private static class RiskFactorConfig {
        double minValue;
        double maxValue;
        Map<String, Double> diseaseCuiToWeight;  // מיפוי CUI של מחלה למשקל
        Map<String, Double> valueThresholds;     // ספי ערכים למשקלים

        public RiskFactorConfig(
                double minValue,
                double maxValue,
                Map<String, Double> diseaseCuiToWeight,
                Map<String, Double> valueThresholds) {

            this.minValue = minValue;
            this.maxValue = maxValue;
            this.diseaseCuiToWeight = diseaseCuiToWeight;
            this.valueThresholds = valueThresholds;
        }
    }
}