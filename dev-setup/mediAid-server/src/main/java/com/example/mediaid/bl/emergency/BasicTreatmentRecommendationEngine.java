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

/**
 * מנוע המלצות טיפול
 */
@Service
public class BasicTreatmentRecommendationEngine {

    private static final Logger logger = LoggerFactory.getLogger(BasicTreatmentRecommendationEngine.class);

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
            riskFactorService.updateUserRiskFactorsInNeo4j(userId, userContext);

            // שלב 3: חיפוש קשרים רפואיים
            List<MedicalConnection> allConnections = findAllMedicalConnections(userContext, new ArrayList<>(symptoms));
            logger.info("Found {} basic medical connections", allConnections.size());

            // שלב 4: ניתוח השפעת גורמי הסיכון על הסימפטומים
            List<MedicalConnection> riskFactorConnections = riskFactorService.analyzeRiskFactorImpact(userContext, symptoms);
            allConnections.addAll(riskFactorConnections);
            logger.info("Added {} risk factor connections", riskFactorConnections.size());

            // שלב 5: קביעת רמת הדחיפות
            TreatmentPlan.UrgencyLevel urgencyLevel = calculateUrgencyLevel(allConnections, symptoms, userContext);
            logger.info("Calculated urgency level: {}", urgencyLevel);

            // שלב 6: יצירת תכנית הטיפול המקיפה
            TreatmentPlan treatmentPlan = buildTreatmentPlan(urgencyLevel, allConnections, symptoms, userContext);

