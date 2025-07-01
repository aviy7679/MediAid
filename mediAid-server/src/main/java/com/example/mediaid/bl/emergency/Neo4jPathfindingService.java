package com.example.mediaid.bl.emergency;

import com.example.mediaid.dto.emergency.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

//מסלולים ושאילתות neo4j
@Service
public class Neo4jPathfindingService {

    private static Logger logger = LoggerFactory.getLogger(Neo4jPathfindingService.class);

    @Autowired
    private Driver neo4jDriver;

//    קשר בין תרופות לבין סימפטומים של המשתמש
    public List<MedicalConnection> findMedicationSideEffects(List<UserMedicalEntity> medications, List<ExtractedSymptom> symptoms) {
        //רשימה שכל אובייקט בה הוא בעצם קשר בין תרופה לסימפטום
        List<MedicalConnection> connections = new ArrayList<>();

        try(Session session = neo4jDriver.session()) {
            for (UserMedicalEntity medication : medications) {
                for (ExtractedSymptom symptom : symptoms) {

                    //חיפוש תופעות לוואי
                    String query = """
                            MATCH (med:Medication {cui: $medCui})-[r:CAUSES_SIDE_EFFECT]->(symp:Symptom {cui: $sympCui})
                            RETURN med.name as medName, symp.name as sympName, r.weight as confidence, r.source as source
                            UNION
                            MATCH (med:Medication {cui: $medCui})-[r:SIDE_EFFECT_OF]-(symp:Symptom {cui: $sympCui})
                            RETURN med.name as medName, symp.name as sympName, r.weight as confidence, r.source as source
                    """;
                    var result = session.readTransaction(tx->
                            tx.run(query, Map.of("medCui", medication.getCui(), "sympCui", symptom.getCui())));
                    result.forEachRemaining(record -> {
                        MedicalConnection connection = new MedicalConnection();
                        connection.setType(MedicalConnection.ConnectionType.SIDE_EFFECT);
                        connection.setFromEntity(record.get("medName").asString());
                        connection.setToEntity(record.get("sympName").asString());
                        connection.setFromCui(medication.getCui());
                        connection.setToCui(symptom.getCui());
                        connection.setConfidence(record.get("confidence").asDouble());
                        connection.setExplanation(String.format("The drug %s may cause a side effect: %s", medication.getName(), symptom.getName()));
                        connections.add(connection);

                        logger.info("Found side effect connections: {} -> {}", medication.getName(), symptom.getName());
                    });

                }
            }
        } catch (Exception e) {
            logger.error("Error finding medication side effects: {}",e.getMessage(), e);
        }
        return connections;
    }


