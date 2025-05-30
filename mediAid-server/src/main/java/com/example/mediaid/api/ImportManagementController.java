package com.example.mediaid.api;

import com.example.mediaid.bl.neo4j.UmlsRelationshipImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/import")
public class ImportManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ImportManagementController.class);

    @Autowired
    private UmlsRelationshipImporter relationshipImporter;

    @Autowired
    private Environment environment;

    /**
     * ניתוח מצב הגרף הנוכחי
     */
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeGraph() {
        logger.info("🔍 API call to analyze graph");

        try {
            Map<String, Object> analysis = relationshipImporter.analyzeCurrentGraph();
            analysis.put("status", "success");
            analysis.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            logger.error("Error analyzing graph", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * ניקוי לולאות עצמיות בלבד
     */
    @PostMapping("/clean/self-loops")
    public ResponseEntity<Map<String, Object>> cleanSelfLoops() {
        logger.info("🧹 API call to clean self-loops");

        try {
            int removed = relationshipImporter.cleanSelfLoops();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("self_loops_removed", removed);
            result.put("message", "Successfully removed " + removed + " self-loops");
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error cleaning self-loops", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * ניקוי מלא של הגרף
     */
    @PostMapping("/clean/full")
    public ResponseEntity<Map<String, Object>> performFullCleanup() {
        logger.info("🧹 API call to perform full cleanup");

        try {
            Map<String, Object> results = relationshipImporter.performFullGraphCleanup();
            results.put("status", "success");
            results.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            logger.error("Error performing full cleanup", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * התחלת ייבוא מחדש עם הסינון החדש
     */
    @PostMapping("/restart-optimized")
    public ResponseEntity<Map<String, Object>> restartOptimizedImport() {
        logger.info("🚀 API call to restart optimized import");

        try {
            String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
            if (mrrelPath == null || mrrelPath.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "MRREL file path not configured");
                return ResponseEntity.badRequest().body(error);
            }

            // התחלת הייבוא בthread נפרד כדי לא לחסום את ה-API
            new Thread(() -> {
                try {
                    logger.info("🚀 Starting optimized import in background thread");
                    relationshipImporter.importRelationships(mrrelPath);
                    logger.info("✅ Optimized import completed successfully");
                } catch (Exception e) {
                    logger.error("❌ Error in optimized import", e);
                }
            }).start();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Optimized import started in background");
            result.put("mrrel_path", mrrelPath);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error starting optimized import", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * מציג הוראות לשימוש
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> showHelp() {
        Map<String, Object> help = new HashMap<>();

        help.put("available_endpoints", Map.of(
                "GET /api/admin/import/analyze", "Analyze current graph state",
                "POST /api/admin/import/clean/self-loops", "Remove self-loops only",
                "POST /api/admin/import/clean/full", "Perform full cleanup (self-loops + optimize)",
                "POST /api/admin/import/restart-optimized", "Restart import with optimized filtering"
        ));

        help.put("recommended_workflow", new String[]{
                "1. Call GET /analyze to see current problems",
                "2. Call POST /clean/full to clean all issues",
                "3. Call POST /restart-optimized to start filtered import",
                "4. Monitor logs for progress"
        });

        help.put("current_config", Map.of(
                "mrrel_path", environment.getProperty("mediaid.umls.mrrel.path", "NOT CONFIGURED"),
                "import_enabled", environment.getProperty("mediaid.data.import.enabled", "false"),
                "relationships_enabled", environment.getProperty("mediaid.data.import.relationships", "false")
        ));

        return ResponseEntity.ok(help);
    }
}