            logger.info("Treatment plan created successfully for user {}", userId);
            return treatmentPlan;

        } catch (Exception e) {
            logger.error("Error in medical analysis for user {}: {}", userId, e.getMessage(), e);
            return createEmergencyPlan(symptoms);
        }
    }

    /**
     * חיפוש כל הקשרים הרפואיים
     */
    private List<MedicalConnection> findAllMedicalConnections(UserMedicalContext userContext, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> allConnections = new ArrayList<>();

        try {
            // תופעות לוואי מתרופות
            List<MedicalConnection> sideEffects = pathfindingService.findMedicationSideEffects(
                    userContext.getCurrentMedications(), symptoms);
            allConnections.addAll(sideEffects);
            logger.debug("Found {} medication side effects", sideEffects.size());

            // קשרים בין מחלות לסימפטומים
            List<MedicalConnection> diseaseSymptoms = pathfindingService.findDiseaseSymptoms(
                    userContext.getActiveDiseases(), symptoms);
            allConnections.addAll(diseaseSymptoms);
            logger.debug("Found {} disease-symptom connections", diseaseSymptoms.size());

            // טיפולים אפשריים
            List<MedicalConnection> treatments = pathfindingService.findPossibleTreatments(symptoms);
            allConnections.addAll(treatments);
            logger.debug("Found {} possible treatments", treatments.size());

        } catch (Exception e) {
            logger.error("Error finding medical connections: {}", e.getMessage(), e);
        }

        return allConnections;
    }

    /**
     * קביעת רמת דחיפות כולל גורמי סיכון
     */
    private TreatmentPlan.UrgencyLevel calculateUrgencyLevel(List<MedicalConnection> connections,
                                                             Set<ExtractedSymptom> symptoms,
                                                             UserMedicalContext userContext) {

        // בדיקת סימפטומים דחופים
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

        // מכפיל על בסיס גורמי סיכון
        double riskMultiplier = riskFactorService.calculateRiskBasedUrgencyMultiplier(userContext, connections);

        // החלטה על בסיס כל הגורמים
        if (sideEffectConnections > 0 || (riskMultiplier > 1.4 && riskFactorConnections > 2)) {
            return TreatmentPlan.UrgencyLevel.HIGH;
        }

        if (highConfidenceConnections > 2 || (riskMultiplier > 1.2 && riskFactorConnections > 1)) {
            return TreatmentPlan.UrgencyLevel.MEDIUM;
        }

        if (riskMultiplier > 1.1 || riskFactorConnections > 0) {
            return TreatmentPlan.UrgencyLevel.MEDIUM;
        }

        return TreatmentPlan.UrgencyLevel.LOW;
    }

    /**
     * בניית תכנית טיפול מקיפה
     */
    private TreatmentPlan buildTreatmentPlan(TreatmentPlan.UrgencyLevel urgencyLevel,
                                             List<MedicalConnection> connections,
                                             Set<ExtractedSymptom> symptoms,
                                             UserMedicalContext userContext) {
        TreatmentPlan plan = new TreatmentPlan();
        plan.setUrgencyLevel(urgencyLevel);
        plan.setFoundConnections(connections);

        // קביעת הדאגה העיקרית
        plan.setMainConcern(determineMainConcern(connections, symptoms, userContext));

        // הסבר למשתמש
        plan.setReasoning(buildReasoning(connections, urgencyLevel, userContext));

        // פעולות מיידיות
        plan.setImmediateActions(generateImmediateActions(connections, urgencyLevel, userContext));

        // בדיקות מומלצות
        plan.setRecommendedTests(generateRecommendedTests(symptoms, urgencyLevel, userContext));

        // ביקורי רופא
        plan.setDoctorVisits(generateDoctorVisits(urgencyLevel, connections, userContext));

        // מידע נוסף
        plan.setAdditionalInfo(buildAdditionalInfo(userContext, connections));

        return plan;
    }

    /**
     * קביעת הדאגה העיקרית
     */
    private String determineMainConcern(List<MedicalConnection> connections, Set<ExtractedSymptom> symptoms, UserMedicalContext userContext) {
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
                .toList();

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
    private String buildReasoning(List<MedicalConnection> connections, TreatmentPlan.UrgencyLevel urgencyLevel, UserMedicalContext userContext) {
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
     *המלצות לפעולות מיידיות
     */
    private List<ImmediateAction> generateImmediateActions(List<MedicalConnection> connections,
                                                           TreatmentPlan.UrgencyLevel urgencyLevel,
                                                           UserMedicalContext userContext) {
        List<ImmediateAction> actions = new ArrayList<>();

        // פעולות דחופות
        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            ImmediateAction emergency = new ImmediateAction();
            emergency.setType(ImmediateAction.ActionType.CALL_EMERGENCY);
            emergency.setDescription("Call emergency services immediately!");
            emergency.setReason("Emergency symptoms requiring immediate treatment identified");
            emergency.setPriority(1);
            actions.add(emergency);
        }

        // תופעות לוואי
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

        // המלצות ספציפיות לגורמי סיכון
        for (UserMedicalEntity riskFactor : userContext.getRiskFactors()) {
            String riskType = riskFactorService.getRiskFactorType(riskFactor);
            double riskWeight = riskFactorService.getRiskFactorWeight(riskFactor);

            if (riskWeight > 0.6) { // גורמי סיכון משמעותיים
                ImmediateAction riskAction = createRiskFactorAction(riskType, riskWeight);
                if (riskAction != null) {
                    actions.add(riskAction);
                }
            }
        }

        // פעולת ניטור כללית
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

    /**
     * בדיקות מומלצות מותאמות לגורמי סיכון
     */
    private List<MedicalTest> generateRecommendedTests(Set<ExtractedSymptom> symptoms,
                                                       TreatmentPlan.UrgencyLevel urgencyLevel,
                                                       UserMedicalContext userContext) {
        List<MedicalTest> tests = new ArrayList<>();

        Map<String, MedicalTest.TestType> symptomToTest = Map.of(
                "chest pain", MedicalTest.TestType.ECG,
                "heart", MedicalTest.TestType.ECG,
                "fever", MedicalTest.TestType.BLOOD_TEST,
                "headache", MedicalTest.TestType.BLOOD_PRESSURE,
                "pain", MedicalTest.TestType.BLOOD_TEST
        );

        Set<MedicalTest.TestType> recommendedTestTypes = new HashSet<>();

        // בדיקות על בסיס סימפטומים
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
            String riskType = riskFactorService.getRiskFactorType(riskFactor);
            if (riskFactorService.getRiskFactorWeight(riskFactor) > 0.6) {
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

    /**
     * ביקורי רופא מותאמים לגורמי סיכון
     */
    private List<DoctorVisit> generateDoctorVisits(TreatmentPlan.UrgencyLevel urgencyLevel,
                                                   List<MedicalConnection> connections,
                                                   UserMedicalContext userContext) {
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
                    .anyMatch(rf -> riskFactorService.getRiskFactorType(rf).contains("HEART") ||
                            riskFactorService.getRiskFactorType(rf).equals("BLOOD_PRESSURE"));

            if (hasHeartRisk && (urgencyLevel == TreatmentPlan.UrgencyLevel.HIGH ||
                    urgencyLevel == TreatmentPlan.UrgencyLevel.MEDIUM)) {
                DoctorVisit cardiologist = new DoctorVisit();
                cardiologist.setType(DoctorVisit.DoctorType.CARDIOLOGIST);
                cardiologist.setReason("Heart-related risk factors require specialist evaluation");
                cardiologist.setUrgency("Within 1-2 weeks");
                visits.add(cardiologist);
            }
        }

        return visits;
    }

    /**
     * מידע נוסף מקיף
     */
    private Map<String, Object> buildAdditionalInfo(UserMedicalContext userContext, List<MedicalConnection> connections) {
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
                .filter(rf -> riskFactorService.getRiskFactorWeight(rf) > 0.6)
                .map(UserMedicalEntity::getName)
                .collect(Collectors.toList());
        info.put("significantRiskFactors", significantRiskFactors);

        // סטטיסטיקות קשרים
        Map<String, Long> connectionsByType = connections.stream()
                .collect(Collectors.groupingBy(
                        conn -> conn.getType().name(),
                        Collectors.counting()
                ));
        info.put("connectionsByType", connectionsByType);

        return info;
    }

    /**
     * תכנית חירום במקרה של שגיאה
     */
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

    /**
     * קבלת המידע הרפואי של המשתמש
     */
    public UserMedicalContext getUserMedicalContext(UUID userId) {
        return medicalContextService.getUserMedicalContext(userId);
    }
}