package com.example.mediaid.bl.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

/**
 * שירות לטיפול בגורמי סיכון דינמיים ב-Neo4j
 */
@Service
public class RiskFactorSer {

    private static final Logger logger = LoggerFactory.getLogger(RiskFactorSer.class);
    private final Driver neo4jDriver;

    // טבלת גורמי סיכון
    private static final Map<String, RiskFactorConfig> RISK_FACTOR_CONFIGS = new HashMap<>();
    static {
        // גיל
        RISK_FACTOR_CONFIGS.put("AGE", new RiskFactorConfig(
                18, 120,
                Map.of(
                        "C4013416", 1.5,  // Diabetes mellitus
                        "C4014808", 1.7,  // Hypertensive disease
                        "C4227568", 1.8,  // Heart failure
                        "C0002962", 1.6,  // Angina pectoris
                        "C3280935", 2.0,  // Myocardial infarction
                        "C0002395", 2.2   // Alzheimer's disease
                ),
                Map.of("40", 1.1, "50", 1.3, "60", 1.6, "70", 1.9, "80", 2.2)
        ));

        // BMI
        RISK_FACTOR_CONFIGS.put("BMI", new RiskFactorConfig(
                15, 50,
                Map.of(
                        "C4013416", 1.8,  // Diabetes mellitus
                        "C4014808", 1.6,  // Hypertensive disease
                        "C4227568", 1.5,  // Heart failure
                        "C0002962", 1.4,  // Angina pectoris
                        "C3280935", 1.7   // Myocardial infarction
                ),
                Map.of("25", 1.0, "30", 1.4, "35", 1.8, "40", 2.2)
        ));

        // לחץ דם סיסטולי
        RISK_FACTOR_CONFIGS.put("BLOOD_PRESSURE_SYSTOLIC", new RiskFactorConfig(
                90, 220,
                Map.of(
                        "C4227568", 1.8,  // Heart failure
                        "C0002962", 1.6,  // Angina pectoris
                        "C3280935", 2.0,  // Myocardial infarction
                        "C3554760", 1.7,  // Stroke
                        "C1561643", 1.5   // Chronic kidney disease
                ),
                Map.of("130", 1.1, "140", 1.4, "160", 1.8, "180", 2.2, "200", 2.5)
        ));

        // רמת גלוקוז בדם
        RISK_FACTOR_CONFIGS.put("BLOOD_GLUCOSE", new RiskFactorConfig(
                70, 350,
                Map.of(
                        "C4013416", 2.0,  // Diabetes mellitus
                        "C4227568", 1.4,  // Heart failure
                        "C0002962", 1.3,  // Angina pectoris
                        "C3280935", 1.5,  // Myocardial infarction
                        "C1561643", 1.6   // Chronic kidney disease
                ),
                Map.of("100", 1.0, "126", 1.3, "150", 1.6, "200", 1.9, "300", 2.3)
        ));

        // עישון (מטופל כערך מספרי)
        RISK_FACTOR_CONFIGS.put("SMOKING_SCORE", new RiskFactorConfig(
                0, 10,
                Map.of(
                        "C0024117", 2.5,  // COPD
                        "C3280935", 2.0,  // Myocardial infarction
                        "C0002962", 1.8,  // Angina pectoris
                        "C4227568", 1.6,  // Heart failure
                        "C0004096", 1.9   // Asthma
                ),
                Map.of("3", 1.2, "5", 1.6, "7", 2.0, "9", 2.4)
        ));
    }

