package com.example.mediaid.bl.emergency;

import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.dto.emergency.*;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TreatmentRecommendationEngine {

    private static final Logger logger = LoggerFactory.getLogger(TreatmentRecommendationEngine.class);

    @Autowired
    private Driver neo4jDriver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Neo4jPathfindingService pathfindingService;

    @Autowired
    private UserMedicalContextService medicalContextService;

    @Autowired
    private RiskFactorService riskFactorService;

    @Autowired
    private MedicalGraphAnalyticsService graphAnalytics;

    public TreatmentPlan analyzeSituation(UUID userId, Set<ExtractedSymptom> symptoms) {
        logger.info("Starting medical analysis for user {} with symptoms {}", userId, symptoms.size());

        try {
            // שלב 1: טעינת המידע הרפואי של המשתמש
            UserMedicalContext userContext = medicalContextService.getUserMedicalContext(userId);
            logger.debug("Loaded user medical context: {} medications, {} diseases, {} risk factors",
                    userContext.getCurrentMedications().size(),
                    userContext.getActiveDiseases().size(),
                    userContext.getRiskFactors().size());

            //Community Detection :שלב 2
            List<MedicalGraphAnalyticsService.MedicalCommunity> medicalCommunities =
                    graphAnalytics.detectMedicalCommunities(getAllUserEntities(userContext));
            logger.debug("Loaded medical communities: {} medications", medicalCommunities.size());

            // שלב 3: חיפוש קשרים רפואיים
            List<MedicalGraphAnalyticsService.MedicalPathway> detectedPathways = new ArrayList<>();
            for (UserMedicalEntity entity : getAllUserEntities(userContext)) {
                List<MedicalGraphAnalyticsService.MedicalPathway> pathways =
                        graphAnalytics.findMedicalPathways(entity.getCui(), symptoms, 5);
                detectedPathways.addAll(pathways);
            }
            logger.debug("Found {} advanced pathways", detectedPathways.size());

            //שלב 4: חישוב הסיכונים
            MedicalGraphAnalyticsService.RiskPropagationResult riskPropagation =
                    graphAnalytics.calculateRiskPropagation(userContext.getRiskFactors(), symptoms, 0.85);
            logger.error("Risk propagation analysis complete. Total risk: {:.3f}",
                    riskPropagation.getTotalRiskScore());

            //Medical Hub Analysis :שלב 5
            List<MedicalGraphAnalyticsService.MedicalHub> medicalHubs =
                    graphAnalytics.findMedicalHubs(getAllUserEntities(userContext));
            logger.debug("Found {} medical hubs", medicalHubs.size());

            //שלב 6: קשרים בסיסיים
            List<MedicalConnection> basicConnections = findBasicConnections(userContext, new ArrayList<>(symptoms));
            logger.debug("Found {} basic basicConnections", basicConnections.size());

            //שלב 7: קביעת רמת הדחיפות
            TreatmentPlan.UrgencyLevel urgencyLevel = calculateUrgencyLevel(
                    symptoms, detectedPathways, riskPropagation, medicalCommunities, userContext);
            logger.debug("Urgency level: {}", urgencyLevel);

            //שלב 8: קביעת תוכנית טיפול
            TreatmentPlan treatmentPlan = buildTreatmentPlan(
                    urgencyLevel, userContext, symptoms, detectedPathways, riskPropagation, medicalCommunities, medicalHubs, basicConnections);
            logger.debug("Treatment plan created successfully!");
            return treatmentPlan;

        } catch (Exception e) {
            logger.error("Error in medical analysis for user {}", userId, e);
            return createEmergencyPlan(symptoms);
        }
    }


    //חישוב רמת הדחיפות
    private TreatmentPlan.UrgencyLevel calculateUrgencyLevel(Set<ExtractedSymptom> symptoms,
                                                             List<MedicalGraphAnalyticsService.MedicalPathway> pathways,
                                                             MedicalGraphAnalyticsService.RiskPropagationResult riskPropagation,
                                                             List<MedicalGraphAnalyticsService.MedicalCommunity> medicalCommunities,
                                                             UserMedicalContext userContext) {
        logger.debug("Calculating urgency level based on symptoms, pathways, risk propagation, and medical communities");

        //בדיקת סימפטומים דחופים
        if (hasEmergencySymptoms(symptoms)) {
            logger.debug("Emergency symptoms found");
            return TreatmentPlan.UrgencyLevel.EMERGENCY;
        }

        //
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

        // חישוב מהקהילות הרפואיות (קהילות גדולות  = בעיה מורכבת יותר)
        double communityScore = medicalCommunities.stream()
                .mapToDouble(c -> c.getCohesionScore() * (c.getSize() / 10.0))
                .max().orElse(0.0);
        urgencyScore += Math.min(1.0, communityScore) * 0.2;
        logger.debug("Medical communities contribution: {:.3f}", Math.min(1.0, communityScore) * 0.2);

        //ציון גומי סיכון כלליים
        double userRiskScore = userContext.getOverallRiskScore() != null ? userContext.getOverallRiskScore() : 0.0;
        urgencyScore += userRiskScore * 0.1;
        logger.debug("User risk factors contribution: {:.3f}", userRiskScore * 0.1);

        logger.info("Final urgency score: {:.3f}", urgencyScore);

        // החלטה על רמת דחיפות
        if (urgencyScore > 0.8) return TreatmentPlan.UrgencyLevel.HIGH;
        if (urgencyScore > 0.5) return TreatmentPlan.UrgencyLevel.MEDIUM;
        return TreatmentPlan.UrgencyLevel.LOW;
    }

    private TreatmentPlan buildTreatmentPlan(
            TreatmentPlan.UrgencyLevel urgencyLevel,
            UserMedicalContext userContext,
            Set<ExtractedSymptom> symptoms,
            List<MedicalGraphAnalyticsService.MedicalPathway> pathways,
            MedicalGraphAnalyticsService.RiskPropagationResult riskPropagation,
            List<MedicalGraphAnalyticsService.MedicalCommunity> communities,
            List<MedicalGraphAnalyticsService.MedicalHub> hubs,
            List<MedicalConnection> basicConnections) {

        TreatmentPlan plan = new TreatmentPlan();
        plan.setUrgencyLevel(urgencyLevel);

        // דאגה עיקרית מבוססת Graph Insights
        plan.setMainConcern(determineMainDiagnosis(pathways, communities, hubs, symptoms));

        // הסבר מפורט עם תובנות גרף
        plan.setReasoning(buildReasoning(urgencyLevel, pathways, riskPropagation, communities, hubs));

        // פעולות מיידיות מתקדמות
        plan.setImmediateActions(generateImmediateActions(urgencyLevel, pathways, hubs, userContext));

        // בדיקות מומלצות בהתבסס על Graph Analytics
        plan.setRecommendedTests(generateRecommendedTests(symptoms, pathways, communities, urgencyLevel));

        // ביקורי רופא מותאמים
        plan.setDoctorVisits(generateDoctorVisits(urgencyLevel, communities, hubs));

        // שילוב קשרים בסיסיים וגרף מתקדם
        List<MedicalConnection> allConnections = new ArrayList<>(basicConnections);
        allConnections.addAll(convertPathwaysToConnections(pathways));
        plan.setFoundConnections(allConnections);

        // מידע נוסף מקיף עם Graph Insights
        plan.setAdditionalInfo(buildAdditionalInfo(userContext, pathways, riskPropagation, communities, hubs));

        return plan;
    }

    //קביעת הדאגה העיקרית
    private String determineMainDiagnosis(List<MedicalGraphAnalyticsService.MedicalPathway> pathways,
                                          List<MedicalGraphAnalyticsService.MedicalCommunity> communities,
                                          List<MedicalGraphAnalyticsService.MedicalHub> hubs,
                                          Set<ExtractedSymptom> symptoms) {
        //המסלול המסוכן ביותר
        Optional<MedicalGraphAnalyticsService.MedicalPathway> mostRiskyPathway = pathways.stream()
                .max(Comparator.comparingDouble(p -> p.getRiskScore() * p.getConfidence()));
        if (mostRiskyPathway.isPresent()) {
            MedicalGraphAnalyticsService.MedicalPathway pathway = mostRiskyPathway.get();
            return String.format("High-risk medical pathway detected: %s (Risk: %.2f, Confidence: %.2f)",
                    pathway.getExplanation(), pathway.getRiskScore(), pathway.getConfidence());
        }

        //אם אין מסלול - קהילות גדולות
        Optional<MedicalGraphAnalyticsService.MedicalCommunity> largestCommunity = communities.stream()
                .max(Comparator.comparingInt(MedicalGraphAnalyticsService.MedicalCommunity::getSize));
        if (largestCommunity.isPresent() && largestCommunity.get().getSize() > 3) {
            return String.format("Complex medical interaction detected in community of %d entities (%s)",
                    largestCommunity.get().getSize(), largestCommunity.get().getDominantType());
        }
        return "Analysis of reported symptoms with advanced graph-based medical insights";
    }


    //בניית הסבר למשתמש
    private String buildReasoning(TreatmentPlan.UrgencyLevel urgencyLevel,
                                  List<MedicalGraphAnalyticsService.MedicalPathway> pathways,
                                  MedicalGraphAnalyticsService.RiskPropagationResult riskPropagation,
                                  List<MedicalGraphAnalyticsService.MedicalCommunity> communities,
                                  List<MedicalGraphAnalyticsService.MedicalHub> hubs) {
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
                    .filter(p -> p.getRiskScore() > 0.5)
                    .count();
            reasoning.append(String.format("Advanced pathway analysis revealed %d significant medical pathways. ", significantPathways));
        }

        if (riskPropagation.getTotalRiskScore() > 0.3) {
            reasoning.append(String.format("Risk propagation analysis indicates elevated total risk (%.2f). ", riskPropagation.getTotalRiskScore()));
        }

        if (!communities.isEmpty()) {
            reasoning.append(String.format("Medical community analysis identified %d interconnected groups of conditions. ", communities.size()));
        }

        if (!hubs.isEmpty()) {
            Optional<MedicalGraphAnalyticsService.MedicalHub> topHub = hubs.stream()
                    .max(Comparator.comparingDouble(MedicalGraphAnalyticsService.MedicalHub::getCentralityScore));
            if (topHub.isPresent() && topHub.get().getCentralityScore() > 10) {
                reasoning.append(String.format("Key medical hub identified: %s (influence: %s). ",
                        topHub.get().getName(), topHub.get().getInfluenceLevel()));
            }
        }

        reasoning.append("This analysis combines traditional medical knowledge with advanced graph-based pattern recognition for comprehensive assessment. ");
        reasoning.append("Recommendations are based on your complete medical profile and interconnection patterns.");

        return reasoning.toString();
    }

    //פעולות מיידיות
    private List<ImmediateAction> generateImmediateActions(TreatmentPlan.UrgencyLevel urgencyLevel,
                                                           List<MedicalGraphAnalyticsService.MedicalPathway> pathways,
                                                           List<MedicalGraphAnalyticsService.MedicalHub> hubs,
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

        // פעולות בהתבסס על Medical Hubs
        for (MedicalGraphAnalyticsService.MedicalHub hub : hubs) {
            if (hub.getCentralityScore() > 20 && "Medications".equals(hub.getType())) {
                ImmediateAction hubAction = new ImmediateAction();
                hubAction.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
                hubAction.setDescription(String.format("Monitor for effects related to %s", hub.getName()));
                hubAction.setReason(String.format("High-influence medication hub detected (%s influence)", hub.getInfluenceLevel()));
                hubAction.setPriority(2);
                hubAction.setMedicationName(hub.getName());
                actions.add(hubAction);
            }
        }

        //פעולות בהתבסס על מסלולים
        pathways.stream()
                .filter(p -> p.getRiskScore() > 0.7)
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
    private List<MedicalConnection> convertPathwaysToConnections(List<MedicalGraphAnalyticsService.MedicalPathway> pathways) {
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
            connections.addAll(pathfindingService.findDiseaseSymptoms(context.getActiveDiseases(), symptoms));
            connections.addAll(pathfindingService.findMedicationSideEffects(context.getCurrentMedications(), symptoms));
            connections.addAll(pathfindingService.findPossibleTreatments(symptoms));
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
                                                       List<MedicalGraphAnalyticsService.MedicalPathway> pathways,
                                                       List<MedicalGraphAnalyticsService.MedicalCommunity> communities,
                                                       TreatmentPlan.UrgencyLevel urgencyLevel) {
        Set<MedicalTest.TestType> recommendedTests = new HashSet<>();

        // בדיקות על בסיס סימפטומים
        for (ExtractedSymptom symptom : symptoms) {
            String symptomName = symptom.getName().toLowerCase();
            if (symptomName.contains("chest pain")) {
                recommendedTests.add(MedicalTest.TestType.ECG);
                recommendedTests.add(MedicalTest.TestType.BLOOD_TEST);
            } else if (symptomName.contains("heart")) {
                recommendedTests.add(MedicalTest.TestType.ECG);
            } else if (symptomName.contains("fever")) {
                recommendedTests.add(MedicalTest.TestType.BLOOD_TEST);
                recommendedTests.add(MedicalTest.TestType.BLOOD_TEST);
            } else if (symptomName.contains("headache")) {
                recommendedTests.add(MedicalTest.TestType.BLOOD_PRESSURE);
            } else if (symptomName.contains("pain")) {
                recommendedTests.add(MedicalTest.TestType.BLOOD_TEST);
            }
        }

        // הוספת בדיקות מותאמות לגורמי סיכון
        for (ExtractedSymptom symptom : symptoms) {
            if (symptom.getName().toLowerCase().contains("chest pain")) {
                recommendedTests.add(MedicalTest.TestType.ECG);
                recommendedTests.add(MedicalTest.TestType.BLOOD_TEST);
            }
            if (symptom.getName().toLowerCase().contains("fever")) {
                recommendedTests.add(MedicalTest.TestType.BLOOD_TEST);
            }
        }

        // בדיקות נוספות בהתבסס על מסלולים מתקדמים
        pathways.stream()
                .filter(p -> p.getRiskScore() > 0.6)
                .forEach(pathway -> {
                    // לוגיקה להמלצת בדיקות בהתבסס על סוג המסלול
                    if (pathway.getNodes().stream().anyMatch(n -> "Disease".equals(n.getType()))) {
                        recommendedTests.add(MedicalTest.TestType.BLOOD_TEST);
                    }
                });

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
                    test.setReason("Recommended based on advanced graph analysis and symptom patterns");
                    test.setUrgency(urgency);
                    return test;
                })
                .collect(Collectors.toList());
    }

    private List<DoctorVisit> generateDoctorVisits(TreatmentPlan.UrgencyLevel urgencyLevel,
                                                   List<MedicalGraphAnalyticsService.MedicalCommunity> communities,
                                                   List<MedicalGraphAnalyticsService.MedicalHub> hubs) {
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

            // המלצות  בהתבסס על Medical Hubs
            hubs.stream()
                    .filter(hub -> hub.getCentralityScore() > 15)
                    .limit(2)
                    .forEach(hub -> {
                        DoctorVisit specialist = new DoctorVisit();
                        specialist.setType(DoctorVisit.DoctorType.CARDIOLOGIST); // דוגמה - צריך לוגיקה מתקדמת יותר**************
                        specialist.setReason(String.format("Specialist consultation recommended due to high-influence medical factor: %s", hub.getName()));
                        specialist.setUrgency("Within 1-2 weeks");
                        visits.add(specialist);
                    });
        }
        return visits;
    }

    private Map<String, Object> buildAdditionalInfo(UserMedicalContext userContext,
                                                    List<MedicalGraphAnalyticsService.MedicalPathway> pathways,
                                                    MedicalGraphAnalyticsService.RiskPropagationResult riskPropagation,
                                                    List<MedicalGraphAnalyticsService.MedicalCommunity> communities,
                                                    List<MedicalGraphAnalyticsService.MedicalHub> hubs) {
        Map<String, Object> info = new HashMap<>();

        //מידע בסיסי
        info.put("analysisTimestamp", System.currentTimeMillis());
        info.put("analysisType", "Advanced Graph-Based Medical Analysis");
        info.put("userRiskLevel", userContext.getRiskLevel());
        info.put("userRiskScore", userContext.getOverallRiskScore());

        //תוצאות ניתוח הגרף
        info.put("advancedPathwaysCount", pathways.size());
        info.put("medicalCommunitiesCount", communities.size());
        info.put("medicalHubsCount", hubs.size());
        info.put("totalRiskPropagation", riskPropagation.getTotalRiskScore());

        //מסלולים משמעותיים
        List<String> significantPathways = pathways.stream()
                .filter(p -> p.getRiskScore() > 0.5)
                .map(MedicalGraphAnalyticsService.MedicalPathway::getExplanation)
                .collect(Collectors.toList());
        info.put("significantPathways", significantPathways);

        // פירוט Medical Hubs
        List<String> topHubs = hubs.stream()
                .filter(h -> h.getCentralityScore() > 10)
                .limit(3)
                .map(h -> h.getName() + " (" + h.getInfluenceLevel() + ")")
                .collect(Collectors.toList());
        info.put("topMedicalHubs", topHubs);

        // פירוט קהילות
        List<String> communityDescriptions = communities.stream()
                .limit(3)
                .map(MedicalGraphAnalyticsService.MedicalCommunity::getDescription)
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
