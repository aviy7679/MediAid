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

//מנוע המלצות טיפול
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


    //הפונקציה המרכזית - ניתוח מצב רפואי ויצירת תכנית טיפול
    public TreatmentPlan analyzeSituation(UUID userId, Set<ExtractedSymptom> symptoms) {
        logger.info("Analysing Situation for user {} with {} symptoms",userId, symptoms.size());

        try{
            //טעינת המידע הרפואי של המשתמש
            UserMedicalContext userContext = medicalContextService.getUserMedicalContext(userId);
            logger.debug("Loaded user medical context: {} medications, {} diseases",userContext.getCurrentMedications().size(), userContext.getActiveDiseases().size());

            //חיפוש קשרים רפואיים
            List<MedicalConnection> allConnection =findAllMedicalConnections(userContext, new ArrayList<>(symptoms));
            logger.info("Found {} medical connections", allConnection.size());

            //קביעת רמת הדחיפות
            TreatmentPlan.UrgencyLevel urgencyLevel =calculateUrgencyLeve(allConnection, symptoms);
            logger.info("Calculated Urgency Level: {}", urgencyLevel);

            //יצירת תכנית הטיפול
            TreatmentPlan treatmentPlan =buildTreatmentPlan(urgencyLevel, allConnection, symptoms, userContext);
            logger.info("Treatment plan created successfully for user {}", userId);
            return treatmentPlan;

        } catch (Exception e) {
            logger.error("Error analyzing situation for user {}: {}",userId, e.getMessage());
            //תכנית  בסיסית במקרה של שגיאה
             return createEmrgencyPlan(symptoms);
        }
    }

    //בניית תכנית טיפול מלאה
    private TreatmentPlan buildTreatmentPlan(TreatmentPlan.UrgencyLevel urgencyLevel,
                                             List<MedicalConnection> connections,
                                             Set<ExtractedSymptom> symptoms,
                                             UserMedicalContext userContext) {
        TreatmentPlan plan = new TreatmentPlan();
        plan.setUrgencyLevel(urgencyLevel);
        plan.setFoundConnections(connections);

        //קביעת הדאגה העיקרית
        plan.setMainConcern(determineMainConcern(connections, symptoms));

        //הסבר למשתמש
        plan.setReasoning(buildReasoning(connections,urgencyLevel));

        //פעולות מיידיות
        plan.setImmediateActions(generateImmediateActions(connections, urgencyLevel));

        //בדיקות מומלצות
        plan.setRecommendedTests(generateRecommendedTests(symptoms, urgencyLevel));

        //ביקורי רופא
        plan.setDoctorVisits(generateDoctorVisits(urgencyLevel,connections));

        //מידע נוסף
        plan.setAdditionalInfo(buildAdditionalInfo(userContext, connections));

        return plan;
    }

    //רמת הדחיפות ע"ס הקשרים
    private TreatmentPlan.UrgencyLevel calculateUrgencyLeve(List<MedicalConnection> connections, Set<ExtractedSymptom> symptoms){
        // בדיקת מילות מפתח סכנה
        Set<String> emergencyKeywords = Set.of(
                "chest pain", "difficulty breathing", "severe pain", "bleeding",
                "unconscious", "seizure", "stroke", "heart attack"
        );

        // בדיקה אם יש סימפטומים דחופים
        boolean hasEmergencySymptoms = symptoms.stream()
                .anyMatch(symptom -> emergencyKeywords.stream()
                        .anyMatch(keyword -> symptom.getName().toLowerCase().contains(keyword)));

        if (hasEmergencySymptoms) {
            logger.warn("Emergency symptoms detected!");
            return TreatmentPlan.UrgencyLevel.EMERGENCY;
        }
        //קשרים עם בטחון גבוה
        long highConfidenceConnections = connections.stream()
                .filter(conn->conn.getConfidence()>0.8)
                .count();

        //תופעות לוואי מתרופות
        long sideEffectConnections = connections.stream()
                .filter(conn->conn.getType()== MedicalConnection.ConnectionType.SIDE_EFFECT)
                .filter(conn->conn.getConfidence()>0.6)
                .count();
        if (sideEffectConnections > 0) {
            return TreatmentPlan.UrgencyLevel.MEDIUM;
        }

        if (highConfidenceConnections > 2) {
            return TreatmentPlan.UrgencyLevel.HIGH;
        }

        return TreatmentPlan.UrgencyLevel.LOW;
    }

    //קביעת הדאגה העיקרית
    private String determineMainConcern(List<MedicalConnection> connections, Set<ExtractedSymptom> symptoms) {
        //תופעות לוואי של תרופות
        Optional<MedicalConnection> sideEffect = connections.stream()
                .filter(conn->conn.getType() == MedicalConnection.ConnectionType.SIDE_EFFECT)
                .max(Comparator.comparingDouble(MedicalConnection::getConfidence));
        if (sideEffect.isPresent()) {
            return String.format("Suspected side effect of the medication %s", sideEffect.get().getFromEntity());
        }
        // חיפוש קשרים למחלות קיימות
        Optional<MedicalConnection> diseaseConnection = connections.stream()
                .filter(conn -> conn.getType() == MedicalConnection.ConnectionType.DISEASE_SYMPTOM)
                .max(Comparator.comparingDouble(MedicalConnection::getConfidence));

        if (diseaseConnection.isPresent()) {
            return String.format("Symptoms may be related to existing disease: %s", diseaseConnection.get().getFromEntity());
        }
        //ברירת מחדל
        return "Analysis of reported symptoms";
    }

    //בניית הסבר למשתמש
    private String buildReasoning(List<MedicalConnection> connections, TreatmentPlan.UrgencyLevel urgencyLevel) {
        StringBuilder reasoning = new StringBuilder();
        switch (urgencyLevel) {
            case EMERGENCY:
                reasoning.append("Symptoms that require immediate medical attention have been identified.");
                break;
            case HIGH:
                reasoning.append("Important connections have been found that require immediate medical consultation.");
                break;
            case MEDIUM:
                reasoning.append("Medical connections were found that should be checked with a doctor.");
                break;
            case LOW:
                reasoning.append("Medical connections were found that require follow-up.");
                break;
        }

        if(!connections.isEmpty()) {
            long sideEffects = connections.stream()
                    .filter(conn->conn.getType()==MedicalConnection.ConnectionType.SIDE_EFFECT)
                    .count();
            if(sideEffects>0) {
                reasoning.append(String.format("%d possible drug side effects links identified.",sideEffects));
            }
            long diseasesSymptoms = connections.stream()
                    .filter(con->con.getType()==MedicalConnection.ConnectionType.DISEASE_SYMPTOM)
                    .count();
            if(diseasesSymptoms>0){
                reasoning.append(String.format("%d symptoms typical of diseases were identified",diseasesSymptoms));
            }
        }
        reasoning.append("These recommendations are based on general medical information and are not a substitute for consulting a doctor.");
        return reasoning.toString();
    }

    //פעולות מיידיות
    private List<ImmediateAction> generateImmediateActions(List<MedicalConnection> connections, TreatmentPlan.UrgencyLevel urgencyLevel) {
        List<ImmediateAction> actions = new ArrayList<>();
        if (urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY) {
            ImmediateAction emergency = new ImmediateAction();
            emergency.setType(ImmediateAction.ActionType.CALL_EMERGENCY);
            emergency.setDescription("Call emergency services immediately!!");
            emergency.setReason("Symptoms that require immediate treatment have been identified.");
            emergency.setPriority(1);
            actions.add(emergency);
        }
        //בדיקת תופעות לוואי חמורות
        List<MedicalConnection> sideEffects = connections.stream()
                .filter(conn->conn.getType()==MedicalConnection.ConnectionType.SIDE_EFFECT)
                .filter(conn->conn.getConfidence()>0.7)
                .collect(Collectors.toList());
        for (MedicalConnection sideEffect : sideEffects) {
            ImmediateAction action = new ImmediateAction();
            action.setType(ImmediateAction.ActionType.STOP_MEDICATION);
            action.setDescription("Consider stopping the medication.");
            action.setReason("Suspected side effect");
            action.setPriority(2);
            actions.add(action);
        }
        //מעקב אחר סימפטומים
        if(urgencyLevel == TreatmentPlan.UrgencyLevel.MEDIUM || urgencyLevel == TreatmentPlan.UrgencyLevel.LOW) {
            ImmediateAction monitor = new ImmediateAction();
            monitor.setType(ImmediateAction.ActionType.MONITOR_SYMPTOMS);
            monitor.setDescription("Monitor symptoms and record changes");
            monitor.setReason("It is important to monitor the development of the situation");
            monitor.setPriority(3);
            actions.add(monitor);
        }
        return actions;
    }

    //בדיקות מומלצות
    private List<MedicalTest> generateRecommendedTests(Set<ExtractedSymptom> symptoms, TreatmentPlan.UrgencyLevel urgencyLevel) {
        List<MedicalTest> tests = new ArrayList<>();

        // מיפוי סימפטומים לבדיקות
        Map<String, MedicalTest.TestType> symptomToTest = Map.of(
                "chest pain", MedicalTest.TestType.ECG,
                "heart", MedicalTest.TestType.ECG,
                "fever", MedicalTest.TestType.BLOOD_TEST,
                "headache", MedicalTest.TestType.BLOOD_PRESSURE,
                "pain", MedicalTest.TestType.BLOOD_TEST
        );
        Set<MedicalTest.TestType> recommendedTestTypes = new HashSet<>();

        for(ExtractedSymptom symptom : symptoms) {
            String symptomName = symptom.getName().toLowerCase();
            for(Map.Entry<String, MedicalTest.TestType> entry : symptomToTest.entrySet()) {
                if(symptomName.contains(entry.getKey()))
                    recommendedTestTypes.add(entry.getValue());
            }
        }
        //יצירת בדיקות לפי רמת דחיפות
        String urgency = urgencyLevel == TreatmentPlan.UrgencyLevel.EMERGENCY?"ASAP":
                urgencyLevel == TreatmentPlan.UrgencyLevel.HIGH?"Within 24h": "Without week";
        for(MedicalTest.TestType testType : recommendedTestTypes) {
            MedicalTest test = new MedicalTest();
            test.setType(testType);
            test.setDescription(testType.getDescription());
            test.setReason("Test relevant to reported symptoms");
            test.setUrgency(urgency);
            tests.add(test);
        }
        return tests;

    }

    //המלצות לביקורי רופא
    private List<DoctorVisit> generateDoctorVisits(TreatmentPlan.UrgencyLevel urgencyLevel, List<MedicalConnection> connections) {
        List<DoctorVisit> visits = new ArrayList<>();

        String urgency=switch (urgencyLevel){
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
        }else{
            DoctorVisit familyDoctor = new DoctorVisit();
            familyDoctor.setType(DoctorVisit.DoctorType.FAMILY_DOCTOR);
            familyDoctor.setReason("tests and medical recommendations");
            familyDoctor.setUrgency(urgency);
            visits.add(familyDoctor);
        }
        return visits;
    }

    //מידע נוסף
    private Map<String, Object> buildAdditionalInfo(UserMedicalContext userContext, List<MedicalConnection> connections) {
        Map<String, Object> info = new HashMap<>();

        info.put("userRiskLevel", userContext.getRiskLevel());
        info.put("userRiskScore", userContext.getOverallRiskScore());
        info.put("totalConnections", connections.size());
        info.put("analysisTimestamp", System.currentTimeMillis());

        return info;
    }

    //חיפוש כל הקשרים הרפואיים הרלוונטיים
    private List<MedicalConnection> findAllMedicalConnections(UserMedicalContext userContext, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> allConnections = new ArrayList<>();
        try{
            //קשרים בין תרופות ותופעות לוואי
            List<MedicalConnection> sideEffects = pathfindingService.findMedicationSideEffects(userContext.getCurrentMedications(), symptoms);
            allConnections.addAll(sideEffects);
            logger.debug("Found {} side effects",sideEffects.size());
            //קשרים בין מחלות וסימפטומים
            List<MedicalConnection> diseasesSymptoms = pathfindingService.findDiseaseSymptoms(userContext.getCurrentMedications(), symptoms);
            allConnections.addAll(diseasesSymptoms);
            logger.debug("Found {} diseases-symptom connections", diseasesSymptoms.size());
            //טיפולים אפשריים
            List<MedicalConnection> treatments = pathfindingService.findPossibleTreatments(symptoms);
            allConnections.addAll(treatments);
            logger.debug("Found {} treatments", treatments.size());
        } catch (Exception e) {
            logger.error("Error finding medical connections: {}",e.getMessage(), e);
        }
        return allConnections;
    }

    //תכנית חירום בסיסית למקרה של שגיאה
    private TreatmentPlan createEmrgencyPlan(Set<ExtractedSymptom> symptoms) {
        logger.warn("Creating emergency treatment plan due to analysis error");
        TreatmentPlan emergencyPlan = new TreatmentPlan();
        emergencyPlan.setUrgencyLevel(TreatmentPlan.UrgencyLevel.MEDIUM);
        emergencyPlan.setMainConcern("Cannot be fully analyzed - recommendation to consult a doctor");
        emergencyPlan.setReasoning("An error occurred during the operation. It is recommended that you consult a doctor for clarification.");

        //פעולה מיידית בסיסית
        ImmediateAction action = new ImmediateAction();
        action.setType(ImmediateAction.ActionType.SEEK_IMMEDIATE_CARE);
        action.setDescription("Cannot be fully analyzed - recommendation to consult a doctor");
        action.setReason("The situation cannot be fully analyzed.");
        action.setPriority(1);

        emergencyPlan.setImmediateActions(List.of(action));

        //ביקור רופא
        DoctorVisit visit = new DoctorVisit();
        visit.setType(DoctorVisit.DoctorType.FAMILY_DOCTOR);
        visit.setReason("Medical condition investigation");
        visit.setUrgency("Within few days");
        emergencyPlan.setDoctorVisits(List.of(visit));

        emergencyPlan.setRecommendedTests(new ArrayList<>());
        emergencyPlan.setFoundConnections(new ArrayList<>());
        emergencyPlan.setAdditionalInfo(Map.of("error","Analysis failed"));

        return emergencyPlan;
    }

    //קבלת מידע רפואי של משתמש
    public UserMedicalContext getUserMedicalContext(UUID userId) {
        return medicalContextService.getUserMedicalContext(userId);
    }


}