    @Autowired
    public RiskFactorSer(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    /**
     * יצירה או עדכון של פרמטר סיכון
     */
    public long createOrUpdateRiskFactor(String type, double value) {
        logger.debug("Creating/updating risk factor: {} with value: {}", type, value);

        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                var result = tx.run(
                        "MERGE (rf:RiskFactor {type: $type}) " +
                                "SET rf.value = $value, rf.updated_at = datetime() " +
                                "RETURN id(rf) as nodeId",
                        Values.parameters("type", type, "value", value)
                );

                if (result.hasNext()) {
                    long nodeId = result.next().get("nodeId").asLong();
                    logger.debug("Risk factor {} updated with ID: {}", type, nodeId);
                    return nodeId;
                }

                return -1L;
            });
        } catch (Exception e) {
            logger.error("Error creating/updating risk factor {}: {}", type, e.getMessage());
            return -1L;
        }
    }

    /**
     * עדכון קשרים והמשקלים המושפעים מגורם סיכון
     */
    public void updateRiskFactorRelationships(String type, double value) {
        logger.debug("Updating relationships for risk factor: {} with value: {}", type, value);

        if (!RISK_FACTOR_CONFIGS.containsKey(type)) {
            logger.warn("Risk factor type not configured: {}", type);
            return;
        }

        RiskFactorConfig config = RISK_FACTOR_CONFIGS.get(type);

        // בדיקה אם הערך בטווח
        if (value < config.minValue || value > config.maxValue) {
            logger.warn("Value {} out of range for {}: expected {}-{}",
                    value, type, config.minValue, config.maxValue);
            return;
        }

        // חישוב משקל הסיכון
        double factorWeight = calculateWeightForValue(config, value);
        logger.debug("Calculated factor weight: {} for value: {}", factorWeight, value);

        try (Session session = neo4jDriver.session()) {
            // עדכון קשרים למחלות מושפעות
            for (Map.Entry<String, Double> entry : config.diseaseCuiToWeight.entrySet()) {
                String diseaseCui = entry.getKey();
                double diseaseSpecificWeight = entry.getValue() * factorWeight;

                session.writeTransaction(tx -> {
                    // מחיקת קשרים קיימים
                    tx.run(
                            "MATCH (rf:RiskFactor {type: $type})-[r:INCREASES_RISK_OF]->(d {cui: $cui}) " +
                                    "DELETE r",
                            Values.parameters("type", type, "cui", diseaseCui)
                    );

                    // יצירת קשר חדש עם משקל מעודכן
                    var result = tx.run(
                            "MATCH (rf:RiskFactor {type: $type}) " +
                                    "MATCH (d {cui: $cui}) " +
                                    "CREATE (rf)-[r:INCREASES_RISK_OF {" +
                                    "weight: $weight, " +
                                    "risk_level: $riskLevel, " +
                                    "source: 'user_profile', " +
                                    "created_at: datetime()}]->(d) " +
                                    "RETURN count(r) as created",
                            Values.parameters(
                                    "type", type,
                                    "cui", diseaseCui,
                                    "weight", Math.min(0.99, diseaseSpecificWeight),
                                    "riskLevel", categorizeRiskLevel(diseaseSpecificWeight)
                            )
                    );

                    if (result.hasNext()) {
                        long created = result.next().get("created").asLong();
                        if (created > 0) {
                            logger.debug("Created risk relationship: {} -> {} (weight: {})",
                                    type, diseaseCui, diseaseSpecificWeight);
                        }
                    }

                    return null;
                });
            }

            logger.info("Updated risk factor relationships for {} with value {}", type, value);

        } catch (Exception e) {
            logger.error("Error updating relationships for risk factor {}: {}", type, e.getMessage());
        }
    }

    /**
     * מחיקת גורם סיכון וכל הקשרים שלו
     */
    public void deleteRiskFactor(String type) {
        logger.info("Deleting risk factor: {}", type);

        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                // מחיקת כל הקשרים תחילה
                tx.run(
                        "MATCH (rf:RiskFactor {type: $type})-[r]-() DELETE r",
                        Values.parameters("type", type)
                );

                // מחיקת הצומת
                var result = tx.run(
                        "MATCH (rf:RiskFactor {type: $type}) DELETE rf RETURN count(rf) as deleted",
                        Values.parameters("type", type)
                );

                if (result.hasNext()) {
                    long deleted = result.next().get("deleted").asLong();
                    logger.info("Deleted {} risk factor nodes of type: {}", deleted, type);
                }

                return null;
            });

        } catch (Exception e) {
            logger.error("Error deleting risk factor {}: {}", type, e.getMessage());
        }
    }

    /**
     * קבלת סטטיסטיקות על גורמי הסיכון
     */
    public Map<String, Object> getRiskFactorStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                // ספירת גורמי סיכון
                var countResult = tx.run("MATCH (rf:RiskFactor) RETURN count(rf) as total");
                if (countResult.hasNext()) {
                    stats.put("total_risk_factors", countResult.next().get("total").asLong());
                }

                // ספירת קשרי סיכון
                var relResult = tx.run(
                        "MATCH ()-[r:INCREASES_RISK_OF]->() RETURN count(r) as total"
                );
                if (relResult.hasNext()) {
                    stats.put("total_risk_relationships", relResult.next().get("total").asLong());
                }

                // גורמי סיכון לפי סוג
                var typeResult = tx.run(
                        "MATCH (rf:RiskFactor) " +
                                "RETURN rf.type as type, rf.value as value " +
                                "ORDER BY rf.type"
                );

                Map<String, Object> riskFactorsByType = new HashMap<>();
                typeResult.forEachRemaining(record -> {
                    String type = record.get("type").asString();
                    double value = record.get("value").asDouble();
                    riskFactorsByType.put(type, value);
                });

                stats.put("risk_factors_by_type", riskFactorsByType);
                return null;
            });

        } catch (Exception e) {
            logger.error("Error getting risk factor statistics: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }

        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    /**
     * חישוב משקל הסיכון לפי הערך הנוכחי
     */
    private double calculateWeightForValue(RiskFactorConfig config, double value) {
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
     * קטגוריזציה של רמת סיכון
     */
    private String categorizeRiskLevel(double weight) {
        if (weight < 1.2) return "low";
        if (weight < 1.6) return "medium";
        if (weight < 2.0) return "high";
        return "very_high";
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

    /**
     * יצירת קשרי סיכון עבור משתמש ספציפי
     */
    public void createUserRiskFactorRelationships(String userId, Map<String, Double> userRiskFactors) {
        logger.info("Creating user-specific risk factor relationships for user: {}", userId);

        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                // יצירת צומת משתמש אם לא קיים
                tx.run(
                        "MERGE (u:User {userId: $userId}) " +
                                "SET u.updated_at = datetime()",
                        Values.parameters("userId", userId)
                );

                // יצירת קשרים לגורמי סיכון
                for (Map.Entry<String, Double> entry : userRiskFactors.entrySet()) {
                    String riskType = entry.getKey();
                    double value = entry.getValue();

                    // מחיקת קשרים קיימים
                    tx.run(
                            "MATCH (u:User {userId: $userId})-[r:HAS_RISK_FACTOR]->(rf:RiskFactor {type: $type}) " +
                                    "DELETE r",
                            Values.parameters("userId", userId, "type", riskType)
                    );

                    // יצירת קשר חדש
                    tx.run(
                            "MATCH (u:User {userId: $userId}) " +
                                    "MATCH (rf:RiskFactor {type: $type}) " +
                                    "CREATE (u)-[r:HAS_RISK_FACTOR {" +
                                    "value: $value, " +
                                    "created_at: datetime()}]->(rf)",
                            Values.parameters("userId", userId, "type", riskType, "value", value)
                    );
                }

                return null;
            });

            logger.info("Created {} risk factor relationships for user: {}",
                    userRiskFactors.size(), userId);

        } catch (Exception e) {
            logger.error("Error creating user risk factor relationships for {}: {}", userId, e.getMessage());
        }
    }
}