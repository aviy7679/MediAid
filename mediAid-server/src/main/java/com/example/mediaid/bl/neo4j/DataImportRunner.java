package com.example.mediaid.bl.neo4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * רכיב המריץ את תהליך ייבוא הנתונים הרפואיים
 */
@Component
public class DataImportRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataImportRunner.class);

    private final UmlsEntityImporter entityImporter;
    private final UmlsRelationshipImporter relationshipImporter;
    private final RiskFactorSer riskFactorService;
    private final Environment environment;

    @Autowired
    public DataImportRunner(
            UmlsEntityImporter entityImporter,
            UmlsRelationshipImporter relationshipImporter,
            RiskFactorSer riskFactorService,
            Environment environment) {

        this.entityImporter = entityImporter;
        this.relationshipImporter = relationshipImporter;
        this.riskFactorService = riskFactorService;
        this.environment = environment;
    }

    /**
     * נקודת כניסה עיקרית לתהליך הייבוא
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runDataImport() {
        // קריאת הגדרות התצורה - Reading configuration settings
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
                logger.info("=== Starting MedicalAid System Data Import ===");

                // הצגת מצב Demo - Display Demo mode status
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

                        // הצגת סטטיסטיקות קשרים
                        Map<String, Long> relStats = entityImporter.getRelationshipStatistics();
                        logger.info("Relationship import statistics:");
                        relStats.forEach((type, count) ->
                                logger.info("  {}: {} relationships", type, count));
                    } else {
                        logger.warn("MRREL file path not configured - skipping relationship import");
                        logger.info("To enable relationship import, set mediaid.umls.mrrel.path in application.properties");
                    }
                } else {
                    logger.info("Relationship import disabled");
                }

                // שלב 3: אתחול גורמי סיכון בסיסיים
                if (initializeRiskFactors) {
                    logger.info("Starting basic risk factors initialization");
                    initializeBasicRiskFactors();
                }

                logger.info("=== Data import completed successfully ===");
                printFinalSummary();

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
     * אתחול גורמי סיכון בסיסיים במערכת
     */
    private void initializeBasicRiskFactors() {
        try {
            logger.info("Creating basic risk factors in Neo4j");

            // יצירת גורמי סיכון עיקריים
            createRiskFactor("AGE", 40, "Age risk factor");
            createRiskFactor("BMI", 25, "Body Mass Index risk factor");
            createRiskFactor("BLOOD_PRESSURE_SYSTOLIC", 120, "Systolic blood pressure risk factor");
            createRiskFactor("BLOOD_GLUCOSE", 100, "Blood glucose level risk factor");

            logger.info("Risk factors initialization completed successfully");

        } catch (Exception e) {
            logger.error("Error in risk factors initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * יצירת גורם סיכון בודד
     */
    private void createRiskFactor(String type, double value, String description) {
        try {
            long nodeId = riskFactorService.createOrUpdateRiskFactor(type, value);
            riskFactorService.updateRiskFactorRelationships(type, value);
            logger.debug("Created {} risk factor with ID: {} ({})", type, nodeId, description);
        } catch (Exception e) {
            logger.warn("Failed to create risk factor {}: {}", type, e.getMessage());
        }
    }

    /**
     * הצגת סיכום מערכת מפורט
     */
    private void printFinalSummary() {
        try {
            logger.info("\n=== System Status Summary ===");

            // סטטיסטיקות ישויות - Entity statistics
            Map<String, Long> entityStats = entityImporter.getImportStatistics();
            long totalEntities = entityStats.values().stream().mapToLong(Long::longValue).sum();
            logger.info("Total entities in graph: {}", totalEntities);

            // סטטיסטיקות קשרים - Relationship statistics
            Map<String, Long> relationshipStats = entityImporter.getRelationshipStatistics();
            long totalRelationships = relationshipStats.getOrDefault("TOTAL_RELATIONSHIPS", 0L);
            logger.info("Total relationships in graph: {}", totalRelationships);

            if (totalEntities > 0) {
                logger.info("\nEntity breakdown:");
                entityStats.forEach((type, count) ->
                        logger.info("  {}: {}", String.format("%-20s", type), String.format("%,d", count)));
            }

            if (totalRelationships > 0) {
                logger.info("\nRelationship breakdown:");
                relationshipStats.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals("TOTAL_RELATIONSHIPS"))
                        .limit(10) // הצגת 10 סוגי קשרים מובילים - Show top 10 relationship types
                        .forEach(entry -> logger.info("  {}: {}",
                                String.format("%-25s", entry.getKey()),
                                String.format("%,d", entry.getValue())));

                if (relationshipStats.size() > 11) { // יותר מ-10 + TOTAL
                    logger.info("  ... and {} more relationship types", relationshipStats.size() - 11);
                }
            }

            // בדיקת תקינות המערכת
            String healthStatus = determineSystemHealth(totalEntities, totalRelationships);
            logger.info("\nSystem health: {}", healthStatus);

            logger.info("\n{}", "=".repeat(50));
            logger.info("MedicalAid system is ready for use");
            logger.info("API endpoints available at: http://localhost:8080/");
            logger.info("{}", "=".repeat(50));

        } catch (Exception e) {
            logger.error("Error generating system summary: {}", e.getMessage(), e);
        }
    }

    /**
     * קביעת מצב בריאות המערכת
     */
    private String determineSystemHealth(long totalEntities, long totalRelationships) {
        if (totalEntities == 0) {
            return "WARNING - No entities found in graph";
        } else if (totalRelationships == 0) {
            return "WARNING - No relationships found in graph";
        } else if (totalEntities < 1000) {
            return "LIMITED - Basic entities available";
        } else if (totalRelationships < 1000) {
            return "LIMITED - Entities available, minimal relationships";
        } else {
            return "HEALTHY - Full system operational";
        }
    }

    /**
     * הפעלה ידנית של תהליך הייבוא
     */
    public void manualDataImport() {
        logger.info("Starting manual import process");

        try {
            entityImporter.importAllEntitiesFromDB();

            String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
            if (mrrelPath != null && !mrrelPath.isEmpty()) {
                relationshipImporter.importRelationships(mrrelPath);
            }

            initializeBasicRiskFactors();
            logger.info("Manual import completed successfully");
        } catch (Exception e) {
            logger.error("Error in manual import: {}", e.getMessage(), e);
        }
    }

    /**
     * קבלת מצב המערכת הנוכחי
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new java.util.HashMap<>();

        try {
            Map<String, Long> entityStats = entityImporter.getImportStatistics();
            status.put("entities", entityStats);

            Map<String, Long> relationshipStats = entityImporter.getRelationshipStatistics();
            status.put("relationships", relationshipStats);

            long totalEntities = entityStats.values().stream().mapToLong(Long::longValue).sum();
            status.put("total_entities", totalEntities);

            long totalRelationships = relationshipStats.values().stream().mapToLong(Long::longValue).sum();
            status.put("total_relationships", totalRelationships);

            status.put("health_status", determineSystemHealth(totalEntities, totalRelationships));
            status.put("demo_mode", DemoMode.MODE);
            status.put("status", "operational");
            status.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            status.put("status", "error");
            status.put("error", e.getMessage());
            status.put("timestamp", System.currentTimeMillis());
        }

        return status;
    }
}