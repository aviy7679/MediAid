package com.example.mediaid.bl.emergency;

import com.example.mediaid.dto.emergency.ExtractedSymptom;
import com.example.mediaid.dto.emergency.MedicalConnection;
import com.example.mediaid.dto.emergency.UserMedicalEntity;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class Neo4jPathfindingService {

    private static Logger logger = LoggerFactory.getLogger(Neo4jPathfindingService.class);

    @Autowired
    private Driver neo4jDriver;

    //קשר בין תרופות לבין סימפטומים של המשתמש
//    public List<MedicalConnection> findMedicationSideEffects(List<UserMedicalEntity> medications, List<ExtractedSymptom> symptoms) {
//        List<MedicalConnection> connections = new ArrayList<>();
//
//        try(Session session = neo4jDriver.session()) {
//            for (UserMedicalEntity medication : medications) {
//                for (ExtractedSymptom symptom : symptoms) {
//
//                    //חיפוש תופעות לוואי
//
//                }
//            }
//        }
//    }
}
