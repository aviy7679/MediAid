

package com.example.mediaid.api;

import com.example.mediaid.bl.emergency.BasicTreatmentRecommendationEngine;
import com.example.mediaid.bl.emergency.MedicalGraphAnalyticsService;
import com.example.mediaid.bl.emergency.SymptomAnalysisService;
import com.example.mediaid.bl.emergency.TreatmentRecommendationEngine;
import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.example.mediaid.dto.emergency.TreatmentPlan;
import com.example.mediaid.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    @Autowired
    private SymptomAnalysisService symptomAnalysisService;

    @Autowired
    private BasicTreatmentRecommendationEngine basicTreatmentEngine;

    @Autowired
    private TreatmentRecommendationEngine treatmentEngine;

    @Autowired
    private MedicalGraphAnalyticsService graphAnalyticsService;


    @Autowired
    private JwtUtil jwtUtil;

    @Value("${mediaid.analysis.use-advanced-engine:true}")
    private boolean useAdvancedEngine;


    /**
     * העלאת נתונים לניתוח - משולב (טקסט + תמונה + אודיו)
     */
    @PostMapping("/upload-data")
    public ResponseEntity<?> uploadDataWithAdvancedAnalysis(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "audio", required = false) MultipartFile audioFile,
            @RequestParam(value = "useAdvanced", defaultValue = "true") boolean forceAdvanced,
            HttpServletRequest httpRequest) {

        try {
            UUID userId = extractUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("Starting {} analysis for user: {}",
                    (useAdvancedEngine && forceAdvanced) ? "ADVANCED GRAPH" : "BASIC", userId);

            Set<ExtractedSymptom> allExtractedSymptoms = new LinkedHashSet<>();
            List<String> processedInputs = new ArrayList<>();

            // שלב 1: עיבוד נתונים (כמו קודם)
            if (text != null && !text.trim().isEmpty()) {
                logger.info("Processing text data for user: {}", userId);
                try {
                    var textSymptoms = symptomAnalysisService.extractSymptomsFromText(text);
                    allExtractedSymptoms.addAll(textSymptoms);
                    processedInputs.add("text");
                    logger.info("Extracted {} symptoms from text", textSymptoms.size());
                } catch (Exception e) {
                    logger.error("Error processing text: {}", e.getMessage());
                    processedInputs.add("text(error: " + e.getMessage() + ")");
                }
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                logger.info("Processing image data for user: {}", userId);
                try {
                    var imageSymptoms = symptomAnalysisService.extractedSymptomsFromImage(imageFile.getBytes());
                    allExtractedSymptoms.addAll(imageSymptoms);
                    processedInputs.add("image");
                    logger.info("Extracted {} symptoms from image", imageSymptoms.size());
                } catch (Exception e) {
                    logger.error("Error processing image: {}", e.getMessage());
                    processedInputs.add("image (error: " + e.getMessage() + ")");
                }
            }

            // בדיקה אם נמצאו סימפטומים
            if (allExtractedSymptoms.isEmpty()) {
                return createEmptyResponse(processedInputs);
            }

            // שלב 2: בחירה בין מנוע בסיסי למתקדם
            TreatmentPlan treatmentPlan;
            String analysisType;

            if (useAdvancedEngine && forceAdvanced) {
                logger.info("Using ADVANCED Graph-Based Analysis Engine");
                analysisType = "advanced_graph_thinking";

                long startTime = System.currentTimeMillis();
                treatmentPlan = treatmentEngine.analyzeSituation(userId, allExtractedSymptoms);
                long analysisTime = System.currentTimeMillis() - startTime;

                logger.info("Advanced analysis completed in {}ms", analysisTime);

                // הוספת מידע על זמן הניתוח למידע הנוסף
                if (treatmentPlan.getAdditionalInfo() == null) {
                    treatmentPlan.setAdditionalInfo(new HashMap<>());
                }
                treatmentPlan.getAdditionalInfo().put("analysisTimeMs", analysisTime);
                treatmentPlan.getAdditionalInfo().put("engineType", "advanced_graph_thinking");

            } else {
                // שימוש במנוע המקורי
                logger.info("Using Original Analysis Engine");
                analysisType = "basic_pathfinding";

                try {
                    treatmentPlan = basicTreatmentEngine.analyzeSituation(userId, allExtractedSymptoms);
                    if (treatmentPlan.getAdditionalInfo() == null) {
                        treatmentPlan.setAdditionalInfo(new HashMap<>());
                    }
                    treatmentPlan.getAdditionalInfo().put("engineType", "basic_pathfinding");
                } catch (Exception e) {
                    logger.error("Error in original analysis: {}", e.getMessage());
                    treatmentPlan = createBasicTreatmentPlan(allExtractedSymptoms);
                }
            }

            // שלב 3: בניית תשובה מקיפה
            Map<String, Object> response = buildComprehensiveResponse(
                    text, imageFile, audioFile, processedInputs,
                    allExtractedSymptoms, treatmentPlan, analysisType, userId);

            logger.info("Analysis completed for user {}. Engine: {}, Symptoms: {}, Urgency: {}",
                    userId, analysisType, allExtractedSymptoms.size(), treatmentPlan.getUrgencyLevel());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Critical error in enhanced analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error processing data: " + e.getMessage()));
        }
    }


    /**
     * קבלת מידע רפואי נוכחי של המשתמש (לצורך בדיקה)
     */
    @GetMapping("/user-medical-context")
    public ResponseEntity<?> getUserMedicalContext(HttpServletRequest httpRequest) {
        try {
            UUID userId = extractUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            var medicalContext = treatmentEngine.getUserMedicalContext(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", userId,
                    "medicalContext", medicalContext
            ));

        } catch (Exception e) {
            logger.error("Error getting user medical context", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting medical context: " + e.getMessage()));
        }
    }

    /**
     * בדיקת חיבור לשרת Python
     */
    @GetMapping("/python-health")
    public ResponseEntity<?> checkPythonHealth() {
        try {
            boolean isHealthy = symptomAnalysisService.checkPythonServerHealth();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "pythonServerHealthy", isHealthy,
                    "message", isHealthy ? "Python server is responsive" : "Python server is not responding"
            ));

        } catch (Exception e) {
            logger.error("Error checking Python server health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error checking Python server: " + e.getMessage()));
        }
    }

    // Helper methods
    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.isValid(token)) {
                    return jwtUtil.extractUserId(token);
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error extracting userId from token", e);
            return null;
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "DATA_ANALYSIS_ERROR");
        error.put("message", message);
        return error;
    }

    private String determineAnalysisType(List<String> processedInputs) {
        if (processedInputs.size() == 1) {
            return processedInputs.get(0);
        } else if (processedInputs.size() > 1) {
            return "combined";
        }
        return "none";
    }

    //תכנית בסיסת ברירת מחדל
    private TreatmentPlan createBasicTreatmentPlan(Set<ExtractedSymptom> symptoms) {
        logger.warn("Creating basic treatment plan due to analysis error");

        TreatmentPlan basicPlan = new TreatmentPlan();
        basicPlan.setUrgencyLevel(TreatmentPlan.UrgencyLevel.MEDIUM);
        basicPlan.setMainConcern("Symptoms were found that require medical consultation");
        basicPlan.setReasoning("A full analysis could not be performed. It is recommended to consult a medical professional.");
        // פעולות מיידיות בסיסיות
        List<com.example.mediaid.dto.emergency.ImmediateAction> actions = new ArrayList<>();
        com.example.mediaid.dto.emergency.ImmediateAction action = new com.example.mediaid.dto.emergency.ImmediateAction();
        action.setType(com.example.mediaid.dto.emergency.ImmediateAction.ActionType.SEEK_IMMEDIATE_CARE);
        action.setDescription("Consult your primary care physician");
        action.setReason("Clarify the reported symptoms");
        action.setPriority(1);
        actions.add(action);
        basicPlan.setImmediateActions(actions);
        // ביקור רופא
        List<com.example.mediaid.dto.emergency.DoctorVisit> visits = new ArrayList<>();
        com.example.mediaid.dto.emergency.DoctorVisit visit = new com.example.mediaid.dto.emergency.DoctorVisit();
        visit.setType(com.example.mediaid.dto.emergency.DoctorVisit.DoctorType.FAMILY_DOCTOR);
        visit.setReason("Clarify symptoms");
        visit.setUrgency("Within a few days");
        visits.add(visit);
        basicPlan.setDoctorVisits(visits);
        // אין בדיקות או קשרים נוספים כרגע
        basicPlan.setRecommendedTests(new ArrayList<>());
        basicPlan.setFoundConnections(new ArrayList<>());
        basicPlan.setAdditionalInfo(Map.of(
                "note", "Basic analysis only",
                "symptomsCount", symptoms.size()
        ));

        return basicPlan;
    }



    @PostMapping("/advanced-graph-analysis")
    public ResponseEntity<?> performAdvancedGraphAnalysis(
            @RequestBody Map<String, Object> analysisRequest,
            HttpServletRequest httpRequest) {

        try {
            UUID userId = extractUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("🕸️ Performing dedicated Graph Analytics for user: {}", userId);

            // חילוץ פרמטרים מהבקשה
            @SuppressWarnings("unchecked")
            List<String> symptomNames = (List<String>) analysisRequest.get("symptoms");
            int maxPathDepth = (Integer) analysisRequest.getOrDefault("maxDepth", 4);
            boolean includeCommunities = (Boolean) analysisRequest.getOrDefault("includeCommunities", true);
            boolean includeHubs = (Boolean) analysisRequest.getOrDefault("includeHubs", true);

            // המרת שמות סימפטומים לאובייקטים
            Set<ExtractedSymptom> symptoms = new HashSet<>();
            for (String symptomName : symptomNames) {
                ExtractedSymptom symptom = new ExtractedSymptom();
                symptom.setName(symptomName);
                symptom.setCui("MANUAL_" + symptomName.hashCode()); // CUI זמני
                symptom.setConfidence(1.0);
                symptom.setSource("manual");
                symptoms.add(symptom);
            }

            // קבלת הקשר הרפואי של המשתמש
            var userContext = treatmentEngine.getUserMedicalContext(userId);
            var allUserEntities = getAllUserEntities(userContext);

            Map<String, Object> graphAnalysisResults = new HashMap<>();

            // 1. Advanced Pathway Analysis
            List<MedicalGraphAnalyticsService.MedicalPathway> pathways = new ArrayList<>();
            for (var entity : allUserEntities) {
                var entityPathways = graphAnalyticsService.findMedicalPathways(
                        entity.getCui(), symptoms, maxPathDepth);
                pathways.addAll(entityPathways);
            }
            graphAnalysisResults.put("advancedPathways", pathways);
            logger.info("🛣️ Found {} advanced pathways", pathways.size());

            // 2. Community Detection (אם מתבקש)
            if (includeCommunities) {
                var communities = graphAnalyticsService.detectMedicalCommunities(allUserEntities);
                graphAnalysisResults.put("medicalCommunities", communities);
                logger.info("🕸️ Detected {} medical communities", communities.size());
            }

            // 3. Risk Propagation Analysis
            var riskPropagation = graphAnalyticsService.calculateRiskPropagation(
                    userContext.getRiskFactors(), symptoms, 0.85);
            graphAnalysisResults.put("riskPropagation", riskPropagation);
            logger.info("📊 Risk propagation: {:.3f} total risk", riskPropagation.getTotalRiskScore());

            // 4. Medical Hub Analysis (אם מתבקש)
            if (includeHubs) {
                var medicalHubs = graphAnalyticsService.findMedicalHubs(allUserEntities);
                graphAnalysisResults.put("medicalHubs", medicalHubs);
                logger.info("🎯 Identified {} medical hubs", medicalHubs.size());
            }

            // בניית תשובה מפורטת
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysisType", "dedicated_graph_analytics");
            response.put("userId", userId);
            response.put("inputSymptoms", symptomNames);
            response.put("analysisParameters", Map.of(
                    "maxDepth", maxPathDepth,
                    "includeCommunities", includeCommunities,
                    "includeHubs", includeHubs
            ));
            response.put("userContextSize", Map.of(
                    "medications", userContext.getCurrentMedications().size(),
                    "diseases", userContext.getActiveDiseases().size(),
                    "riskFactors", userContext.getRiskFactors().size()
            ));
            response.put("graphAnalysisResults", graphAnalysisResults);
            response.put("analysisTimestamp", System.currentTimeMillis());

            logger.info("✅ Dedicated graph analysis completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error in dedicated graph analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error in graph analysis: " + e.getMessage()));
        }
    }

    /**
     * API להשוואה בין המנועים
     */
    @PostMapping("/compare-engines")
    public ResponseEntity<?> compareAnalysisEngines(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            HttpServletRequest httpRequest) {

        try {
            UUID userId = extractUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid or missing authorization token"));
            }

            logger.info("⚖️ Comparing analysis engines for user: {}", userId);

            // חילוץ סימפטומים
            Set<ExtractedSymptom> symptoms = extractSymptoms(text, imageFile);
            if (symptoms.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("No symptoms found for comparison"));
            }

            Map<String, Object> comparison = new HashMap<>();

            // ניתוח עם המנוע המקורי
            long startTime1 = System.currentTimeMillis();
            TreatmentPlan basicPlan = basicTreatmentEngine.analyzeSituation(userId, symptoms);
            long basicTime = System.currentTimeMillis() - startTime1;

            // ניתוח עם המנוע המתקדם
            long startTime2 = System.currentTimeMillis();
            TreatmentPlan advancedPlan = treatmentEngine.analyzeSituation(userId, symptoms);
            long advancedTime = System.currentTimeMillis() - startTime2;

            // בניית השוואה
            comparison.put("basicAnalysis", Map.of(
                    "urgencyLevel", basicPlan.getUrgencyLevel(),
                    "connectionsFound", basicPlan.getFoundConnections().size(),
                    "immediateActions", basicPlan.getImmediateActions().size(),
                    "recommendedTests", basicPlan.getRecommendedTests().size(),
                    "analysisTimeMs", basicTime,
                    "mainConcern", basicPlan.getMainConcern()
            ));

            comparison.put("advancedAnalysis", Map.of(
                    "urgencyLevel", advancedPlan.getUrgencyLevel(),
                    "connectionsFound", advancedPlan.getFoundConnections().size(),
                    "immediateActions", advancedPlan.getImmediateActions().size(),
                    "recommendedTests", advancedPlan.getRecommendedTests().size(),
                    "analysisTimeMs", advancedTime,
                    "mainConcern", advancedPlan.getMainConcern(),
                    "additionalInsights", advancedPlan.getAdditionalInfo()
            ));

            comparison.put("performance", Map.of(
                    "basicTimeMs", basicTime,
                    "advancedTimeMs", advancedTime,
                    "timeRatio", (double) advancedTime / basicTime,
                    "advancedSlower", advancedTime > basicTime
            ));

            comparison.put("insightsDiff", Map.of(
                    "connectionsImprovement", advancedPlan.getFoundConnections().size() - basicPlan.getFoundConnections().size(),
                    "urgencyDifference", !basicPlan.getUrgencyLevel().equals(advancedPlan.getUrgencyLevel()),
                    "moreDetailedReasoning", advancedPlan.getReasoning().length() > basicPlan.getReasoning().length()
            ));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comparison", comparison);
            response.put("symptomsAnalyzed", symptoms.size());
            response.put("comparisonTimestamp", System.currentTimeMillis());

            logger.info("⚖️ Engine comparison completed. Basic: {}ms, Advanced: {}ms", basicTime, advancedTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error in engine comparison", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error in comparison: " + e.getMessage()));
        }
    }

    // =============== HELPER METHODS ===============

    private Set<ExtractedSymptom> extractSymptoms(String text, MultipartFile imageFile) {
        Set<ExtractedSymptom> symptoms = new LinkedHashSet<>();

        if (text != null && !text.trim().isEmpty()) {
            try {
                symptoms.addAll(symptomAnalysisService.extractSymptomsFromText(text));
            } catch (Exception e) {
                logger.warn("Error extracting from text: {}", e.getMessage());
            }
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                symptoms.addAll(symptomAnalysisService.extractedSymptomsFromImage(imageFile.getBytes()));
            } catch (Exception e) {
                logger.warn("Error extracting from image: {}", e.getMessage());
            }
        }

        return symptoms;
    }

    private Map<String, Object> buildComprehensiveResponse(
            String text, MultipartFile imageFile, MultipartFile audioFile,
            List<String> processedInputs, Set<ExtractedSymptom> symptoms,
            TreatmentPlan treatmentPlan, String analysisType, UUID userId) {

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("analysisType", analysisType);
        response.put("engineUsed", analysisType.equals("advanced_graph_thinking") ? "Enhanced Graph Analytics" : "Basic Pathfinding");
        response.put("processedInputs", processedInputs);

        // הוסף טקסט אם קיים
        if (text != null && !text.trim().isEmpty()) {
            response.put("originalText", text);
        }

        // הוסף שם קובץ תמונה אם קיים
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageFileName = imageFile.getOriginalFilename();
            if (imageFileName != null && !imageFileName.trim().isEmpty()) {
                response.put("imageFileName", imageFileName);
            }
        }

        response.put("extractedSymptoms", new ArrayList<>(symptoms));
        response.put("treatmentPlan", treatmentPlan);
        response.put("timestamp", System.currentTimeMillis());

        // מידע מתקדם אם השתמשנו במנוע המתקדם
        if ("advanced_graph_thinking".equals(analysisType) && treatmentPlan.getAdditionalInfo() != null) {
            response.put("graphInsights", treatmentPlan.getAdditionalInfo());
        }

        return response;
    }

    private ResponseEntity<?> createEmptyResponse(List<String> processedInputs) {
        Map<String, Object> emptyResponse = new HashMap<>();
        emptyResponse.put("success", true);
        emptyResponse.put("message", "No symptoms detected in the provided data");
        emptyResponse.put("processedInputs", processedInputs);
        emptyResponse.put("extractedSymptoms", new ArrayList<>());
        emptyResponse.put("treatmentPlan", null);
        emptyResponse.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(emptyResponse);
    }

    private List<com.example.mediaid.dto.emergency.UserMedicalEntity> getAllUserEntities(
            com.example.mediaid.dto.emergency.UserMedicalContext context) {
        List<com.example.mediaid.dto.emergency.UserMedicalEntity> allEntities = new ArrayList<>();
        allEntities.addAll(context.getCurrentMedications());
        allEntities.addAll(context.getActiveDiseases());
        allEntities.addAll(context.getRiskFactors());
        return allEntities;
    }

}