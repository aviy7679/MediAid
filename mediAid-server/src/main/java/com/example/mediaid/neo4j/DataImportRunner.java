package com.example.mediaid.neo4j;

import com.example.mediaid.bl.DemoMode;
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
    private final RiskFactorSer riskFactorSer;
    private final Environment environment;

    @Autowired
    public DataImportRunner(
            UmlsEntityImporter entityImporter,
            UmlsRelationshipImporter relationshipImporter,
            RiskFactorSer riskFactorSer,
            Environment environment) {

        this.entityImporter = entityImporter;
        this.relationshipImporter = relationshipImporter;
        this.riskFactorSer = riskFactorSer;
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
     * הצגת סיכום מערכת מפורט
     */
    private void printFinalSummary() {
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

}