package com.example.mediaid.bl.emergency;

import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.example.mediaid.dto.emergency.MedicalConnection;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//מסלולים ושאילתות neo4j
@Service
public class Neo4jPathfindingService {

    private static Logger logger = LoggerFactory.getLogger(Neo4jPathfindingService.class);

    @Autowired
    private Driver neo4jDriver;

    //    קשר בין תרופות לבין סימפטומים של המשתמש
    public List<MedicalConnection> findMedicationSideEffects(List<UserMedicalEntity> medications, List<ExtractedSymptom> symptoms) {
        List<MedicalConnection> connections = new ArrayList<>();

        try(Session session = neo4jDriver.session()) {
            for (UserMedicalEntity medication : medications) {
                for (ExtractedSymptom symptom : symptoms) {
                    String query = """
                        MATCH (med:Medication {cui: $medCui})-[r:CAUSES_SIDE_EFFECT]->(symp:Symptom {cui: $sympCui})
                        RETURN med.name as medName, symp.name as sympName, r.weight as confidence, r.source as source
                        UNION
                        MATCH (med:Medication {cui: $medCui})-[r:SIDE_EFFECT_OF]-(symp:Symptom {cui: $sympCui})
                        RETURN med.name as medName, symp.name as sympName, r.weight as confidence, r.source as source
                """;

                    // תיקון: עיבוד נכון של התוצאות
                    List<Record> records = session.readTransaction(tx ->
                            tx.run(query, Map.of("medCui", medication.getCui(), "sympCui", symptom.getCui())).list()
                    );

                    for (Record record : records) {
                        try {
                            MedicalConnection connection = new MedicalConnection();
                            connection.setType(MedicalConnection.ConnectionType.SIDE_EFFECT);
                            connection.setFromEntity(record.get("medName").asString());
                            connection.setToEntity(record.get("sympName").asString());
                            connection.setFromCui(medication.getCui());
                            connection.setToCui(symptom.getCui());
                            connection.setConfidence(record.get("confidence").asDouble());
                            connection.setExplanation(String.format("The drug %s may cause a side effect: %s",
                                    medication.getName(), symptom.getName()));
                            connections.add(connection);

                            logger.info("Found side effect connection: {} -> {}", medication.getName(), symptom.getName());
                        } catch (Exception e) {
                            logger.warn("Error processing side effect record: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error finding medication side effects: {}", e.getMessage(), e);
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

                    List<Record> records = session.readTransaction(tx ->
                            tx.run(query, Map.of("disCui", disease.getCui(), "sympCui", symptom.getCui())).list());

                    for (Record record : records) {
                        try {
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
                        } catch (Exception e) {
                            logger.warn("Error processing disease-symptom record: {}", e.getMessage());
                        }
                    }
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

                // תיקון: צריכה נכונה של התוצאות - החזרת list במקום Result
                List<Record> records = session.readTransaction(tx ->
                        tx.run(treatmentQuery, Map.of("sympCui", symptom.getCui())).list());

                // עיבוד התוצאות
                for (Record record : records) {
                    try {
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
                    } catch (Exception e) {
                        logger.warn("Error processing treatment record: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error finding possible treatments: {}", e.getMessage(), e);
        }

        return connections;
    }
}