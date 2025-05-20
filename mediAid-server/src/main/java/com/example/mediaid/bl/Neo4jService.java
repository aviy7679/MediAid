package com.example.mediaid.bl;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class Neo4jService {

    private final Driver neo4jDriver;

    public Neo4jService(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    public void initializeDatabase() {
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx->{
               //אילוצים לייחודיות של CUI
               tx.run("CREATE CONSTRAINT IF NOT EXIST FOR (d:Disease) REQUIRE d.cui IS UNIQUE");
               tx.run("CREATE CONSTRAINT IF NOT EXIST FOR (m:Medication) REQUIRE m.cui IS UNIQUE");
               tx.run("CREATE CONSTRAINT IF NOT EXIST FOR (s:Symptom) REQUIRE d.cui IS UNIQUE");
               return null;
            });
        }
    }

    //הוספת מחלה
    public void addDisease(String cui, String name){
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx->
                    tx.run("MERGE (d:Disease {cui: $cui}) SET d.name = $name",
                            Map.of("cui", cui, "name", name)));
        }
    }

    //הוספת תרופה
    public void addMedication(String cui, String name){
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx->
                    tx.run("MERGE (m:Medication {cui: $cui}) SET m.name = $name",
                            Map.of("cui", cui, "name", name)));
        }
    }

    //הוספת סימפטום
    public void addSymptom(String cui, String name){
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx->
                    tx.run("MERGE (s:Symptom {cui: $cui}) SET s.name = $name",
                            Map.of("cui", cui, "name", name)));
        }
    }

    //קשר בין סימפטום ומחלה
    public void createSymptomDiseaseRelation(String symptomCui, String diseaseCui,double weight){
        try(Session session = neo4jDriver.session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("symptomCui", symptomCui);
            params.put("diseaseCui",diseaseCui);
            params.put("weight",weight);

            session.writeTransaction(tx->
                    tx.run(
                            "MATCH (s:Symptom {cui:$symptomCui})"+
                                    "MATCH (d:Disease {cui:$diseaseCui})"+
                                    "MERGE (s)-[r:INDICATES {weight: $weight}]->(d)"+
                                    "RETURN s, r, d",
                            params
                    ));
        }
    }

    //  קשר בין תרופה למחלה
    public void createMedicationDiseasesRelation(String medicationCui, String diseaseCui, double efficacy) {
        try (Session session = neo4jDriver.session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("medicationCui", medicationCui);
            params.put("diseaseCui", diseaseCui);
            params.put("efficacy", efficacy);

            session.writeTransaction(tx ->
                    tx.run(
                            "MATCH (m:Medication {cui: $medicationCui}) " +
                                    "MATCH (d:Disease {cui: $diseaseCui}) " +
                                    "MERGE (m)-[r:TREATS {efficacy: $efficacy}]->(d) " +
                                    "RETURN m, r, d",
                            params
                    )
            );
        }
    }


}



