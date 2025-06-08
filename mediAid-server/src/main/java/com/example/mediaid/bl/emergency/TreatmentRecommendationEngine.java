package com.example.mediaid.bl.emergency;

import com.example.mediaid.bl.neo4j.RiskFactorSer;
import com.example.mediaid.dal.UserRepository;
import com.example.mediaid.dto.emergency.*;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * מנוע המלצות טיפול מעודכן - כולל גורמי סיכון מקיפים
 */
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
    private RiskFactorSer riskFactorSer;

    /**
     * הפונקציה המרכזית - ניתוח מצב רפואי מקיף
     */
    public TreatmentPlan analyzeSituation(UUID userId, Set<ExtractedSymptom> symptoms) {
        logger.info("Starting comprehensive medical analysis for user {} with {} symptoms", userId, symptoms.size());

        try {
            // שלב 1: טעינת המידע הרפואי של המשתמש
            UserMedicalContext userContext = medicalContextService.getUserMedicalContext(userId);
            logger.debug("Loaded user medical context: {} medications, {} diseases, {} risk factors",
                    userContext.getCurrentMedications().size(),
                    userContext.getActiveDiseases().size(),
                    userContext.getRiskFactors().size());

            // שלב 2: עדכון גורמי סיכון דינמיים ב-Neo4j
            updateUserRiskFactorsInNeo4j(userId, userContext);

            // שלב 3: חיפוש קשרים רפואיים
            List<MedicalConnection> allConnections = findAllMedicalConnections(userContext, new ArrayList<>(symptoms));
            logger.info("Found {} medical connections (including risk factors)", allConnections.size());

            // שלב 4: ניתוח השפעת גורמי הסיכון על הסימפטומים
            List<MedicalConnection> riskFactorConnections = analyzeRiskFactorImpact(userContext, symptoms);
            allConnections.addAll(riskFactorConnections);
            logger.info("Added {} risk factor connections", riskFactorConnections.size());

            // שלב 5: קביעת רמת הדחיפות
            TreatmentPlan.UrgencyLevel urgencyLevel = calculateUrgencyLevelWithRiskFactors(
                    allConnections, symptoms, userContext);
            logger.info("Calculated urgency level: {} (considering risk factors)", urgencyLevel);

            // שלב 6: יצירת תכנית הטיפול המקיפה
            TreatmentPlan treatmentPlan = buildComprehensiveTreatmentPlan(
                    urgencyLevel, allConnections, symptoms, userContext);

            logger.info("Comprehensive treatment plan created for user {}", userId);
            return treatmentPlan;

        } catch (Exception e) {
            logger.error("Error in comprehensive medical analysis for user {}: {}", userId, e.getMessage(), e);
            return createEmergencyPlan(symptoms);
        }
    }

    /**
     * עדכון גורמי סיכון של המשתמש ב-Neo4j
     */
    private void updateUserRiskFactorsInNeo4j(UUID userId, UserMedicalContext userContext) {
        try {
            Map<String, Double> userRiskFactors = new HashMap<>();

            // המרת גורמי סיכון לערכים מספריים
            if (userContext.getBasicInfo() != null) {
                // גיל
                if (userContext.getBasicInfo().get("age") != null) {
                    userRiskFactors.put("AGE", ((Integer) userContext.getBasicInfo().get("age")).doubleValue());
                }

                // BMI
                if (userContext.getBasicInfo().get("bmi") != null) {
                    userRiskFactors.put("BMI", (Double) userContext.getBasicInfo().get("bmi"));
                }

                // עישון (המרה לציון מספרי)
                if (userContext.getBasicInfo().get("smokingRiskWeight") != null) {
                    double smokingWeight = (Double) userContext.getBasicInfo().get("smokingRiskWeight");
                    userRiskFactors.put("SMOKING_SCORE", smokingWeight * 10); // נרמול לסקלה של 0-10
                }

                // לחץ דם (נמיר מהתיאור למספר)
                if (userContext.getBasicInfo().get("bloodPressure") != null) {
                    String bpDescription = (String) userContext.getBasicInfo().get("bloodPressure");
                    double bpValue = convertBloodPressureDescriptionToValue(bpDescription);
                    userRiskFactors.put("BLOOD_PRESSURE_SYSTOLIC", bpValue);
                }
            }

            // יצירת קשרים ב-Neo4j
            if (!userRiskFactors.isEmpty()) {
                riskFactorSer.createUserRiskFactorRelationships(userId.toString(), userRiskFactors);
                logger.debug("Updated {} risk factors in Neo4j for user {}", userRiskFactors.size(), userId);
            }

        } catch (Exception e) {
            logger.warn("Could not update risk factors in Neo4j for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * המרת תיאור לחץ דם לערך מספרי
     */
    private double convertBloodPressureDescriptionToValue(String description) {
        return switch (description.toLowerCase()) {
            case "normal" -> 120;
            case "elevated" -> 130;
            case "stage 1 hypertension" -> 140;
            case "stage 2 hypertension" -> 160;
            case "hypertensive crisis" -> 180;
            default -> 120;
        };
    }

    /**
     * ניתוח השפעת גורמי הסיכון על הסימפטומים
     */
    private List<MedicalConnection> analyzeRiskFactorImpact(UserMedicalContext userContext, Set<ExtractedSymptom> symptoms) {
        List<MedicalConnection> riskConnections = new ArrayList<>();

        try {
            for (UserMedicalEntity riskFactor : userContext.getRiskFactors()) {
                for (ExtractedSymptom symptom : symptoms) {
                    MedicalConnection connection = analyzeSpecificRiskFactorImpact(riskFactor, symptom, userContext);
                    if (connection != null) {
                        riskConnections.add(connection);
                    }
                }
            }

            logger.debug("Analyzed risk factor impact: found {} connections", riskConnections.size());

        } catch (Exception e) {
            logger.error("Error analyzing risk factor impact: {}", e.getMessage());
        }

        return riskConnections;
    }

    /**
     * ניתוח השפעה של גורם סיכון ספציפי על סימפטום
     */
    private MedicalConnection analyzeSpecificRiskFactorImpact(UserMedicalEntity riskFactor, ExtractedSymptom symptom, UserMedicalContext userContext) {
        try {
            // לוגיקת ניתוח לפי סוג גורם הסיכון והסימפטום
            String riskType = getRiskFactorType(riskFactor);
            double riskWeight = getRiskFactorWeight(riskFactor);

            // מיפוי קשרים ידועים בין גורמי סיכון לסימפטומים
            boolean isRelevant = isRiskFactorRelevantToSymptom(riskType, symptom.getName(), riskWeight);

            if (isRelevant) {
                MedicalConnection connection = new MedicalConnection();
                connection.setType(MedicalConnection.ConnectionType.RISK_FACTOR);
                connection.setFromEntity(riskFactor.getName());
                connection.setToEntity(symptom.getName());
                connection.setFromCui(riskFactor.getCui());
                connection.setToCui(symptom.getCui());
                connection.setConfidence(calculateRiskFactorConfidence(riskType, symptom.getName(), riskWeight));
                connection.setExplanation(buildRiskFactorExplanation(riskFactor, symptom, userContext));

                return connection;
            }

        } catch (Exception e) {
            logger.debug("Error analyzing specific risk factor impact: {}", e.getMessage());
        }

        return null;
    }

    /**
     * בדיקה אם גורם סיכון רלוונטי לסימפטום
     */
    private boolean isRiskFactorRelevantToSymptom(String riskType, String symptomName, double riskWeight) {
        Map<String, Set<String>> riskSymptomMap = Map.of(
                "SMOKING", Set.of("chest pain", "shortness of breath", "cough", "fatigue"),
                "BMI", Set.of("chest pain", "shortness of breath", "fatigue", "joint pain"),
                "BLOOD_PRESSURE", Set.of("headache", "chest pain", "dizziness", "fatigue"),
                "AGE_GROUP", Set.of("fatigue", "joint pain", "memory problems", "dizziness"),
                "FAMILY_HEART_DISEASE", Set.of("chest pain", "shortness of breath", "palpitations"),
                "FAMILY_CANCER", Set.of("fatigue", "weight loss", "pain"),
                "STRESS", Set.of("headache", "chest pain", "fatigue", "insomnia"),
                "PHYSICAL_ACTIVITY", Set.of("fatigue", "shortness of breath", "chest pain")
        );

        Set<String> relevantSymptoms = riskSymptomMap.get(riskType);
        if (relevantSymptoms == null) return false;

        return relevantSymptoms.stream()
                .anyMatch(rs -> symptomName.toLowerCase().contains(rs.toLowerCase())) && riskWeight > 0.2;
    }

    /**
     * חישוב רמת ביטחון לקשר גורם סיכון-סימפטום
     */
    private double calculateRiskFactorConfidence(String riskType, String symptomName, double riskWeight) {
        double baseConfidence = switch (riskType) {
            case "SMOKING" -> 0.8;
            case "BMI" -> 0.7;
            case "BLOOD_PRESSURE" -> 0.85;
            case "FAMILY_HEART_DISEASE" -> 0.75;
            case "AGE_GROUP" -> 0.6;
            default -> 0.5;
        };

        return Math.min(0.95, baseConfidence * (1 + riskWeight));
    }

    /**
     * בניית הסבר לקשר גורם סיכון-סימפטום
     */
    private String buildRiskFactorExplanation(UserMedicalEntity riskFactor, ExtractedSymptom symptom, UserMedicalContext userContext) {
        String riskType = getRiskFactorType(riskFactor);

        return switch (riskType) {
            case "SMOKING" -> String.format("עישון יכול להיות גורם לסימפטום %s בשל השפעתו על מערכת הנשימה והלב", symptom.getName());
            case "BMI" -> String.format("עודף משקל יכול לתרום להופעת %s בשל העומס הנוסף על הגוף", symptom.getName());
            case "BLOOD_PRESSURE" -> String.format("לחץ דם גבוה יכול לגרום ל%s כתוצאה מעומס על מערכת הלב וכלי הדם", symptom.getName());
            case "FAMILY_HEART_DISEASE" -> String.format("היסטוריה משפחתית של מחלות לב מעלה את הסיכון להופעת %s", symptom.getName());
            case "AGE_GROUP" -> String.format("הגיל יכול להיות גורם תורם להופעת %s", symptom.getName());
            default -> String.format("גורם הסיכון %s עשוי להיות קשור לסימפטום %s", riskFactor.getName(), symptom.getName());
        };
    }

    /**
     * קביעת רמת דחיפות כולל גורמי סיכון
     */
    private TreatmentPlan.UrgencyLevel calculateUrgencyLevelWithRiskFactors(
            List<MedicalConnection> connections, Set<ExtractedSymptom> symptoms, UserMedicalContext userContext) {

        // בדיקת סימפטומים דחופים (כמו קודם)
        Set<String> emergencyKeywords = Set.of(
                "chest pain", "difficulty breathing", "severe pain", "bleeding",
                "unconscious", "seizure", "stroke", "heart attack"
        );

        boolean hasEmergencySymptoms = symptoms.stream()
                .anyMatch(symptom -> emergencyKeywords.stream()
                        .anyMatch(keyword -> symptom.getName().toLowerCase().contains(keyword)));

        if (hasEmergencySymptoms) {
            logger.warn("Emergency symptoms detected!");
            return TreatmentPlan.UrgencyLevel.EMERGENCY;
        }

        // ניתוח גורמי סיכון
        double riskFactorScore = calculateOverallRiskFactorScore(userContext);
        logger.debug("Overall risk factor score: {}", riskFactorScore);

        // קשרים עם ביטחון גבוה
        long highConfidenceConnections = connections.stream()
                .filter(conn -> conn.getConfidence() > 0.8)
                .count();

        // תופעות לוואי מתרופות
        long sideEffectConnections = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.SIDE_EFFECT)
                .filter(conn -> conn.getConfidence() > 0.6)
                .count();

        // קשרי גורמי סיכון
        long riskFactorConnections = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.RISK_FACTOR)
                .filter(conn -> conn.getConfidence() > 0.7)
                .count();

        // החלטה על בסיס כל הגורמים
        if (sideEffectConnections > 0 || (riskFactorScore > 0.8 && riskFactorConnections > 2)) {
            return TreatmentPlan.UrgencyLevel.HIGH;
        }

        if (highConfidenceConnections > 2 || (riskFactorScore > 0.6 && riskFactorConnections > 1)) {
            return TreatmentPlan.UrgencyLevel.MEDIUM;
        }

        if (riskFactorScore > 0.4 || riskFactorConnections > 0) {
            return TreatmentPlan.UrgencyLevel.MEDIUM;
        }

        return TreatmentPlan.UrgencyLevel.LOW;
    }

    /**
     * חישוב ציון גורמי סיכון כולל
     */
    private double calculateOverallRiskFactorScore(UserMedicalContext userContext) {
        if (userContext.getRiskFactors().isEmpty()) {
            return 0.0;
        }

        double totalWeight = 0.0;
        int factorCount = 0;

        for (UserMedicalEntity riskFactor : userContext.getRiskFactors()) {
            double weight = getRiskFactorWeight(riskFactor);
            if (weight > 0.1) { // רק גורמי סיכון משמעותיים
                totalWeight += weight;
                factorCount++;
            }
        }

        return factorCount > 0 ? totalWeight / factorCount : 0.0;
    }

    /**
     * בניית תכנית טיפול מקיפה
     */
    private TreatmentPlan buildComprehensiveTreatmentPlan(TreatmentPlan.UrgencyLevel urgencyLevel,
                                                          List<MedicalConnection> connections,
                                                          Set<ExtractedSymptom> symptoms,
                                                          UserMedicalContext userContext) {
        TreatmentPlan plan = new TreatmentPlan();
        plan.setUrgencyLevel(urgencyLevel);
        plan.setFoundConnections(connections);

        // קביעת הדאגה העיקרית (כולל גורמי סיכון)
        plan.setMainConcern(determineMainConcernWithRiskFactors(connections, symptoms, userContext));

        // הסבר למשתמש (מעודכן)
        plan.setReasoning(buildReasoningWithRiskFactors(connections, urgencyLevel, userContext));

        // פעולות מיידיות (כולל המלצות לגורמי סיכון)
        plan.setImmediateActions(generateImmediateActionsWithRiskFactors(connections, urgencyLevel, userContext));

        // בדיקות מומלצות (מותאמות לגורמי סיכון)
        plan.setRecommendedTests(generateRecommendedTestsWithRiskFactors(symptoms, urgencyLevel, userContext));

        // ביקורי רופא
        plan.setDoctorVisits(generateDoctorVisitsWithRiskFactors(urgencyLevel, connections, userContext));

        // מידע נוסף מקיף
        plan.setAdditionalInfo(buildComprehensiveAdditionalInfo(userContext, connections));

        return plan;
    }

    /**
     * קביעת הדאגה העיקרית כולל גורמי סיכון
     */
    private String determineMainConcernWithRiskFactors(List<MedicalConnection> connections, Set<ExtractedSymptom> symptoms, UserMedicalContext userContext) {
        // תופעות לוואי
        Optional<MedicalConnection> sideEffect = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.SIDE_EFFECT)
                .max(Comparator.comparingDouble(MedicalConnection::getConfidence));

        if (sideEffect.isPresent()) {
            return String.format("Suspected side effect of medication %s", sideEffect.get().getFromEntity());
        }

        // גורמי סיכון משמעותיים
        List<MedicalConnection> significantRiskFactors = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.RISK_FACTOR)
                .filter(conn -> conn.getConfidence() > 0.7)
                .collect(Collectors.toList());

        if (!significantRiskFactors.isEmpty()) {
            String riskFactorNames = significantRiskFactors.stream()
                    .map(MedicalConnection::getFromEntity)
                    .collect(Collectors.joining(", "));
            return String.format("Symptoms may be influenced by risk factors: %s", riskFactorNames);
        }

        // מחלות קיימות
        Optional<MedicalConnection> diseaseConnection = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.DISEASE_SYMPTOM)
                .max(Comparator.comparingDouble(MedicalConnection::getConfidence));

        if (diseaseConnection.isPresent()) {
            return String.format("Symptoms may be related to existing condition: %s", diseaseConnection.get().getFromEntity());
        }

        return "Analysis of reported symptoms with consideration of personal risk factors";
    }

    /**
     * בניית הסבר מקיף
     */
    private String buildReasoningWithRiskFactors(List<MedicalConnection> connections, TreatmentPlan.UrgencyLevel urgencyLevel, UserMedicalContext userContext) {
        StringBuilder reasoning = new StringBuilder();

        switch (urgencyLevel) {
            case EMERGENCY -> reasoning.append("Emergency symptoms requiring immediate medical attention have been identified. ");
            case HIGH -> reasoning.append("Significant medical connections requiring prompt attention have been found. ");
            case MEDIUM -> reasoning.append("Medical connections requiring follow-up have been identified. ");
            case LOW -> reasoning.append("Some medical connections have been found that warrant monitoring. ");
        }

        // הוספת מידע על גורמי סיכון
        long riskFactorConnections = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.RISK_FACTOR)
                .count();

        if (riskFactorConnections > 0) {
            reasoning.append(String.format("Your personal risk factors (%d identified) may be contributing to these symptoms. ", riskFactorConnections));
        }

        if (userContext.getOverallRiskScore() != null && userContext.getOverallRiskScore() > 0.5) {
            reasoning.append("Your overall risk profile suggests closer medical monitoring may be beneficial. ");
        }

        reasoning.append("These recommendations are based on your personal medical profile and general medical knowledge. ");
        reasoning.append("They are not a substitute for professional medical consultation.");

        return reasoning.toString();
    }

    /**
     * פעולות מיידיות כולל המלצות לגורמי סיכון
     */
    private List<ImmediateAction> generateImmediateActionsWithRiskFactors(List<MedicalConnection> connections, TreatmentPlan.UrgencyLevel urgencyLevel, UserMedicalContext userContext) {
        List<ImmediateAction> actions = generateImmediateActions(connections, urgencyLevel);

        // הוספת המלצות ספציפיות לגורמי סיכון
        for (UserMedicalEntity riskFactor : userContext.getRiskFactors()) {
            String riskType = getRiskFactorType(riskFactor);
            double riskWeight = getRiskFactorWeight(riskFactor);

            if (riskWeight > 0.6) { // גורמי סיכון משמעותיים
                ImmediateAction riskAction = createRiskFactorAction(riskType, riskWeight);
                if (riskAction != null) {
                    actions.add(riskAction);
                }
            }
        }

        return actions;
    }

    /**
     * יצירת פעולה ספציפית לגורם סיכון
     */
    private ImmediateAction createRiskFactorAction(String riskType, double riskWeight) {
        ImmediateAction action = new ImmediateAction();
        action.setPriority(riskWeight > 0.8 ? 2 : 3);

        switch (riskType) {
            case "SMOKING" -> {
                action.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
                action.setDescription("Consider discussing smoking cessation with your doctor");
                action.setReason("Smoking significantly increases health risks");
                return action;
            }
            case "BMI" -> {
                action.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
                action.setDescription("Monitor symptoms and consider lifestyle modifications");
                action.setReason("Weight management may help reduce symptoms");
                return action;
            }
            case "BLOOD_PRESSURE" -> {
                action.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
                action.setDescription("Monitor blood pressure regularly");
                action.setReason("High blood pressure requires ongoing monitoring");
                return action;
            }
        }

        return null;
    }

    // Helper methods
    private String getRiskFactorType(UserMedicalEntity riskFactor) {
        if (riskFactor.getAdditionalData() != null && riskFactor.getAdditionalData().get("risk_type") != null) {
            return (String) riskFactor.getAdditionalData().get("risk_type");
        }

        String name = riskFactor.getName().toLowerCase();
        if (name.contains("smok")) return "SMOKING";
        if (name.contains("bmi") || name.contains("weight")) return "BMI";
        if (name.contains("blood pressure") || name.contains("hypertension")) return "BLOOD_PRESSURE";
        if (name.contains("age")) return "AGE_GROUP";
        if (name.contains("family") && name.contains("heart")) return "FAMILY_HEART_DISEASE";
        if (name.contains("family") && name.contains("cancer")) return "FAMILY_CANCER";
        if (name.contains("stress")) return "STRESS";
        if (name.contains("activity") || name.contains("exercise")) return "PHYSICAL_ACTIVITY";

        return "OTHER";
    }

    private double getRiskFactorWeight(UserMedicalEntity riskFactor) {
        if (riskFactor.getAdditionalData() != null && riskFactor.getAdditionalData().get("weight") != null) {
            return (Double) riskFactor.getAdditionalData().get("weight");
        }

        // ברירת מחדל לפי חומרה
        return switch (riskFactor.getSeverity() != null ? riskFactor.getSeverity().toLowerCase() : "medium") {
            case "high" -> 0.8;
            case "medium" -> 0.5;
            case "low" -> 0.3;
            default -> 0.5;
        };
    }

    // שאר המתודות נשארות כמו קודם...
    private List<MedicalConnection> findAllMedicalConnections(UserMedicalContext userContext, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> allConnections = new ArrayList<>();
        try {
            List<MedicalConnection> sideEffects = pathfindingService.findMedicationSideEffects(userContext.getCurrentMedications(), symptoms);
            allConnections.addAll(sideEffects);

            List<MedicalConnection> diseaseSymptoms = pathfindingService.findDiseaseSymptoms(userContext.getActiveDiseases(), symptoms);
            allConnections.addAll(diseaseSymptoms);

            List<MedicalConnection> treatments = pathfindingService.findPossibleTreatments(symptoms);
            allConnections.addAll(treatments);
        } catch (Exception e) {
            logger.error("Error finding medical connections: {}", e.getMessage(), e);
        }
        return allConnections;
    }

    private List<ImmediateAction> generateImmediateActions(List<MedicalConnection> connections, TreatmentPlan.UrgencyLevel urgencyLevel) {
        List<ImmediateAction> actions = new ArrayList<>();

        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            ImmediateAction emergency = new ImmediateAction();
            emergency.setType(ImmediateAction.ActionType.CALL_EMERGENCY);
            emergency.setDescription("Call emergency services immediately!");
            emergency.setReason("Emergency symptoms requiring immediate treatment identified");
            emergency.setPriority(1);
            actions.add(emergency);
        }

        List<MedicalConnection> sideEffects = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.SIDE_EFFECT)
                .filter(conn -> conn.getConfidence() > 0.7)
                .collect(Collectors.toList());

        for (MedicalConnection sideEffect : sideEffects) {
            ImmediateAction action = new ImmediateAction();
            action.setType(ImmediateAction.ActionType.STOP_MEDICATION);
            action.setDescription("Consider consulting doctor about medication " + sideEffect.getFromEntity());
            action.setReason("Suspected side effect");
            action.setPriority(2);
            actions.add(action);
        }

        if (urgencyLevel == TreatmentPlan.UrgencyLevel.MEDIUM || urgencyLevel == TreatmentPlan.UrgencyLevel.LOW) {
            ImmediateAction monitor = new ImmediateAction();
            monitor.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
            monitor.setDescription("Monitor symptoms and record changes");
            monitor.setReason("Important to track symptom progression");
            monitor.setPriority(3);
            actions.add(monitor);
        }

        return actions;
    }

    private List<MedicalTest> generateRecommendedTestsWithRiskFactors(Set<ExtractedSymptom> symptoms, TreatmentPlan.UrgencyLevel urgencyLevel, UserMedicalContext userContext) {
        List<MedicalTest> tests = new ArrayList<>();

        Map<String, MedicalTest.TestType> symptomToTest = Map.of(
                "chest pain", MedicalTest.TestType.ECG,
                "heart", MedicalTest.TestType.ECG,
                "fever", MedicalTest.TestType.BLOOD_TEST,
                "headache", MedicalTest.TestType.BLOOD_PRESSURE,
                "pain", MedicalTest.TestType.BLOOD_TEST
        );

        Set<MedicalTest.TestType> recommendedTestTypes = new HashSet<>();

        for (ExtractedSymptom symptom : symptoms) {
            String symptomName = symptom.getName().toLowerCase();
            for (Map.Entry<String, MedicalTest.TestType> entry : symptomToTest.entrySet()) {
                if (symptomName.contains(entry.getKey())) {
                    recommendedTestTypes.add(entry.getValue());
                }
            }
        }

        // הוספת בדיקות מותאמות לגורמי סיכון
        for (UserMedicalEntity riskFactor : userContext.getRiskFactors()) {
            String riskType = getRiskFactorType(riskFactor);
            if (getRiskFactorWeight(riskFactor) > 0.6) {
                switch (riskType) {
                    case "BMI" -> recommendedTestTypes.add(MedicalTest.TestType.BLOOD_TEST);
                    case "BLOOD_PRESSURE" -> recommendedTestTypes.add(MedicalTest.TestType.BLOOD_PRESSURE);
                    case "FAMILY_HEART_DISEASE" -> recommendedTestTypes.add(MedicalTest.TestType.ECG);
                }
            }
        }

        String urgency = switch (urgencyLevel) {
            case EMERGENCY -> "ASAP";
            case HIGH -> "Within 24h";
            case MEDIUM -> "Within week";
            case LOW -> "Within month";
        };

        for (MedicalTest.TestType testType : recommendedTestTypes) {
            MedicalTest test = new MedicalTest();
            test.setType(testType);
            test.setDescription(testType.getDescription());
            test.setReason("Test relevant to reported symptoms and risk factors");
            test.setUrgency(urgency);
            tests.add(test);
        }

        return tests;
    }

    private List<DoctorVisit> generateDoctorVisitsWithRiskFactors(TreatmentPlan.UrgencyLevel urgencyLevel, List<MedicalConnection> connections, UserMedicalContext userContext) {
        List<DoctorVisit> visits = new ArrayList<>();

        String urgency = switch (urgencyLevel) {
            case EMERGENCY -> "Immediately";
            case HIGH -> "Same day";
            case MEDIUM -> "Within few days";
            case LOW -> "Within week";
        };

        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            DoctorVisit emergency = new DoctorVisit();
            emergency.setType(DoctorVisit.DoctorType.EMERGENCY_ROOM);
            emergency.setReason("Emergency symptoms");
            emergency.setUrgency(urgency);
            visits.add(emergency);
        } else {
            DoctorVisit familyDoctor = new DoctorVisit();
            familyDoctor.setType(DoctorVisit.DoctorType.FAMILY_DOCTOR);
            familyDoctor.setReason("Symptom evaluation and risk factor management");
            familyDoctor.setUrgency(urgency);
            visits.add(familyDoctor);

            // המלצה לרופא מומחה לפי גורמי סיכון
            boolean hasHeartRisk = userContext.getRiskFactors().stream()
                    .anyMatch(rf -> getRiskFactorType(rf).contains("HEART") || getRiskFactorType(rf).equals("BLOOD_PRESSURE"));

            if (hasHeartRisk && (urgencyLevel == TreatmentPlan.UrgencyLevel.HIGH || urgencyLevel == TreatmentPlan.UrgencyLevel.MEDIUM)) {
                DoctorVisit cardiologist = new DoctorVisit();
                cardiologist.setType(DoctorVisit.DoctorType.CARDIOLOGIST);
                cardiologist.setReason("Heart-related risk factors require specialist evaluation");
                cardiologist.setUrgency("Within 1-2 weeks");
                visits.add(cardiologist);
            }
        }

        return visits;
    }

    private Map<String, Object> buildComprehensiveAdditionalInfo(UserMedicalContext userContext, List<MedicalConnection> connections) {
        Map<String, Object> info = new HashMap<>();

        info.put("userRiskLevel", userContext.getRiskLevel());
        info.put("userRiskScore", userContext.getOverallRiskScore());
        info.put("totalConnections", connections.size());
        info.put("riskFactorCount", userContext.getRiskFactors().size());
        info.put("activeMedicationsCount", userContext.getCurrentMedications().size());
        info.put("activeDiseasesCount", userContext.getActiveDiseases().size());
        info.put("analysisTimestamp", System.currentTimeMillis());

        // פירוט גורמי הסיכון המשמעותיים
        List<String> significantRiskFactors = userContext.getRiskFactors().stream()
                .filter(rf -> getRiskFactorWeight(rf) > 0.6)
                .map(UserMedicalEntity::getName)
                .collect(Collectors.toList());
        info.put("significantRiskFactors", significantRiskFactors);

        return info;
    }

    private TreatmentPlan createEmergencyPlan(Set<ExtractedSymptom> symptoms) {
        logger.warn("Creating emergency treatment plan due to analysis error");

        TreatmentPlan emergencyPlan = new TreatmentPlan();
        emergencyPlan.setUrgencyLevel(TreatmentPlan.UrgencyLevel.MEDIUM);
        emergencyPlan.setMainConcern("Unable to complete full analysis - medical consultation recommended");
        emergencyPlan.setReasoning("An error occurred during analysis. Medical consultation is recommended for proper evaluation.");

        ImmediateAction action = new ImmediateAction();
        action.setType(ImmediateAction.ActionType.SEEK_IMMEDIATE_CARE);
        action.setDescription("Consult healthcare provider for proper evaluation");
        action.setReason("Complete analysis could not be performed");
        action.setPriority(1);

        emergencyPlan.setImmediateActions(List.of(action));

        DoctorVisit visit = new DoctorVisit();
        visit.setType(DoctorVisit.DoctorType.FAMILY_DOCTOR);
        visit.setReason("Medical evaluation required");
        visit.setUrgency("Within few days");
        emergencyPlan.setDoctorVisits(List.of(visit));

        emergencyPlan.setRecommendedTests(new ArrayList<>());
        emergencyPlan.setFoundConnections(new ArrayList<>());
        emergencyPlan.setAdditionalInfo(Map.of("error", "Analysis incomplete", "symptomsCount", symptoms.size()));

        return emergencyPlan;
    }

    public UserMedicalContext getUserMedicalContext(UUID userId) {
        return medicalContextService.getUserMedicalContext(userId);
    }
}