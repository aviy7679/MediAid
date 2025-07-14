package com.example.mediaid.bl.emergency;

import com.example.mediaid.dto.emergency.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.mediaid.constants.MedicalAnalysisConstants.*;

@Service
public class TreatmentRecommendationEngine {

    private static final Logger logger = LoggerFactory.getLogger(TreatmentRecommendationEngine.class);

    @Autowired
    private MedicalAnalysisService analysisService;

    @Autowired
    private UserMedicalContextService medicalContextService;


    public TreatmentPlan analyzeSituation(UUID userId, Set<ExtractedSymptom> symptoms) {
        logger.info("Starting medical analysis for user {} with {} symptoms", userId, symptoms.size());

        try {
            // שלב 1: טעינת המידע הרפואי של המשתמש
            UserMedicalContext userContext = medicalContextService.getUserMedicalContext(userId);
            logger.debug("Loaded user medical context: {} medications, {} diseases, {} risk factors",
                    userContext.getCurrentMedications().size(),
                    userContext.getActiveDiseases().size(),
                    userContext.getRiskFactors().size());

            // שלב 2: Community Detection - עם טיפול בשגיאות
            List<MedicalAnalysisService.MedicalCommunity> medicalCommunities = new ArrayList<>();
            try {
                medicalCommunities = analysisService.detectMedicalCommunities(getAllUserEntities(userContext));
                logger.debug("Loaded {} medical communities", medicalCommunities.size());
            } catch (Exception e) {
                logger.warn("Community detection failed: {}", e.getMessage());
                // ממשיכים עם רשימה ריקה
            }

            // שלב 3: חיפוש קשרים רפואיים - עם טיפול בשגיאות
            List<MedicalAnalysisService.MedicalPathway> detectedPathways = new ArrayList<>();
            try {
                for (UserMedicalEntity entity : getAllUserEntities(userContext)) {
                    try {
                        List<MedicalAnalysisService.MedicalPathway> pathways =
                                analysisService.findMedicalPathways(entity.getCui(), symptoms, MAX_PATH_DEPTH);
                        detectedPathways.addAll(pathways);
                    } catch (Exception e) {
                        logger.debug("Pathway analysis failed for entity {}: {}", entity.getCui(), e.getMessage());
                        // ממשיכים לישות הבאה
                    }
                }
                logger.debug("Found {} advanced pathways", detectedPathways.size());
            } catch (Exception e) {
                logger.warn("Pathway analysis completely failed: {}", e.getMessage());
            }

            // שלב 4: חישוב הסיכונים - עם טיפול בשגיאות
            MedicalAnalysisService.RiskPropagationResult riskPropagation = null;
            try {
                riskPropagation = analysisService.calculateRiskPropagation(userContext.getRiskFactors(), symptoms, RISK_DECAY_FACTOR);
                logger.info("Risk propagation analysis complete. Total risk: {:.3f}",
                        riskPropagation.getTotalRiskScore());
            } catch (Exception e) {
                logger.warn("Risk propagation analysis failed: {}", e.getMessage());
                // יצירת תוצאה בסיסית
                riskPropagation = createFallbackRiskPropagation();
            }


            // שלב 6: קשרים בסיסיים
            List<MedicalConnection> basicConnections = new ArrayList<>();
            try {
                basicConnections = findBasicConnections(userContext, new ArrayList<>(symptoms));
                logger.debug("Found {} basic connections", basicConnections.size());
            } catch (Exception e) {
                logger.error("Even basic connections failed: {}", e.getMessage());
                // זה באמת בעייתי, אבל נמשיך
            }

            // שלב 7: קביעת רמת הדחיפות
            TreatmentPlan.UrgencyLevel urgencyLevel;
            try {
                urgencyLevel = calculateUrgencyLevel(symptoms, detectedPathways, riskPropagation, medicalCommunities, userContext);
                logger.debug("Urgency level: {}", urgencyLevel);
            } catch (Exception e) {
                logger.error("Urgency calculation failed: {}", e.getMessage());
                // ברירת מחדל בטוחה
                urgencyLevel = TreatmentPlan.UrgencyLevel.MEDIUM;
            }

            // שלב 8: קביעת תוכנית טיפול
            TreatmentPlan treatmentPlan;
            try {
                treatmentPlan = buildTreatmentPlan(
                        urgencyLevel, userContext, symptoms, detectedPathways, riskPropagation, medicalCommunities, basicConnections);
                logger.debug("Treatment plan created successfully!");
            } catch (Exception e) {
                logger.error("Treatment plan building failed: {}", e.getMessage());
                // יצירת תכנית חירום
                treatmentPlan = createEmergencyPlan(symptoms);
                // אבל נוודא שיש לפחות דאגה עיקרית
                if (treatmentPlan.getMainConcern() == null || treatmentPlan.getMainConcern().isEmpty()) {
                    treatmentPlan.setMainConcern(generateFallbackMainConcern(symptoms, userContext, basicConnections));
                }
            }

            // בדיקה אחרונה שיש דאגה עיקרית
            if (treatmentPlan.getMainConcern() == null || treatmentPlan.getMainConcern().isEmpty()) {
                treatmentPlan.setMainConcern(generateFallbackMainConcern(symptoms, userContext, basicConnections));
            }

            return treatmentPlan;

        } catch (Exception e) {
            logger.error("Error in medical analysis for user {}", userId, e);
            TreatmentPlan emergencyPlan = createEmergencyPlan(symptoms);
            // וודא שיש דאגה עיקרית גם בחירום
            if (emergencyPlan.getMainConcern() == null || emergencyPlan.getMainConcern().isEmpty()) {
                emergencyPlan.setMainConcern("🚨 Emergency-level medical situation identified through advanced graph analysis. This analysis combines traditional medical knowledge with advanced graph-based pattern recognition for comprehensive assessment. Recommendations are based on your complete medical profile and interconnection patterns.");
            }
            return emergencyPlan;
        }
    }

