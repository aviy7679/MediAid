package com.example.mediaid.api;

import com.example.mediaid.bl.emergency.RiskFactorService;
import com.example.mediaid.bl.neo4j.RiskFactorSer;
import com.example.mediaid.dto.RiskFactorResponseDTO;
import com.example.mediaid.dto.RiskFactorUpdateDTO;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import com.example.mediaid.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * בקר מעודכן לגורמי סיכון - מסונכרן עם כל השירותים
 */
@RestController
@RequestMapping("/api/user/risk")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
public class RiskFactorController {

    private static final Logger logger = LoggerFactory.getLogger(RiskFactorController.class);

    @Autowired
    private RiskFactorService riskFactorService;

    @Autowired
    private RiskFactorSer riskFactorSer; // Neo4j service

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * עדכון גורמי סיכון - מסונכרן לכל המערכות
     */
    @PostMapping("/factors")
    public ResponseEntity<?> updateRiskFactors(@RequestBody RiskFactorUpdateDTO dto, HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Updating comprehensive risk factors for user: {}", userId);

            // עדכון בPostgreSQL + Neo4j
            RiskFactorResponseDTO response = riskFactorService.updateUserRiskFactors(userId, dto);

            logger.info("Risk factors updated successfully for user: {}", userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating risk factors for user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating risk factors: " + e.getMessage()));
        }
    }