    //קשר בין מחלה וסימפטומים
    public List<MedicalConnection> findDiseaseSymptoms(List<UserMedicalEntity> diseases, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> connections = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            for (UserMedicalEntity disease : diseases) {
                for (ExtractedSymptom symptom : symptoms) {

                    String query = """
                            MATCH (dis:Disease {cui: $disCui})-[r:CAUSES_SYMPTOM]->(symp:Symptom {cui: $sympCui})
                            RETURN dis.name as disName, symp.name as sympName, r.weight as confidence, r.source as source
                            UNION
                            MATCH (dis:Disease {cui: $disCui})<-[r:INDICATES]-(symp:Symptom {cui: $sympCui})
                            RETURN dis.name as disName, symp.name as sympName, r.weight as confidence, r.source as source
                            """;

                    var result = session.readTransaction(tx ->
                            tx.run(query, Map.of("disCui", disease.getCui(), "sympCui", symptom.getCui())));

                    result.forEachRemaining(record -> {
                        MedicalConnection connection = new MedicalConnection();
                        connection.setType(MedicalConnection.ConnectionType.DISEASE_SYMPTOM);
                        connection.setFromEntity(record.get("disName").asString());
                        connection.setToEntity(record.get("sympName").asString());
                        connection.setFromCui(disease.getCui());
                        connection.setToCui(symptom.getCui());
                        connection.setConfidence(record.get("confidence").asDouble());
                        connection.setExplanation(
                                String.format("המחלה %s יכולה להיות הגורם לסימפטום: %s",
                                        disease.getName(), symptom.getName())
                        );
                        connections.add(connection);

                        logger.info("Found disease-symptom connection: {} -> {}",
                                disease.getName(), symptom.getName());
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Error finding disease symptoms: {}", e.getMessage(), e);
        }

        return connections;
    }

    //חיפוש טיפול אפשרי לסימפטום
    public List<MedicalConnection> findPossibleTreatments(List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> connections = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            for (ExtractedSymptom symptom : symptoms) {

                // חיפוש תרופות שמטפלות בסימפטום
                String treatmentQuery = """
                    MATCH (symp:Symptom {cui: $sympCui})<-[r:CAUSES_SYMPTOM]-(dis:Disease)<-[t:TREATS]-(med:Medication)
                    RETURN DISTINCT med.name as medName, med.cui as medCui, dis.name as disName, 
                           (r.weight + t.weight) / 2 as confidence
                    ORDER BY confidence DESC
                    LIMIT 5
                    """;

                var result = session.readTransaction(tx ->
                        tx.run(treatmentQuery, Map.of("sympCui", symptom.getCui())));

                result.forEachRemaining(record -> {
                    MedicalConnection connection = new MedicalConnection();
                    connection.setType(MedicalConnection.ConnectionType.DISEASE_SYMPTOM);
                    connection.setFromEntity(symptom.getName());
                    connection.setToEntity(record.get("medName").asString());
                    connection.setFromCui(symptom.getCui());
                    connection.setToCui(record.get("medCui").asString());
                    connection.setConfidence(record.get("confidence").asDouble());
                    connection.setExplanation(
                            String.format("התרופה %s עשויה לעזור בטיפול בסימפטום %s (דרך %s)",
                                    record.get("medName").asString(), symptom.getName(), record.get("disName").asString())
                    );
                    connections.add(connection);
                });
            }
        } catch (Exception e) {
            logger.error("Error finding possible treatments: {}", e.getMessage(), e);
        }

        return connections;
    }

    //חיפוש מסלול מלא
    public List<Map<String, Object>> findFullMedicalPath(String sourceCui, String symptomCui) {
        List<Map<String, Object>> paths = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            // מסלול: תרופה -> תופעת לוואי -> סימפטום -> טיפול אפשרי
            String pathQuery = """
                MATCH path = (source)-[r1]->(symptom:Symptom {cui: $sympCui})<-[r2:CAUSES_SYMPTOM]-(disease)<-[r3:TREATS]-(treatment)
                WHERE source.cui = $sourceCui
                RETURN path, 
                       source.name as sourceName, 
                       symptom.name as symptomName,
                       disease.name as diseaseName,
                       treatment.name as treatmentName,
                       type(r1) as relationshipType,
                       (r1.weight + r2.weight + r3.weight) / 3 as pathConfidence
                ORDER BY pathConfidence DESC
                LIMIT 3
                """;

            var result = session.readTransaction(tx ->
                    tx.run(pathQuery, Map.of("sourceCui", sourceCui, "sympCui", symptomCui)));

            result.forEachRemaining(record -> {
                Map<String, Object> pathInfo = new HashMap<>();
                pathInfo.put("source", record.get("sourceName").asString());
                pathInfo.put("symptom", record.get("symptomName").asString());
                pathInfo.put("disease", record.get("diseaseName").asString());
                pathInfo.put("treatment", record.get("treatmentName").asString());
                pathInfo.put("relationship_type", record.get("relationshipType").asString());
                pathInfo.put("confidence", record.get("pathConfidence").asDouble());
                paths.add(pathInfo);
            });

        } catch (Exception e) {
            logger.error("Error finding full medical path: {}", e.getMessage(), e);
        }

        return paths;
    }

    public List<MedicalTest> findRecommendedTests(Set<ExtractedSymptom> symptoms,
                                                  List<UserMedicalEntity> diseases,
                                                  TreatmentPlan.UrgencyLevel urgencyLevel) {
        List<MedicalTest> tests = new ArrayList<>();

        logger.debug("🔍 Finding recommended tests from graph for {} symptoms and {} diseases",
                symptoms.size(), diseases.size());

        try (Session session = neo4jDriver.session()) {
            // שליפה מהגרף: סימפטומים ומחלות -> בדיקות
            String testQuery = """
                MATCH (source)-[r:REQUIRES_TEST]->(t:Test)
                WHERE source.cui IN $cuis
                RETURN t.cui as testCui, t.name as testName, 
                       r.weight as confidence, source.name as sourceName,
                       coalesce(r.urgency, 'medium') as urgency
                ORDER BY r.weight DESC
                LIMIT 10
                """;

            // איסוף כל ה-CUIs
            Set<String> allCuis = new HashSet<>();
            symptoms.forEach(s -> allCuis.add(s.getCui()));
            diseases.forEach(d -> allCuis.add(d.getCui()));

            if (allCuis.isEmpty()) {
                return tests;
            }

            var result = session.readTransaction(tx ->
                    tx.run(testQuery, Map.of("cuis", new ArrayList<>(allCuis))));

            // המרה ל-MedicalTest
            result.forEachRemaining(record -> {
                MedicalTest test = new MedicalTest();
                String testName = record.get("testName").asString();

                // מיפוי פשוט לסוג בדיקה
                test.setType(mapToTestType(testName));
                test.setDescription(testName);
                test.setReason("Recommended for: " + record.get("sourceName").asString());
                test.setUrgency(determineUrgency(record.get("confidence").asDouble(), urgencyLevel));

                tests.add(test);
                logger.debug("Found test: {} (confidence: {})", testName, record.get("confidence").asDouble());
            });

        } catch (Exception e) {
            logger.error("Error finding tests from graph: {}", e.getMessage());
        }

        logger.debug("🔍 Found {} tests from graph", tests.size());
        return tests;
    }

    // פונקציות עזר פשוטות
    private MedicalTest.TestType mapToTestType(String testName) {
        String lower = testName.toLowerCase();
        if (lower.contains("ecg") || lower.contains("ekg")) return MedicalTest.TestType.ECG;
        if (lower.contains("blood") || lower.contains("cbc")) return MedicalTest.TestType.BLOOD_TEST;
        if (lower.contains("pressure")) return MedicalTest.TestType.BLOOD_PRESSURE;
        if (lower.contains("x-ray") || lower.contains("chest")) return MedicalTest.TestType.XRAY;
        if (lower.contains("ultrasound")) return MedicalTest.TestType.ULTRASOUND;
        if (lower.contains("ct")) return MedicalTest.TestType.CT_SCAN;
        if (lower.contains("mri")) return MedicalTest.TestType.MRI;
        if (lower.contains("urine")) return MedicalTest.TestType.URINE_TEST;
        if (lower.contains("glucose") || lower.contains("sugar")) return MedicalTest.TestType.BLOOD_SUGAR;
        return MedicalTest.TestType.BLOOD_TEST; // ברירת מחדל
    }

    private String determineUrgency(double confidence, TreatmentPlan.UrgencyLevel globalUrgency) {
        // אם הדחיפות הכללית גבוהה, נעדכן בהתאם
        if (globalUrgency == TreatmentPlan.UrgencyLevel.EMERGENCY) return "ASAP";
        if (globalUrgency == TreatmentPlan.UrgencyLevel.HIGH) return "Within 24h";

        // אחרת, על בסיס confidence
        if (confidence >= 0.8) return "Within 24h";
        if (confidence >= 0.6) return "Within week";
        return "Within month";
    }
}