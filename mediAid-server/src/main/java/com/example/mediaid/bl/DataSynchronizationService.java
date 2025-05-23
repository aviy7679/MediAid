//package com.example.mediaid.bl;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//@Service
//@EnableScheduling
//public class DataSynchronizationService {
//
//    private final DiseaseRepository diseaseRepository;
//    private final MedicationRepository medicationRepository;
//    private final Neo4jService neo4jService;
//
//    @Autowired
//    public DataSynchronizationService(
//            DiseaseRepository diseaseRepository,
//            MedicationRepository medicationRepository,
//            Neo4jService neo4jService){
//        this.diseaseRepository = diseaseRepository;
//        this.medicationRepository = medicationRepository;
//        this.neo4jService = neo4jService;
//    }
//
//    //סנכרון ראשוני
//    public void initialSync(){
//        neo4jService.initializeDatabase();
//        synchronizeDiseases();
//        synchronizeMedications();
//        System.out.println("Initial sync complete.");
//    }
//
//    // סנכרון תקופתי אוטומטי - פעם ביום
//    @Scheduled(cron = "0 0 0 * * ?")  // מופעל בחצות בכל יום
//    public void scheduledSync() {
//        System.out.println("Starting periodic synchronization...");
//        synchronizeDiseases();
//        synchronizeMedications();
//        System.out.println("Periodic synchronization completed successfully.");
//    }
//
//
//    //סנכרון PostgreSQL עם neo4j
//
//    private void synchronizeDiseases(){
//        Iterable<Disease> diseases = diseaseRepository.findAll();
//        int count = 0;
//
//        for(Disease disease:diseases){
//            neo4jService.addDisease(disease.getCui(),disease.getName());
//            count++;
//
//            //מדפיס התקדמות בכל 1000
//            if (count % 1000 == 0){
//                System.out.println("מחלות"+count+"סונכרנו");
//            }
//        }
//        System.out.println("רשומות" + count + " סנכרון מחלות הושלם: ");
//
//    }
//    private void synchronizeMedications(){
//        Iterable<Medication> medications = medicationRepository.findAll();
//        int count = 0;
//
//        for(Medication medication:medications){
//            neo4jService.addMedication(medication.getCui(),medication.getName());
//            count++;
//
//            if (count % 1000 == 0){
//                System.out.println("תרופות " + count + " סונכרנו");
//            }
//        }
//        System.out.println("רשומות" + count + " סנכרון תרופות הושלם: ");
//
//    }
//
//}
