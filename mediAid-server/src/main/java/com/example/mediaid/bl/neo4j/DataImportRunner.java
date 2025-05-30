package com.example.mediaid.bl.neo4j;

import com.example.mediaid.bl.RelationshipProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * מחלקה המריצה את תהליך ייבוא הנתונים
 */
@Component
public class DataImportRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataImportRunner.class);

    private final UmlsEntityImporter entityImporter;
    private final UmlsRelationshipImporter relationshipImporter;
    private final RiskFactorSer riskFactorService;
    private final Environment environment;

    @Autowired
    private RelationshipProcessor relationshipProcessor;

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

//    @EventListener(ApplicationReadyEvent.class)
//    public void runDataImport() {
//        // בדיקה אם לבצע ייבוא נתונים (ניתן להגדיר בקובץ הקונפיגורציה)
//        boolean importEnabled = Boolean.parseBoolean(
//                environment.getProperty("mediaid.data.import.enabled", "false"));
//
//        boolean importEntities = Boolean.parseBoolean(
//                environment.getProperty("mediaid.data.import.entities", "true"));
//
//        boolean importRelationships = Boolean.parseBoolean(
//                environment.getProperty("mediaid.data.import.relationships", "false"));
//
//        boolean initializeRiskFactors = Boolean.parseBoolean(
//                environment.getProperty("mediaid.data.initialize.risk-factors", "true"));
//
//        if (importEnabled) {
//            try {
//                logger.info("=== Starting MedicalAid System Data Import ===");
//
//                if (importEntities) {
//                    logger.info("Starting entity import from PostgreSQL to Neo4j...");
//
//                    // ייבוא ישויות מ-PostgreSQL
//                    entityImporter.importAllEntitiesFromDB();
//
//                    // הדפסת סטטיסטיקות
//                    Map<String, Long> stats = entityImporter.getImportStatistics();
//                    logger.info("Entity import statistics:");
//                    stats.forEach((type, count) ->
//                            logger.info("  {}: {} entities", type, count));
//                } else {
//                    logger.info("Entity import disabled");
//                }
//
//                if (importRelationships) {
//                    String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
//                    if (mrrelPath != null && !mrrelPath.isEmpty()) {
//                        logger.info("Starting relationship import from UMLS...");
//                        // ייבוא קשרים - עדיין מקובץ MRREL כי אין לנו טבלת קשרים ב-PostgreSQL
//                        relationshipImporter.importRelationships(mrrelPath);
//
//                        // בדיקת תוצאות ייבוא קשרים
//                        Map<String, Object> relStats = relationshipImporter.validateImportedRelationships();
//                        logger.info("Relationship import statistics:");
//                        relStats.forEach((type, count) ->
//                                logger.info("  {}: {} relationships", type, count));
//                    } else {
//                        logger.info("MRREL file path not configured - skipping relationship import");
//                    }
//                } else {
//                    logger.info("Relationship import disabled");
//                }
//
//                if (initializeRiskFactors) {
//                    logger.info("Starting basic risk factors initialization...");
//                    initializeBasicRiskFactors();
//                }
//
//                logger.info("=== Data import completed successfully! ===");
//                printFinalSummary();
//
//            } catch (Exception e) {
//                logger.error("Error in data import: {}", e.getMessage(), e);
//            }
//        } else {
//            logger.info("Data import disabled. To enable, set mediaid.data.import.enabled=true");
//        }
//    }
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
            logger.info("=== Starting MedicalAid System Data Import ===");

            if (importEntities) {
                logger.info("Starting entity import from PostgreSQL to Neo4j...");
                entityImporter.importAllEntitiesFromDB();

                Map<String, Long> stats = entityImporter.getImportStatistics();
                logger.info("Entity import statistics:");
                stats.forEach((type, count) ->
                        logger.info("  {}: {} entities", type, count));
            } else {
                logger.info("Entity import disabled");
            }

            if (importRelationships) {
                String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
                if (mrrelPath != null && !mrrelPath.isEmpty()) {
                    logger.info("Processing relationships from MRREL to PostgreSQL...");
                    relationshipProcessor.processAndSaveRelationships(mrrelPath);

                    logger.info("Starting relationship import from PostgreSQL to Neo4j...");
                    entityImporter.importRelationshipsFromDB();

                    Map<String, Long> relStats = entityImporter.getRelationshipStatistics();
                    logger.info("Relationship import statistics:");
                    relStats.forEach((type, count) ->
                            logger.info("  {}: {} relationships", type, count));
                } else {
                    logger.info("MRREL file path not configured - skipping relationship import");
                }
            } else {
                logger.info("Relationship import disabled");
            }

            if (initializeRiskFactors) {
                logger.info("Starting basic risk factors initialization...");
                initializeBasicRiskFactors();
            }

            logger.info("=== Data import completed successfully! ===");
            printFinalSummary();

        } catch (Exception e) {
            logger.error("Error in data import: {}", e.getMessage(), e);
        }
    } else {
        logger.info("Data import disabled. To enable, set mediaid.data.import.enabled=true");
    }
}

    private void printFinalSummary() {
        try {
            logger.info("\n=== System Status Summary ===");

            Map<String, Long> entityStats = entityImporter.getImportStatistics();
            long totalEntities = entityStats.values().stream().mapToLong(Long::longValue).sum();
            logger.info("Total entities in graph: {}", totalEntities);

            Map<String, Long> relationshipStats = entityImporter.getRelationshipStatistics();
            long totalRelationships = relationshipStats.getOrDefault("TOTAL_RELATIONSHIPS", 0L);
            logger.info("Total relationships in graph: {}", totalRelationships);

            logger.info("\nEntity breakdown:");
            entityStats.forEach((type, count) ->
                    logger.info("  {}: {}", String.format("%-15s", type), String.format("%,d", count)));

            if (totalRelationships > 0) {
                logger.info("\nRelationship breakdown:");
                relationshipStats.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals("TOTAL_RELATIONSHIPS"))
                        .forEach(entry -> logger.info("  {}: {}",
                                String.format("%-25s", entry.getKey()),
                                String.format("%,d", entry.getValue())));
            }

            logger.info("\n{}", "=".repeat(40));
            logger.info("System is ready for use!");
            logger.info("API can be accessed at: http://localhost:8080/");
            logger.info("{}", "=".repeat(40));

        } catch (Exception e) {
            logger.error("Error printing summary: {}", e.getMessage(), e);
        }
    }
    /**
     * אתחול גורמי סיכון בסיסיים
     */
    private void initializeBasicRiskFactors() {
        try {
            // יצירת גורמי סיכון ברירת מחדל עם ערכים התחלתיים
            logger.info("Creating basic risk factors...");

            // גיל - ערך בסיסי 40
            long ageNodeId = riskFactorService.createOrUpdateRiskFactor("AGE", 40);
            riskFactorService.updateRiskFactorRelationships("AGE", 40);
            logger.info("Created AGE risk factor with ID: {}", ageNodeId);

            // BMI - ערך בסיסי 25
            long bmiNodeId = riskFactorService.createOrUpdateRiskFactor("BMI", 25);
            riskFactorService.updateRiskFactorRelationships("BMI", 25);
            logger.info("Created BMI risk factor with ID: {}", bmiNodeId);

            // לחץ דם - ערך בסיסי 120
            long bpNodeId = riskFactorService.createOrUpdateRiskFactor("BLOOD_PRESSURE_SYSTOLIC", 120);
            riskFactorService.updateRiskFactorRelationships("BLOOD_PRESSURE_SYSTOLIC", 120);
            logger.info("Created BLOOD_PRESSURE_SYSTOLIC risk factor with ID: {}", bpNodeId);

            // רמת גלוקוז - ערך בסיסי 100
            long glucoseNodeId = riskFactorService.createOrUpdateRiskFactor("BLOOD_GLUCOSE", 100);
            riskFactorService.updateRiskFactorRelationships("BLOOD_GLUCOSE", 100);
            logger.info("Created BLOOD_GLUCOSE risk factor with ID: {}", glucoseNodeId);

            logger.info("Risk factors initialization completed successfully");

        } catch (Exception e) {
            logger.error("Error in risk factors initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * הדפסת סיכום סופי של המערכת
     */
//    private void printFinalSummary() {
//        try {
//            logger.info("\n=== System Status Summary ===");
//
//            // סטטיסטיקות ישויות
//            Map<String, Long> entityStats = entityImporter.getImportStatistics();
//            long totalEntities = entityStats.values().stream().mapToLong(Long::longValue).sum();
//            logger.info("Total entities in graph: {}", totalEntities);
//
//            // סטטיסטיקות קשרים
//            Map<String, Object> relationshipStats = relationshipImporter.validateImportedRelationships();
//            long totalRelationships = relationshipStats.values().stream()
//                    .filter(v -> v instanceof Number)
//                    .mapToLong(v -> ((Number) v).longValue())
//                    .sum();
//            logger.info("Total relationships in graph: {}", totalRelationships);
//
//            // הצגת פירוט
//            logger.info("\nEntity breakdown:");
//            entityStats.forEach((type, count) ->
//                    logger.info("  {}: {}", String.format("%-15s", type), String.format("%,d", count)));
//
//            if (totalRelationships > 0) {
//                logger.info("\nRelationship breakdown:");
//                relationshipStats.forEach((type, count) -> {
//                    if (count instanceof Number) {
//                        logger.info("  {}: {}", String.format("%-25s", type), String.format("%,d", ((Number) count).longValue()));
//                    }
//                });
//            }
//
//            logger.info("\n{}", "=".repeat(40));
//            logger.info("System is ready for use!");
//            logger.info("API can be accessed at: http://localhost:8080/mediaid/api/medical/");
//            logger.info("{}", "=".repeat(40));
//
//        } catch (Exception e) {
//            logger.error("Error printing summary: {}", e.getMessage(), e);
//        }
//    }

    /**
     * הפעלה ידנית של ייבוא נתונים - לשימוש מControllers או מקומות אחרים
     */
    public void manualDataImport() {
        logger.info("Starting manual import...");

        try {
            // ייבוא ישויות
            entityImporter.importAllEntitiesFromDB();

            // אתחול גורמי סיכון
            initializeBasicRiskFactors();

            logger.info("Manual import completed successfully");

        } catch (Exception e) {
            logger.error("Error in manual import: {}", e.getMessage(), e);
        }
    }
    

    /**
     * קבלת מצב המערכת
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new java.util.HashMap<>();

        try {
            // סטטיסטיקות ישויות
            Map<String, Long> entityStats = entityImporter.getImportStatistics();
            status.put("entities", entityStats);

            // סטטיסטיקות קשרים
            Map<String, Object> relationshipStats = relationshipImporter.validateImportedRelationships();
            status.put("relationships", relationshipStats);

            // מצב כללי
            long totalEntities = entityStats.values().stream().mapToLong(Long::longValue).sum();
            status.put("total_entities", totalEntities);

            long totalRelationships = relationshipStats.values().stream()
                    .filter(v -> v instanceof Number)
                    .mapToLong(v -> ((Number) v).longValue())
                    .sum();
            status.put("total_relationships", totalRelationships);

            status.put("status", "healthy");
            status.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            status.put("status", "error");
            status.put("error", e.getMessage());
            status.put("timestamp", System.currentTimeMillis());
        }

        return status;
    }
}