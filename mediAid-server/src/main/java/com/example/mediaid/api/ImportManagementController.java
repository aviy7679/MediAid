//package com.example.mediaid.api;
//
//import com.example.mediaid.bl.neo4j.UmlsEntityImporter;
//import com.example.mediaid.bl.neo4j.UmlsRelationshipImporter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.env.Environment;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/admin/import")
//public class ImportManagementController {
//
//    private static final Logger logger = LoggerFactory.getLogger(ImportManagementController.class);
//
//    @Autowired
//    private UmlsRelationshipImporter relationshipImporter;
//
//    @Autowired
//    private UmlsEntityImporter entityImporter;
//
//    @Autowired
//    private Environment environment;
//
//    /**
//     * ניתוח מצב הגרף הנוכחי
//     */
//    @GetMapping("/analyze")
//    public ResponseEntity<Map<String, Object>> analyzeGraph() {
//        logger.info("🔍 API call to analyze graph");
//
//        try {
//            Map<String, Object> analysis = relationshipImporter.analyzeCurrentGraph();
//            analysis.put("status", "success");
//            analysis.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(analysis);
//
//        } catch (Exception e) {
//            logger.error("Error analyzing graph", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * ניקוי לולאות עצמיות בלבד
//     */
//    @PostMapping("/clean/self-loops")
//    public ResponseEntity<Map<String, Object>> cleanSelfLoops() {
//        logger.info("🧹 API call to clean self-loops");
//
//        try {
//            int removed = relationshipImporter.cleanSelfLoops();
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("status", "success");
//            result.put("self_loops_removed", removed);
//            result.put("message", "Successfully removed " + removed + " self-loops");
//            result.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            logger.error("Error cleaning self-loops", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * ניקוי מלא של הגרף
//     */
//    @PostMapping("/clean/full")
//    public ResponseEntity<Map<String, Object>> performFullCleanup() {
//        logger.info("🧹 API call to perform full cleanup");
//
//        try {
//            Map<String, Object> results = relationshipImporter.performFullGraphCleanup();
//            results.put("status", "success");
//            results.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(results);
//
//        } catch (Exception e) {
//            logger.error("Error performing full cleanup", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * התחלת ייבוא מחדש עם הסינון החדש (ישירות לNeo4j - שיטה ישנה)
//     */
//    @PostMapping("/restart-optimized")
//    public ResponseEntity<Map<String, Object>> restartOptimizedImport() {
//        logger.info("🚀 API call to restart optimized import");
//
//        try {
//            String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
//            if (mrrelPath == null || mrrelPath.isEmpty()) {
//                Map<String, Object> error = new HashMap<>();
//                error.put("status", "error");
//                error.put("message", "MRREL file path not configured");
//                return ResponseEntity.badRequest().body(error);
//            }
//
//            new Thread(() -> {
//                try {
//                    logger.info("🚀 Starting optimized import in background thread");
//                    relationshipImporter.importRelationships(mrrelPath);
//                    logger.info("✅ Optimized import completed successfully");
//                } catch (Exception e) {
//                    logger.error("❌ Error in optimized import", e);
//                }
//            }).start();
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("status", "success");
//            result.put("message", "Optimized import started in background (direct to Neo4j)");
//            result.put("mrrel_path", mrrelPath);
//            result.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            logger.error("Error starting optimized import", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * עיבוד קשרים מMRREL לפוסטגרס
//     */
//    @PostMapping("/process-relationships")
//    public ResponseEntity<Map<String, Object>> processRelationships() {
//        logger.info("🔗 API call to process relationships");
//
//        try {
//            String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
//            if (mrrelPath == null || mrrelPath.isEmpty()) {
//                Map<String, Object> error = new HashMap<>();
//                error.put("status", "error");
//                error.put("message", "MRREL file path not configured");
//                return ResponseEntity.badRequest().body(error);
//            }
//
//            new Thread(() -> {
//                try {
//                    logger.info("🚀 Starting relationship processing in background thread");
//                    relationshipProcessor.processAndSaveRelationships(mrrelPath);
//                    logger.info("✅ Relationship processing completed successfully");
//                } catch (Exception e) {
//                    logger.error("❌ Error in relationship processing", e);
//                }
//            }).start();
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("status", "success");
//            result.put("message", "Relationship processing started in background");
//            result.put("mrrel_path", mrrelPath);
//            result.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            logger.error("Error starting relationship processing", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * ייבוא קשרים מפוסטגרס לNeo4j
//     */
//    @PostMapping("/import-relationships-to-neo4j")
//    public ResponseEntity<Map<String, Object>> importRelationshipsToNeo4j() {
//        logger.info("📊 API call to import relationships to Neo4j");
//
//        try {
//            new Thread(() -> {
//                try {
//                    logger.info("🚀 Starting relationship import to Neo4j in background thread");
//                    entityImporter.importRelationshipsFromDB();
//                    logger.info("✅ Relationship import to Neo4j completed successfully");
//                } catch (Exception e) {
//                    logger.error("❌ Error in relationship import to Neo4j", e);
//                }
//            }).start();
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("status", "success");
//            result.put("message", "Relationship import to Neo4j started in background");
//            result.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            logger.error("Error starting relationship import to Neo4j", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * סטטיסטיקות קשרים
//     */
//    @GetMapping("/relationship-stats")
//    public ResponseEntity<Map<String, Object>> getRelationshipStatistics() {
//        logger.info("📈 API call to get relationship statistics");
//
//        try {
//            Map<String, Object> stats = new HashMap<>();
//
//            // סטטיסטיקות מפוסטגרס
//            long postgresCount = relationshipRepository.count();
//            stats.put("postgres_relationships", postgresCount);
//
//            List<String> relationshipTypes = relationshipRepository.findDistinctRelationshipTypes();
//            stats.put("postgres_relationship_types", relationshipTypes.size());
//            stats.put("postgres_relationship_types_list", relationshipTypes);
//
//            List<String> sources = relationshipRepository.findDistinctSources();
//            stats.put("postgres_sources", sources);
//
//            // סטטיסטיקות מNeo4j
//            Map<String, Long> neo4jStats = entityImporter.getRelationshipStatistics();
//            stats.put("neo4j_relationships", neo4jStats);
//
//            stats.put("status", "success");
//            stats.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(stats);
//
//        } catch (Exception e) {
//            logger.error("Error getting relationship statistics", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * תהליך מלא - עיבוד וייבוא
//     */
//    @PostMapping("/full-pipeline")
//    public ResponseEntity<Map<String, Object>> runFullPipeline() {
//        logger.info("🚀 API call to run full relationship pipeline");
//
//        try {
//            String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
//            if (mrrelPath == null || mrrelPath.isEmpty()) {
//                Map<String, Object> error = new HashMap<>();
//                error.put("status", "error");
//                error.put("message", "MRREL file path not configured");
//                return ResponseEntity.badRequest().body(error);
//            }
//
//            new Thread(() -> {
//                try {
//                    logger.info("🚀 Starting FULL pipeline: MRREL -> PostgreSQL -> Neo4j");
//
//                    logger.info("Step 1: Processing MRREL to PostgreSQL...");
//                    relationshipProcessor.processAndSaveRelationships(mrrelPath);
//                    logger.info("✅ Step 1 completed");
//
//                    logger.info("Step 2: Importing from PostgreSQL to Neo4j...");
//                    entityImporter.importRelationshipsFromDB();
//                    logger.info("✅ Step 2 completed");
//
//                    logger.info("🎉 FULL pipeline completed successfully!");
//                } catch (Exception e) {
//                    logger.error("❌ Error in full pipeline", e);
//                }
//            }).start();
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("status", "success");
//            result.put("message", "Full relationship pipeline started in background");
//            result.put("mrrel_path", mrrelPath);
//            result.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            logger.error("Error starting full pipeline", e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("status", "error");
//            error.put("message", e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    /**
//     * הוראות שימוש
//     */
//    @GetMapping("/help")
//    public ResponseEntity<Map<String, Object>> showHelp() {
//        Map<String, Object> help = new HashMap<>();
//
//        help.put("available_endpoints", Map.of(
//                "GET /api/admin/import/analyze", "Analyze current graph state",
//                "POST /api/admin/import/clean/self-loops", "Remove self-loops only",
//                "POST /api/admin/import/clean/full", "Perform full cleanup",
//                "POST /api/admin/import/restart-optimized", "Direct MRREL to Neo4j (old method)",
//                "POST /api/admin/import/process-relationships", "Process MRREL to PostgreSQL",
//                "POST /api/admin/import/import-relationships-to-neo4j", "Import PostgreSQL to Neo4j",
//                "POST /api/admin/import/full-pipeline", "Complete pipeline: MRREL -> PostgreSQL -> Neo4j",
//                "GET /api/admin/import/relationship-stats", "Get relationship statistics"
//        ));
//
//        help.put("recommended_workflow", new String[]{
//                "OPTION 1 - Full Pipeline (Recommended):",
//                "1. Call POST /full-pipeline - does everything automatically",
//                "2. Monitor with GET /relationship-stats",
//                "",
//                "OPTION 2 - Step by Step:",
//                "1. Call POST /process-relationships (MRREL -> PostgreSQL)",
//                "2. Call POST /import-relationships-to-neo4j (PostgreSQL -> Neo4j)",
//                "3. Monitor with GET /relationship-stats",
//                "",
//                "OPTION 3 - Direct (Old method):",
//                "1. Call POST /restart-optimized (direct MRREL -> Neo4j)"
//        });
//
//        help.put("current_config", Map.of(
//                "mrrel_path", environment.getProperty("mediaid.umls.mrrel.path", "NOT CONFIGURED"),
//                "import_enabled", environment.getProperty("mediaid.data.import.enabled", "false"),
//                "relationships_enabled", environment.getProperty("mediaid.data.import.relationships", "false")
//        ));
//
//        help.put("time_estimates", Map.of(
//                "process_relationships", "2-6 hours (depends on MRREL size)",
//                "import_to_neo4j", "30-90 minutes (depends on PostgreSQL data)",
//                "full_pipeline", "3-8 hours total",
//                "note", "Times vary based on hardware and data size"
//        ));
//
//        return ResponseEntity.ok(help);
//    }
//}