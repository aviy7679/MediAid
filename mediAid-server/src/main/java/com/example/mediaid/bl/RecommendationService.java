package com.example.mediaid.bl;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private final Driver neo4jDriver;

    public RecommendationService(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }


    //מציאת מחלות לפי סימפטומים
    public List<Map<String, Object>> findPossibleDisease(List<String> symptomsCuis){
        try(Session session = neo4jDriver.session()) {
            return session.readTransaction(tx->{
            Map<String,Object> params = new HashMap<>();
            params.put("symptomsCuis",symptomsCuis);

            Result result = tx.run(
                    "MATCH (s:Symptom)-[r:INDICATES]->(d:Disease) "+
                            "WHERE s.cui IN $symptomCuis "+
                            "WITH d, sum(r.weight) AS score, count(s) AS symptomCount "+
                            "RETURN d.cui AS cui, d.name AS name, "+
                            "score/symptomCount AS probability ORDER BY probability DESC LIMIT 10",
                    params
            );

            List<Map<String, Object>> diseases = new ArrayList<>();
            while(result.hasNext()){
                Record record = result.next();
                Map<String, Object> disease = new HashMap<>();
                disease.put("cui",record.get("cui").asString());
                disease.put("name",record.get("name").asString());
                disease.put("probability",record.get("probability").asDouble());
                diseases.add(disease);
            }
            return diseases;
            });


        }
    }

    //מציאת טיפולים מומלצים למחלה
    public List<Map<String, Object>> findRecommendedMedications(String diseasesCui){
        try(Session session = neo4jDriver.session()) {
            return session.readTransaction(tx->{
                Map<String,Object> params = new HashMap<>();
                params.put("diseaseCui",diseasesCui);

                Result result = tx.run(
                        "MATCH (d:Disease {cui: $diseaseCui})<-[r:TREATS]-(m:Medication) " +
                                "RETURN m.cui AS cui, m.name AS name, r.efficacy AS efficacy " +
                                "ORDER BY r.efficacy DESC LIMIT 5",
                        params
                );

                List<Map<String, Object>> medications = new ArrayList<>();
                while(result.hasNext()){
                    Record record = result.next();
                    Map<String, Object> medication = new HashMap<>();
                    medication.put("cui",record.get("cui").asString());
                    medication.put("name",record.get("name").asString());
                    if(record.get("efficacy")!=null){
                        medication.put("efficacy",record.get("efficacy").asDouble());
                    }else{
                        medication.put("efficacy",0.0);
                    }
                    medications.add(medication);
                }
                return medications;
            });
        }
    }

}