    //חישוב רמת הדחיפות
    private TreatmentPlan.UrgencyLevel calculateUrgencyLevel(Set<ExtractedSymptom> symptoms,
                                                             List<MedicalAnalysisService.MedicalPathway> pathways,
                                                             MedicalAnalysisService.RiskPropagationResult riskPropagation,
                                                             List<MedicalAnalysisService.MedicalCommunity> medicalCommunities,
                                                             UserMedicalContext userContext) {
        logger.debug("Calculating urgency level based on symptoms, pathways, risk propagation, and medical communities");

        //בדיקת סימפטומים דחופים
        if (hasEmergencySymptoms(symptoms)) {
            logger.debug("Emergency symptoms found");
            return TreatmentPlan.UrgencyLevel.EMERGENCY;
        }

        double urgencyScore = 0.0;
        double riskScore = Math.min(1.0, riskPropagation.getTotalRiskScore() / symptoms.size());
        urgencyScore += riskScore * 0.4;
        logger.debug("Risk propagation contribution: {:.3f}", riskScore * 0.4);

        // חישוב לפי המסלולים
        double pathwayScore = pathways.stream()
                .mapToDouble(p -> p.getRiskScore() * p.getConfidence())
                .max().orElse(0.0);
        urgencyScore += Math.min(1.0, pathwayScore) * 0.3;
        logger.debug("Advanced pathways contribution: {:.3f}", Math.min(1.0, pathwayScore) * 0.3);

        // חישוב מהקהילות הרפואיות (קהילות גדולות = בעיה מורכבת יותר)
        double communityScore = medicalCommunities.stream()
                .mapToDouble(c -> c.getCohesionScore() * (c.getSize() / 10.0))
                .max().orElse(0.0);
        urgencyScore += Math.min(1.0, communityScore) * 0.2;
        logger.debug("Medical communities contribution: {:.3f}", Math.min(1.0, communityScore) * 0.2);

        //ציון גורמי סיכון כלליים
        double userRiskScore = userContext.getOverallRiskScore() != null ? userContext.getOverallRiskScore() : 0.0;
        urgencyScore += userRiskScore * 0.1;
        logger.debug("User risk factors contribution: {:.3f}", userRiskScore * 0.1);

        logger.info("Final urgency score: {:.3f}", urgencyScore);

        // החלטה על רמת דחיפות
        if (urgencyScore > HIGH_URGENCY_THRESHOLD) return TreatmentPlan.UrgencyLevel.HIGH;
        if (urgencyScore > MEDIUM_URGENCY_THRESHOLD) return TreatmentPlan.UrgencyLevel.MEDIUM;
        return TreatmentPlan.UrgencyLevel.LOW;
    }