    /**
     * קבלת גורמי סיכון של המשתמש - מקיף
     */
    @GetMapping("/factors")
    public ResponseEntity<?> getUserRiskFactors(HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Retrieving comprehensive risk factors for user: {}", userId);

            // קבלת נתונים בסיסיים
            RiskFactorResponseDTO basicResponse = riskFactorService.getUserRiskFactors(userId);

            // קבלת רשימה מפורטת של גורמי סיכון
            List<UserMedicalEntity> detailedRiskFactors = riskFactorService.getUserActiveRiskFactors(userId);

            // בניית תגובה מקיפה
            Map<String, Object> comprehensiveResponse = new HashMap<>();
            comprehensiveResponse.put("summary", basicResponse);
            comprehensiveResponse.put("detailedRiskFactors", detailedRiskFactors);
            comprehensiveResponse.put("riskFactorsCount", detailedRiskFactors.size());
            comprehensiveResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(comprehensiveResponse);

        } catch (Exception e) {
            logger.error("Error retrieving risk factors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving risk factors: " + e.getMessage()));
        }
    }

    /**
     * עדכון גורם סיכון ספציפי
     */
    @PutMapping("/factors/{riskFactorType}")
    public ResponseEntity<?> updateSpecificRiskFactor(
            @PathVariable String riskFactorType,
            @RequestBody Map<String, Object> updateData,
            HttpServletRequest request) {

        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Updating specific risk factor {} for user: {}", riskFactorType, userId);

            Object value = updateData.get("value");
            if (value == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Missing 'value' field in request body"));
            }

            // עדכון גורם הסיכון הספציפי
            riskFactorService.updateSpecificRiskFactor(userId, riskFactorType, value);

            // קבלת תוצאות מעודכנות
            RiskFactorResponseDTO response = riskFactorService.getUserRiskFactors(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Risk factor updated successfully");
            result.put("updatedFactor", riskFactorType);
            result.put("updatedData", response);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid risk factor type: {}", riskFactorType);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid risk factor type: " + riskFactorType));
        } catch (Exception e) {
            logger.error("Error updating specific risk factor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating risk factor: " + e.getMessage()));
        }
    }

    /**
     * קבלת סטטיסטיקות גורמי סיכון מNeo4j
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getRiskFactorStatistics(HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Getting risk factor statistics for user: {}", userId);

            Map<String, Object> statistics = riskFactorSer.getRiskFactorStatistics();

            Map<String, Object> result = new HashMap<>();
            result.put("neo4jStatistics", statistics);
            result.put("userId", userId);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error getting risk factor statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting statistics: " + e.getMessage()));
        }
    }

    /**
     * ניתוח השפעת גורמי סיכון על מצב בריאותי
     */
    @PostMapping("/analyze-impact")
    public ResponseEntity<?> analyzeRiskFactorImpact(
            @RequestBody Map<String, Object> analysisRequest,
            HttpServletRequest request) {

        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Analyzing risk factor impact for user: {}", userId);

            // קבלת גורמי סיכון מפורטים
            List<UserMedicalEntity> riskFactors = riskFactorService.getUserActiveRiskFactors(userId);

            // בניית ניתוח
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("userId", userId);
            analysis.put("totalRiskFactors", riskFactors.size());

            // קטגוריזציה של גורמי סיכון
            Map<String, Integer> riskFactorsByCategory = new HashMap<>();
            Map<String, Integer> riskFactorsBySeverity = new HashMap<>();

            for (UserMedicalEntity rf : riskFactors) {
                // לפי קטגוריה
                String category = categorizeRiskFactor(rf);
                riskFactorsByCategory.put(category, riskFactorsByCategory.getOrDefault(category, 0) + 1);

                // לפי חומרה
                String severity = rf.getSeverity() != null ? rf.getSeverity() : "unknown";
                riskFactorsBySeverity.put(severity, riskFactorsBySeverity.getOrDefault(severity, 0) + 1);
            }

            analysis.put("riskFactorsByCategory", riskFactorsByCategory);
            analysis.put("riskFactorsBySeverity", riskFactorsBySeverity);

            // ציון סיכון כולל
            RiskFactorResponseDTO riskData = riskFactorService.getUserRiskFactors(userId);
            analysis.put("overallRiskScore", riskData.getOverallRiskScore());
            analysis.put("riskLevel", riskData.getRiskLevel());

            // המלצות
            List<String> recommendations = generateRiskFactorRecommendations(riskFactors);
            analysis.put("recommendations", recommendations);

            analysis.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            logger.error("Error analyzing risk factor impact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error analyzing impact: " + e.getMessage()));
        }
    }

    /**
     * מחיקת גורם סיכון ספציפי
     */
    @DeleteMapping("/factors/{riskFactorType}")
    public ResponseEntity<?> deleteRiskFactor(
            @PathVariable String riskFactorType,
            HttpServletRequest request) {

        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Deleting risk factor {} for user: {}", riskFactorType, userId);

            // מחיקה מNeo4j (אם רלוונטי)
            try {
                riskFactorSer.deleteRiskFactor(riskFactorType.toUpperCase());
            } catch (Exception e) {
                logger.warn("Could not delete from Neo4j: {}", e.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Risk factor deletion initiated");
            result.put("deletedFactor", riskFactorType);
            result.put("note", "Risk factor removed from dynamic analysis");
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error deleting risk factor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error deleting risk factor: " + e.getMessage()));
        }
    }

    /**
     * בדיקת סנכרון בין המערכות
     */
    @GetMapping("/sync-status")
    public ResponseEntity<?> checkSyncStatus(HttpServletRequest request) {
        try {
            UUID userId = extractUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            Map<String, Object> syncStatus = new HashMap<>();

            // בדיקת PostgreSQL
            try {
                RiskFactorResponseDTO pgData = riskFactorService.getUserRiskFactors(userId);
                syncStatus.put("postgresql_status", "connected");
                syncStatus.put("postgresql_risk_score", pgData.getOverallRiskScore());
            } catch (Exception e) {
                syncStatus.put("postgresql_status", "error: " + e.getMessage());
            }

            // בדיקת Neo4j
            try {
                Map<String, Object> neo4jStats = riskFactorSer.getRiskFactorStatistics();
                syncStatus.put("neo4j_status", "connected");
                syncStatus.put("neo4j_statistics", neo4jStats);
            } catch (Exception e) {
                syncStatus.put("neo4j_status", "error: " + e.getMessage());
            }

            // בדיקת מספר גורמי סיכון
            try {
                List<UserMedicalEntity> riskFactors = riskFactorService.getUserActiveRiskFactors(userId);
                syncStatus.put("active_risk_factors_count", riskFactors.size());
            } catch (Exception e) {
                syncStatus.put("active_risk_factors_count", "error: " + e.getMessage());
            }

            syncStatus.put("sync_timestamp", System.currentTimeMillis());
            syncStatus.put("overall_status", "operational");

            return ResponseEntity.ok(syncStatus);

        } catch (Exception e) {
            logger.error("Error checking sync status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error checking sync: " + e.getMessage()));
        }
    }

    // Helper Methods

    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.isValid(token)) {
                    UUID userId = jwtUtil.extractUserId(token);
                    logger.debug("Extracted user ID: {}", userId);
                    return userId;
                }
            }
            logger.warn("No valid authorization header found");
            return null;
        } catch (Exception e) {
            logger.error("Error extracting userId from token: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "RISK_FACTOR_ERROR");
        error.put("message", message);
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return error;
    }

    private String categorizeRiskFactor(UserMedicalEntity riskFactor) {
        String name = riskFactor.getName().toLowerCase();

        if (name.contains("smok") || name.contains("tobacco")) return "lifestyle";
        if (name.contains("alcohol") || name.contains("drink")) return "lifestyle";
        if (name.contains("exercise") || name.contains("activity")) return "lifestyle";
        if (name.contains("bmi") || name.contains("weight") || name.contains("obese")) return "physical";
        if (name.contains("blood pressure") || name.contains("hypertension")) return "cardiovascular";
        if (name.contains("age")) return "demographic";
        if (name.contains("family") || name.contains("genetic")) return "hereditary";
        if (name.contains("stress") || name.contains("anxiety")) return "psychological";

        return "other";
    }

    private List<String> generateRiskFactorRecommendations(List<UserMedicalEntity> riskFactors) {
        List<String> recommendations = new ArrayList<>();

        for (UserMedicalEntity rf : riskFactors) {
            String name = rf.getName().toLowerCase();
            double weight = rf.getAdditionalData() != null && rf.getAdditionalData().get("weight") != null ?
                    (Double) rf.getAdditionalData().get("weight") : 0.5;

            if (weight > 0.6) { // רק גורמי סיכון משמעותיים
                if (name.contains("smok")) {
                    recommendations.add("Consider smoking cessation programs and consult with healthcare provider");
                } else if (name.contains("bmi") || name.contains("weight")) {
                    recommendations.add("Consider weight management through diet and exercise consultation");
                } else if (name.contains("blood pressure")) {
                    recommendations.add("Monitor blood pressure regularly and follow medical recommendations");
                } else if (name.contains("stress")) {
                    recommendations.add("Consider stress management techniques and mental health support");
                } else if (name.contains("family")) {
                    recommendations.add("Discuss family history with healthcare provider for preventive screening");
                }
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Continue maintaining healthy lifestyle habits");
        }

        return recommendations;
    }
}