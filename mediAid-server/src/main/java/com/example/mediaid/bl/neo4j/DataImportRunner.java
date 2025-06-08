package com.example.mediaid.bl.neo4j;

import com.example.mediaid.bl.emergency.RiskFactorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * רכיב  המריץ את תהליך ייבוא הנתונים הרפואיים
 */
@Component
public class DataImportRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataImportRunner.class);

    private final UmlsEntityImporter entityImporter;
    private final UmlsRelationshipImporter relationshipImporter;
    private final RiskFactorSer riskFactorSer; // שונה מ-riskFactorService
    private final RiskFactorService riskFactorService; // שירות PostgreSQL
    private final Environment environment;

    @Autowired
    public DataImportRunner(
            UmlsEntityImporter entityImporter,
            UmlsRelationshipImporter relationshipImporter,
            RiskFactorSer riskFactorSer,
            RiskFactorService riskFactorService,
            Environment environment) {

        this.entityImporter = entityImporter;
        this.relationshipImporter = relationshipImporter;
        this.riskFactorSer = riskFactorSer;
        this.riskFactorService = riskFactorService;
        this.environment = environment;
    }

    /**
     * נקודת כניסה עיקרית לתהליך הייבוא המלא
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runDataImport() {
        boolean importEnabled = Boolean.parseBoolean(
                environment.getProperty("mediaid.data.import.enabled", "false"));

        boolean importEntities = Boolean.parseBoolean(
                environment.getProperty("mediaid.data.import.entities", "true"));

        boolean importRelationships = Boolean.parseBoolean(
                environment.getProperty("mediaid.data.import.relationships", "false"));

        boolean initializeRiskFactors = Boolean.parseBoolean(
                environment.getProperty("mediaid.data.initialize.risk-factors", "true"));

        if (importEnabled) {
            try {
                logger.info("=== Starting MediAid System Data Import with Risk Factors ===");

                // הצגת מצב Demo
                logger.info("Demo mode status: {}", DemoMode.getDemoStats());

                // שלב 1: ייבוא ישויות מ-PostgreSQL ל-Neo4j
                if (importEntities) {
                    logger.info("Starting entity import from PostgreSQL to Neo4j");

                    if (DemoMode.MODE) {
                        logger.info("Using demo-specific import method");
                        entityImporter.importDemoEntitiesFromDB();
                    } else {
                        logger.info("Using full import method");
                        entityImporter.importAllEntitiesFromDB();
                    }

                    // הצגת סטטיסטיקות ישויות
                    Map<String, Long> entityStats = entityImporter.getImportStatistics();
                    logger.info("Entity import statistics:");
                    entityStats.forEach((type, count) ->
                            logger.info("  {}: {} entities", type, count));
                } else {
                    logger.info("Entity import disabled");
                }

                // שלב 2: ייבוא קשרים ישירות מ-MRREL
                if (importRelationships) {
                    String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
                    if (mrrelPath != null && !mrrelPath.isEmpty()) {
                        logger.info("Starting direct relationship import from MRREL file");
                        logger.info("MRREL file path: {}", mrrelPath);
                        logger.info("Demo mode filter: {}",
                                DemoMode.MODE ? "ENABLED" : "DISABLED");

                        // ייבוא ישיר מ-MRREL ל- Neo4j
                        relationshipImporter.importRelationships(mrrelPath);

                    } else {
                        logger.warn("MRREL file path not configured - skipping relationship import");
                        logger.info("To enable relationship import, set mediaid.umls.mrrel.path in application.properties");
                    }
                } else {
                    logger.info("Relationship import disabled");
                }

                // שלב 3: אתחול גורמי סיכון מקיפים
                if (initializeRiskFactors) {
                    logger.info("Starting comprehensive risk factors initialization");
                    initializeComprehensiveRiskFactors();
                }

                logger.info("=== Data import completed successfully ===");
                printFinalSummaryWithRiskFactors();

            } catch (Exception e) {
                logger.error("Critical error in data import: {}", e.getMessage(), e);
                logger.error("Import process failed - system may not function properly");
            }
        } else {
            logger.info("Data import disabled");
            logger.info("To enable data import, set mediaid.data.import.enabled=true in application.properties");
            logger.info("Demo mode status: {}", DemoMode.getDemoStats());
        }
    }

    /**
     * אתחול מקיף של גורמי סיכון במערכת
     */
    private void initializeComprehensiveRiskFactors() {
        try {
            logger.info("Creating comprehensive risk factors in Neo4j");

            // גורמי סיכון דינמיים עיקריים
            createRiskFactor("AGE", 45, "Age-related health risks");
            createRiskFactor("BMI", 25, "Body Mass Index health indicator");
            createRiskFactor("BLOOD_PRESSURE_SYSTOLIC", 120, "Systolic blood pressure reading");
            createRiskFactor("BLOOD_GLUCOSE", 100, "Blood glucose level indicator");
            createRiskFactor("SMOKING_SCORE", 0, "Smoking habit severity score");

            // גורמי סיכון נוספים למצב מתקדם
            createRiskFactor("CHOLESTEROL_TOTAL", 200, "Total cholesterol level");
            createRiskFactor("HEART_RATE_RESTING", 70, "Resting heart rate");
            createRiskFactor("STRESS_LEVEL_SCORE", 3, "Psychological stress level");

            logger.info("Comprehensive risk factors initialization completed successfully");

        } catch (Exception e) {
            logger.error("Error in comprehensive risk factors initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * יצירת גורם סיכון בודד עם קשרים
     */
    private void createRiskFactor(String type, double value, String description) {
        try {
            long nodeId = riskFactorSer.createOrUpdateRiskFactor(type, value);
            riskFactorSer.updateRiskFactorRelationships(type, value);
            logger.debug("Created {} risk factor with ID: {} ({})", type, nodeId, description);
        } catch (Exception e) {
            logger.warn("Failed to create risk factor {}: {}", type, e.getMessage());
        }
    }

    /**
     * הצגת סיכום מערכת מפורט
     */
    private void printFinalSummaryWithRiskFactors() {
        try {
            logger.info("\n=== Comprehensive System Status Summary ===");

            // סטטיסטיקות ישויות
            Map<String, Long> entityStats = entityImporter.getImportStatistics();
            long totalEntities = entityStats.values().stream().mapToLong(Long::longValue).sum();
            logger.info("Total entities in graph: {}", totalEntities);


            // סטטיסטיקות גורמי סיכון
            try {
                Map<String, Object> riskFactorStats = riskFactorSer.getRiskFactorStatistics();
                logger.info("Risk Factor System Status:");
                logger.info("  Neo4j risk factors: {}", riskFactorStats.getOrDefault("total_risk_factors", 0));
                logger.info("  Risk relationships: {}", riskFactorStats.getOrDefault("total_risk_relationships", 0));

                @SuppressWarnings("unchecked")   //המרה לא בטוחה
                Map<String, Object> riskFactorsByType = (Map<String, Object>) riskFactorStats.get("risk_factors_by_type");
                if (riskFactorsByType != null && !riskFactorsByType.isEmpty()) {
                    logger.info("  Active risk factors:");
                    riskFactorsByType.forEach((type, value) ->
                            logger.info("    {}: {}", type, value));
                }
            } catch (Exception e) {
                logger.warn("Could not retrieve risk factor statistics: {}", e.getMessage());
            }

            if (totalEntities > 0) {
                logger.info("\nEntity breakdown:");
                entityStats.forEach((type, count) ->
                        logger.info("  {}: {}", String.format("%-20s", type), String.format("%,d", count)));
            }

        } catch (Exception e) {
            logger.error("Error generating comprehensive system summary: {}", e.getMessage(), e);
        }
    }


    /**
     * הפעלה ידנית של תהליך הייבוא המקיף
     */
    public void manualComprehensiveDataImport() {
        logger.info("Starting manual comprehensive import process");

        try {
            entityImporter.importAllEntitiesFromDB();

            String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
            if (mrrelPath != null && !mrrelPath.isEmpty()) {
                relationshipImporter.importRelationships(mrrelPath);
            }

            initializeComprehensiveRiskFactors();
            logger.info("Manual comprehensive import completed successfully");
        } catch (Exception e) {
            logger.error("Error in manual comprehensive import: {}", e.getMessage(), e);
        }
    }


}