    private String determineMainDiagnosis(List<MedicalAnalysisService.MedicalPathway> pathways,
                                          List<MedicalAnalysisService.MedicalCommunity> communities) {
        // 1. בדיקת מסלולים מסוכנים
        if (pathways != null && !pathways.isEmpty()) {
            Optional<MedicalAnalysisService.MedicalPathway> mostRiskyPathway = pathways.stream()
                    .max(Comparator.comparingDouble(p -> p.getRiskScore() * p.getConfidence()));
            if (mostRiskyPathway.isPresent()) {
                MedicalAnalysisService.MedicalPathway pathway = mostRiskyPathway.get();
                return String.format("High-risk medical pathway detected: %s (Risk: %.2f, Confidence: %.2f)",
                        pathway.getExplanation(), pathway.getRiskScore(), pathway.getConfidence());
            }
        }

        // 2. בדיקת קהילות גדולות
        if (communities != null && !communities.isEmpty()) {
            Optional<MedicalAnalysisService.MedicalCommunity> largestCommunity = communities.stream()
                    .max(Comparator.comparingInt(MedicalAnalysisService.MedicalCommunity::getSize));
            if (largestCommunity.isPresent() && largestCommunity.get().getSize() > MIN_COMMUNITY_SIZE) {
                return String.format("Complex medical interaction detected in community of %d entities (%s)",
                        largestCommunity.get().getSize(), largestCommunity.get().getDominantType());
            }
        }

        // 3. fallback - ברירת מחדל עם הסבר מתקדם
        return "🚨 Emergency-level medical situation identified through advanced graph analysis. This analysis combines traditional medical knowledge with advanced graph-based pattern recognition for comprehensive assessment. Recommendations are based on your complete medical profile and interconnection patterns.";
    }

    // פונקציה חדשה ליצירת דאגה עיקרית כ-fallback
    private String generateFallbackMainConcern(Set<ExtractedSymptom> symptoms, UserMedicalContext userContext, List<MedicalConnection> basicConnections) {

        // בדוק תופעות לוואי מתרופות
        if (basicConnections != null) {
            Optional<MedicalConnection> sideEffect = basicConnections.stream()
                    .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.SIDE_EFFECT)
                    .max(Comparator.comparingDouble(MedicalConnection::getConfidence));

            if (sideEffect.isPresent()) {
                return String.format("Suspected side effect of medication %s", sideEffect.get().getFromEntity());
            }

            // בדוק קשרי מחלות
            Optional<MedicalConnection> diseaseConnection = basicConnections.stream()
                    .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.DISEASE_SYMPTOM)
                    .max(Comparator.comparingDouble(MedicalConnection::getConfidence));

            if (diseaseConnection.isPresent()) {
                return String.format("Symptoms may be related to existing condition: %s", diseaseConnection.get().getFromEntity());
            }
        }

        // אם יש מידע רפואי קיים
        if (userContext != null) {
            if (userContext.getCurrentMedications().size() > 0 || userContext.getActiveDiseases().size() > 0) {
                return String.format("Analysis of %d reported symptoms considering your medical history (%d medications, %d conditions)",
                        symptoms.size(), userContext.getCurrentMedications().size(), userContext.getActiveDiseases().size());
            }
        }

        // ברירת מחדל אחרונה
        return "🚨 Emergency-level medical situation identified through advanced graph analysis. This analysis combines traditional medical knowledge with advanced graph-based pattern recognition for comprehensive assessment. Recommendations are based on your complete medical profile and interconnection patterns.";
    }

    private TreatmentPlan buildTreatmentPlan(
            TreatmentPlan.UrgencyLevel urgencyLevel,
            UserMedicalContext userContext,
            Set<ExtractedSymptom> symptoms,
            List<MedicalAnalysisService.MedicalPathway> pathways,
            MedicalAnalysisService.RiskPropagationResult riskPropagation,
            List<MedicalAnalysisService.MedicalCommunity> communities,
            List<MedicalConnection> basicConnections) {

        TreatmentPlan plan = new TreatmentPlan();
        plan.setUrgencyLevel(urgencyLevel);

        // דאגה עיקרית - עם fallback
        try {
            String mainConcern = determineMainDiagnosis(pathways, communities);
            if (mainConcern == null || mainConcern.trim().isEmpty()) {
                mainConcern = generateFallbackMainConcern(symptoms, userContext, basicConnections);
            }
            plan.setMainConcern(mainConcern);
        } catch (Exception e) {
            logger.error("Error determining main concern: {}", e.getMessage());
            plan.setMainConcern(generateFallbackMainConcern(symptoms, userContext, basicConnections));
        }

        // הסבר מפורט עם תובנות גרף
        try {
            plan.setReasoning(buildReasoning(urgencyLevel, pathways, riskPropagation, communities));
        } catch (Exception e) {
            logger.error("Error building reasoning: {}", e.getMessage());
            plan.setReasoning("Medical analysis completed with consideration of available data. Recommendations are based on reported symptoms and available medical information.");
        }

        // פעולות מיידיות מתקדמות
        try {
            plan.setImmediateActions(generateImmediateActions(urgencyLevel, pathways, userContext));
        } catch (Exception e) {
            logger.error("Error generating immediate actions: {}", e.getMessage());
            plan.setImmediateActions(generateBasicImmediateActions(urgencyLevel));
        }

        // בדיקות מומלצות בהתבסס על Graph Analytics
        try {
            plan.setRecommendedTests(generateRecommendedTests(symptoms, pathways, communities, urgencyLevel, userContext));        } catch (Exception e) {
            logger.error("Error generating recommended tests: {}", e.getMessage());
            plan.setRecommendedTests(generateBasicTests(symptoms, urgencyLevel, userContext));
        }

        // ביקורי רופא מותאמים
        try {
            plan.setDoctorVisits(generateDoctorVisits(urgencyLevel, communities));
        } catch (Exception e) {
            logger.error("Error generating doctor visits: {}", e.getMessage());
            plan.setDoctorVisits(generateBasicDoctorVisits(urgencyLevel));
        }

        // שילוב קשרים בסיסיים וגרף מתקדם
        List<MedicalConnection> allConnections = new ArrayList<>();
        if (basicConnections != null) {
            allConnections.addAll(basicConnections);
        }
        if (pathways != null) {
            try {
                allConnections.addAll(convertPathwaysToConnections(pathways));
            } catch (Exception e) {
                logger.warn("Error converting pathways to connections: {}", e.getMessage());
            }
        }
        plan.setFoundConnections(allConnections);

        // מידע נוסף מקיף עם Graph Insights
        try {
            plan.setAdditionalInfo(buildAdditionalInfo(userContext, pathways, riskPropagation, communities));
        } catch (Exception e) {
            logger.error("Error building additional info: {}", e.getMessage());
            plan.setAdditionalInfo(Map.of("error", "Some analysis components failed", "timestamp", System.currentTimeMillis()));
        }

        return plan;
    }

    // פונקציות fallback בסיסיות
    private List<ImmediateAction> generateBasicImmediateActions(TreatmentPlan.UrgencyLevel urgencyLevel) {
        List<ImmediateAction> actions = new ArrayList<>();

        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            ImmediateAction emergency = new ImmediateAction();
            emergency.setType(ImmediateAction.ActionType.CALL_EMERGENCY);
            emergency.setDescription("Call emergency services immediately!");
            emergency.setReason("Emergency symptoms detected");
            emergency.setPriority(1);
            actions.add(emergency);
        } else {
            ImmediateAction monitor = new ImmediateAction();
            monitor.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
            monitor.setDescription("Monitor symptoms and seek medical consultation");
            monitor.setReason("Medical evaluation recommended");
            monitor.setPriority(1);
            actions.add(monitor);
        }

        return actions;
    }

    private List<MedicalTest> generateBasicTests(Set<ExtractedSymptom> symptoms, TreatmentPlan.UrgencyLevel urgencyLevel, UserMedicalContext userContext) {        List<MedicalTest> tests = new ArrayList<>();

        MedicalTest basicTest = new MedicalTest();
        basicTest.setType(MedicalTest.TestType.BLOOD_TEST);
        basicTest.setDescription("Basic blood work");
        basicTest.setReason("General health assessment");
        basicTest.setUrgency(urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY ? "ASAP" : "Within week");
        tests.add(basicTest);

        return tests;
    }

    private List<DoctorVisit> generateBasicDoctorVisits(TreatmentPlan.UrgencyLevel urgencyLevel) {
        List<DoctorVisit> visits = new ArrayList<>();

        DoctorVisit visit = new DoctorVisit();
        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            visit.setType(DoctorVisit.DoctorType.EMERGENCY_ROOM);
            visit.setUrgency("Immediately");
        } else {
            visit.setType(DoctorVisit.DoctorType.FAMILY_DOCTOR);
            visit.setUrgency("Within few days");
        }
        visit.setReason("Medical evaluation required");
        visits.add(visit);

        return visits;
    }

    private MedicalAnalysisService.RiskPropagationResult createFallbackRiskPropagation() {
        MedicalAnalysisService.RiskPropagationResult result = new MedicalAnalysisService.RiskPropagationResult();
        result.setSymptomRiskScores(new HashMap<>());
        result.setPropagationPaths(new ArrayList<>());
        result.setTotalRiskScore(0.0);
        return result;
    }

    //בניית הסבר למשתמש
    private String buildReasoning(TreatmentPlan.UrgencyLevel urgencyLevel,
                                  List<MedicalAnalysisService.MedicalPathway> pathways,
                                  MedicalAnalysisService.RiskPropagationResult riskPropagation,
                                  List<MedicalAnalysisService.MedicalCommunity> communities) {
        StringBuilder reasoning = new StringBuilder();

        //הסבר בסיסי על רמת הדחיפות
        switch (urgencyLevel) {
            case EMERGENCY ->
                    reasoning.append("🚨 Emergency-level medical situation identified through advanced graph analysis. ");
            case HIGH -> reasoning.append("⚠️ High-priority medical concerns detected via multi-pathway analysis. ");
            case MEDIUM ->
                    reasoning.append("📊 Moderate medical concerns identified through graph-based pattern detection. ");
            case LOW -> reasoning.append("ℹ️ Medical patterns detected that warrant monitoring and follow-up. ");
        }

        //תובנות ספציפיות
        if (!pathways.isEmpty()) {
            int significantPathways = (int) pathways.stream()
                    .filter(p -> p.getRiskScore() > MIN_PATHWAY_CONFIDENCE)
                    .count();
            reasoning.append(String.format("Advanced pathway analysis revealed %d significant medical pathways. ", significantPathways));
        }

        if (riskPropagation.getTotalRiskScore() > MIN_RISK_PROPAGATION) {
            reasoning.append(String.format("Risk propagation analysis indicates elevated total risk (%.2f). ", riskPropagation.getTotalRiskScore()));
        }

        if (!communities.isEmpty()) {
            reasoning.append(String.format("Medical community analysis identified %d interconnected groups of conditions. ", communities.size()));
        }

        reasoning.append("This analysis combines traditional medical knowledge with advanced graph-based pattern recognition for comprehensive assessment. ");
        reasoning.append("Recommendations are based on your complete medical profile and interconnection patterns.");

        return reasoning.toString();
    }

    //פעולות מיידיות
    private List<ImmediateAction> generateImmediateActions(TreatmentPlan.UrgencyLevel urgencyLevel,
                                                           List<MedicalAnalysisService.MedicalPathway> pathways,
                                                           UserMedicalContext context) {
        List<ImmediateAction> actions = new ArrayList<>();

        //פעולות דחופות
        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            ImmediateAction emergency = new ImmediateAction();
            emergency.setType(ImmediateAction.ActionType.CALL_EMERGENCY);
            emergency.setDescription("Call emergency services immediately!");
            emergency.setReason("Critical medical pathways detected through graph analysis");
            emergency.setPriority(1);
            actions.add(emergency);
        }


        //פעולות בהתבסס על מסלולים
        pathways.stream()
                .filter(p -> p.getRiskScore() > MIN_PATHWAY_CONFIDENCE)
                .limit(2)
                .forEach(pathway -> {
                    ImmediateAction pathwayAction = new ImmediateAction();
                    pathwayAction.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
                    pathwayAction.setDescription(String.format("Monitor pathway: %s", pathway.getTargetName()));
                    pathwayAction.setReason(String.format("High-risk pathway detected (Risk: %.2f)", pathway.getRiskScore()));
                    pathwayAction.setPriority(3);
                    actions.add(pathwayAction);
                });

        // פעולה כללית של מעקב
        if (urgencyLevel != TreatmentPlan.UrgencyLevel.EMERGENCY) {
            ImmediateAction monitor = new ImmediateAction();
            monitor.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
            monitor.setDescription("Monitor symptoms and track any changes using graph-based pattern recognition");
            monitor.setReason("Comprehensive medical pattern analysis suggests careful monitoring");
            monitor.setPriority(4);
            actions.add(monitor);
        }

        return actions;
    }

    //המרת מסלולים לקשרים רפואיים
    private List<MedicalConnection> convertPathwaysToConnections(List<MedicalAnalysisService.MedicalPathway> pathways) {
        return pathways.stream()
                .map(pathway -> {
                    MedicalConnection connection = new MedicalConnection();
                    connection.setType(MedicalConnection.ConnectionType.RISK_FACTOR);
                    connection.setFromEntity(pathway.getNodes().isEmpty() ? "Unknown" : pathway.getNodes().get(0).getName());
                    connection.setToEntity(pathway.getTargetName());
                    connection.setFromCui(pathway.getSourceCui());
                    connection.setToCui(pathway.getTargetCui());
                    connection.setConfidence(pathway.getConfidence());
                    connection.setExplanation(pathway.getExplanation());
                    return connection;
                }).collect(Collectors.toList());
    }

    private List<UserMedicalEntity> getAllUserEntities(UserMedicalContext context) {
        List<UserMedicalEntity> entities = new ArrayList<>();
        entities.addAll(context.getCurrentMedications());
        entities.addAll(context.getActiveDiseases());
        entities.addAll(context.getRiskFactors());
        return entities;
    }

    private List<MedicalConnection> findBasicConnections(UserMedicalContext context, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> connections = new ArrayList<>();
        try {
            connections.addAll(analysisService.findDiseaseSymptoms(context.getActiveDiseases(), symptoms));
            connections.addAll(analysisService.findMedicationSideEffects(context.getCurrentMedications(), symptoms));
            connections.addAll(analysisService.findPossibleTreatments(symptoms));
        } catch (Exception e) {
            logger.warn("Error finding basic connections: {}", e.getMessage(), e);
        }
        return connections;
    }

    //סימפטומים דחופים
    private boolean hasEmergencySymptoms(Set<ExtractedSymptom> symptoms) {
        Set<String> emergencyKeywords = Set.of(
                "chest pain", "difficulty breathing", "severe pain", "bleeding",
                "unconscious", "seizure", "stroke", "heart attack"
        );

        return symptoms.stream()
                .anyMatch(symptom -> emergencyKeywords.stream()
                        .anyMatch(keyword -> symptom.getName().toLowerCase().contains(keyword)));
    }

    private List<MedicalTest> generateRecommendedTests(Set<ExtractedSymptom> symptoms,
                                                       List<MedicalAnalysisService.MedicalPathway> pathways,
                                                       List<MedicalAnalysisService.MedicalCommunity> communities,
                                                       TreatmentPlan.UrgencyLevel urgencyLevel,
                                                       UserMedicalContext userContext) {
        Set<MedicalTest.TestType> recommendedTests = new HashSet<>();

        try {
            // חיפוש בדיקות בגרף לסימפטומים
            List<MedicalConnection> testConnections = analysisService.findRecommendedTests(new ArrayList<>(symptoms));

            for (MedicalConnection connection : testConnections) {
                if (connection.getConfidence() > 0.3) { // סף ביטחון
                    MedicalTest.TestType testType = mapTestNameToType(connection.getToEntity());
                    if (testType != null) {
                        recommendedTests.add(testType);
                    }
                }
            }

            // חיפוש בדיקות למחלות פעילות
            if (userContext != null && userContext.getActiveDiseases() != null) {
                List<MedicalConnection> diseaseTests = analysisService.findTestsForDiseases(userContext.getActiveDiseases());

                for (MedicalConnection connection : diseaseTests) {
                    if (connection.getConfidence() > 0.3) {
                        MedicalTest.TestType testType = mapTestNameToType(connection.getToEntity());
                        if (testType != null) {
                            recommendedTests.add(testType);
                        }
                    }
                }
            }

            logger.info("Found {} graph-based test recommendations", recommendedTests.size());

        } catch (Exception e) {
            logger.warn("Graph-based test search failed, using fallback: {}", e.getMessage());
            return generateBasicTests(symptoms, urgencyLevel, userContext);        }

        // אם לא נמצאו בדיקות בגרף, fallback בסיסי
        if (recommendedTests.isEmpty()) {
            return generateBasicTests(symptoms, urgencyLevel, userContext);        }

        String urgency = switch (urgencyLevel) {
            case EMERGENCY -> "ASAP";
            case HIGH -> "Within 24h";
            case MEDIUM -> "Within week";
            case LOW -> "Within month";
        };

        return recommendedTests.stream()
                .map(testType -> {
                    MedicalTest test = new MedicalTest();
                    test.setType(testType);
                    test.setDescription(testType.getDescription());
                    test.setReason("Recommended based on medical graph analysis");
                    test.setUrgency(urgency);
                    return test;
                })
                .collect(Collectors.toList());
    }

    /**
     * מיפוי שם בדיקה לסוג
     */
    private MedicalTest.TestType mapTestNameToType(String testName) {
        String name = testName.toLowerCase();

        if (name.contains("blood") || name.contains("cbc")) return MedicalTest.TestType.BLOOD_TEST;
        if (name.contains("ecg") || name.contains("ekg")) return MedicalTest.TestType.ECG;
        if (name.contains("x-ray")) return MedicalTest.TestType.XRAY;
        if (name.contains("ultrasound")) return MedicalTest.TestType.ULTRASOUND;
        if (name.contains("ct")) return MedicalTest.TestType.CT_SCAN;
        if (name.contains("mri")) return MedicalTest.TestType.MRI;
        if (name.contains("urine")) return MedicalTest.TestType.URINE_TEST;
        if (name.contains("glucose") || name.contains("sugar")) return MedicalTest.TestType.BLOOD_SUGAR;
        if (name.contains("pressure")) return MedicalTest.TestType.BLOOD_PRESSURE;

        return MedicalTest.TestType.BLOOD_TEST; // ברירת מחדל
    }

    private List<DoctorVisit> generateDoctorVisits(TreatmentPlan.UrgencyLevel urgencyLevel,
                                                   List<MedicalAnalysisService.MedicalCommunity> communities) {
        List<DoctorVisit> visits = new ArrayList<>();

        String urgency = switch (urgencyLevel) {
            case EMERGENCY -> "Emergency";
            case HIGH -> "High";
            case MEDIUM -> "Medium";
            case LOW -> "Low";
        };

        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            DoctorVisit emergency = new DoctorVisit();
            emergency.setType(DoctorVisit.DoctorType.EMERGENCY_ROOM);
            emergency.setReason("Emergency situation identified through advanced medical graph analysis");
            emergency.setUrgency(urgency);
            visits.add(emergency);
        } else {
            DoctorVisit familyDoctor = new DoctorVisit();
            familyDoctor.setType(DoctorVisit.DoctorType.FAMILY_DOCTOR);
            familyDoctor.setReason("Comprehensive evaluation recommended based on graph-based medical pattern analysis");
            familyDoctor.setUrgency(urgency);
            visits.add(familyDoctor);


        }
        return visits;
    }

    private Map<String, Object> buildAdditionalInfo(UserMedicalContext userContext,
                                                    List<MedicalAnalysisService.MedicalPathway> pathways,
                                                    MedicalAnalysisService.RiskPropagationResult riskPropagation,
                                                    List<MedicalAnalysisService.MedicalCommunity> communities) {
        Map<String, Object> info = new HashMap<>();

        //מידע בסיסי
        info.put("analysisTimestamp", System.currentTimeMillis());
        info.put("analysisType", "Advanced Graph-Based Medical Analysis");
        info.put("userRiskLevel", userContext.getRiskLevel());
        info.put("userRiskScore", userContext.getOverallRiskScore());

        //תוצאות ניתוח הגרף
        info.put("advancedPathwaysCount", pathways.size());
        info.put("medicalCommunitiesCount", communities.size());
        info.put("totalRiskPropagation", riskPropagation.getTotalRiskScore());

        //מסלולים משמעותיים
        List<String> significantPathways = pathways.stream()
                .filter(p -> p.getRiskScore() > MIN_PATHWAY_CONFIDENCE)
                .map(MedicalAnalysisService.MedicalPathway::getExplanation)
                .collect(Collectors.toList());
        info.put("significantPathways", significantPathways);

        // פירוט קהילות
        List<String> communityDescriptions = communities.stream()
                .limit(MAX_COMMUNITIES_TO_RETURN)
                .map(MedicalAnalysisService.MedicalCommunity::getDescription)
                .collect(Collectors.toList());
        info.put("medicalCommunities", communityDescriptions);

        return info;
    }

    private TreatmentPlan createEmergencyPlan(Set<ExtractedSymptom> symptoms) {
        logger.warn("Creating emergency treatment plan due to analysis error");

        TreatmentPlan emergencyPlan = new TreatmentPlan();
        emergencyPlan.setUrgencyLevel(TreatmentPlan.UrgencyLevel.MEDIUM);
        emergencyPlan.setMainConcern("Unable to complete advanced analysis - medical consultation recommended");
        emergencyPlan.setReasoning("An error occurred during advanced graph analysis. Traditional medical consultation is recommended for proper evaluation.");

        ImmediateAction action = new ImmediateAction();
        action.setType(ImmediateAction.ActionType.SEEK_IMMEDIATE_CARE);
        action.setDescription("Consult healthcare provider for proper evaluation");
        action.setReason("Advanced analysis could not be completed");
        action.setPriority(1);

        emergencyPlan.setImmediateActions(List.of(action));

        DoctorVisit visit = new DoctorVisit();
        visit.setType(DoctorVisit.DoctorType.FAMILY_DOCTOR);
        visit.setReason("Medical evaluation required");
        visit.setUrgency("Within few days");
        emergencyPlan.setDoctorVisits(List.of(visit));

        emergencyPlan.setRecommendedTests(new ArrayList<>());
        emergencyPlan.setFoundConnections(new ArrayList<>());
        emergencyPlan.setAdditionalInfo(Map.of("error", "Advanced analysis incomplete", "symptomsCount", symptoms.size()));

        return emergencyPlan;
    }

    public UserMedicalContext getUserMedicalContext(UUID userId) {
        return medicalContextService.getUserMedicalContext(userId);
    }
}