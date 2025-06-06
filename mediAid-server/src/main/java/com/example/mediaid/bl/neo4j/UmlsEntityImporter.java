package com.example.mediaid.bl.neo4j;


import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import com.example.mediaid.dal.UMLS_terms.Symptom;
import com.example.mediaid.dal.UMLS_terms.SymptomRepository;
import com.example.mediaid.dal.UMLS_terms.RiskFactor;
import com.example.mediaid.dal.UMLS_terms.RiskFactorRepository;
import com.example.mediaid.dal.UMLS_terms.Procedure;
import com.example.mediaid.dal.UMLS_terms.ProcedureRepository;
import com.example.mediaid.dal.UMLS_terms.AnatomicalStructure;
import com.example.mediaid.dal.UMLS_terms.AnatomicalStructureRepository;
import com.example.mediaid.dal.UMLS_terms.LabTest;
import com.example.mediaid.dal.UMLS_terms.LabTestRepository;
import com.example.mediaid.dal.UMLS_terms.BiologicalFunction;
import com.example.mediaid.dal.UMLS_terms.BiologicalFunctionRepository;


import com.example.mediaid.dal.UMLS_terms.relationships.UmlsRelationship;
import com.example.mediaid.dal.UMLS_terms.relationships.UmlsRelationshipRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
public class UmlsEntityImporter extends UmlsImporter{

    private static final  int BATCH_SIZE = 5000;
    private static final int PAGE_SIZE = 1000;
    private static final Logger logger = LoggerFactory.getLogger(UmlsEntityImporter.class);

    //repo לקריאת מהפוסטגרס
    private final DiseaseRepository diseaseRepository;
    private final MedicationRepository medicationRepository;
    private final SymptomRepository symptomRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final ProcedureRepository procedureRepository;
    private final AnatomicalStructureRepository anatomicalStructureRepository;
    private final LabTestRepository labTestRepository;
    private final BiologicalFunctionRepository biologicalFunctionRepository;

    @Autowired
    public UmlsEntityImporter(Driver neo4jDriver,
                              DiseaseRepository diseaseRepository,
                              MedicationRepository medicationRepository,
                              SymptomRepository symptomRepository,
                              RiskFactorRepository riskFactorRepository,
                              ProcedureRepository procedureRepository,
                              AnatomicalStructureRepository anatomicalStructureRepository,
                              LabTestRepository labTestRepository,
                              BiologicalFunctionRepository biologicalFunctionRepository) {
        super(neo4jDriver);
        this.diseaseRepository = diseaseRepository;
        this.medicationRepository = medicationRepository;
        this.symptomRepository = symptomRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.procedureRepository = procedureRepository;
        this.anatomicalStructureRepository = anatomicalStructureRepository;
        this.labTestRepository = labTestRepository;
        this.biologicalFunctionRepository = biologicalFunctionRepository;
    }

    public void importAllEntitiesFromDB(){
        logger.info("Start importing all entities from PostGreSQL");
        try{
            List<EntityImportConfig<?>> importConfig = Arrays.asList(
                    new EntityImportConfig<>("Diseases",diseaseRepository, EntityTypes.DISEASE, this::mapDisease),
                    new EntityImportConfig<>("Medications", medicationRepository, EntityTypes.MEDICATION, this::mapMedication),
                    new EntityImportConfig<>("Symptoms", symptomRepository, EntityTypes.SYMPTOM, this::mapSymptom),
                    new EntityImportConfig<>("Risk factors", riskFactorRepository, EntityTypes.RISK_FACTOR,this::mapRiskFactor),
                    new EntityImportConfig<>("Procedures", procedureRepository, EntityTypes.PROCEDURE, this::mapProcedure),
                    new EntityImportConfig<>("Anatomical Structures", anatomicalStructureRepository, EntityTypes.ANATOMICAL_STRUCTURE, this::mapAnatomicalStructure),
                    new EntityImportConfig<>("Lab Tests", labTestRepository, EntityTypes.LABORATORY_TEST, this::mapLabTest),
                    new EntityImportConfig<>("Biological Functions", biologicalFunctionRepository, EntityTypes.BIOLOGICAL_FUNCTION, this::mapBiologicalFunction)

            );

            //ייבוא כל סוגי הישויות
            for (EntityImportConfig<?> config : importConfig){
                if (config.repository!=null){
                    importEntitiesGeneric(config);
                }else{
                    logger.info("The repository does not exist for {}", config.displayName);
                }
            }

            logger.info("End importing all entities ");
        }catch (Exception e){
            logger.error("Error while importing all entities");
            e.printStackTrace();
        }

    }


    //מתודה גנרית לייבוא ישויות
    private <T> void importEntitiesGeneric(EntityImportConfig<T> config){
        logger.info("Start importing {}", config.displayName);

        long total = config.repository.count();
        logger.info("Total entities to import: {}", total);

        int pageNumber = 0;
        int importedCount = 0;
        List<Map<String, Object>> batch = new ArrayList<>();

        Page<T> page;
        do{
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            page = config.repository.findAll(pageable);

            for(T entity : page.getContent()){
                try{
                    Map<String, Object> mappedEntity = config.mapper.apply(entity);
                    if(mappedEntity != null && mappedEntity.containsKey("cui")){
                        batch.add(mappedEntity);
                        importedCount++;
                    }

                    //עיבוד האצווה
                    if (batch.size() >= BATCH_SIZE){
                        createEntitiesInBatch(batch);
                        batch.clear();
                        logger.info("Imported {} entities from {}", importedCount, total);
                    }

                } catch (Exception e) {
                    logger.error("Error while processing entity {}", entity, e);
                }
            }
            pageNumber++;

        }while (page.hasNext());

    }

    //יצירת ישויות במקבץ אצוות בגרף
    private void createEntitiesInBatch(List<Map<String, Object>> entities) {
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                for (Map<String, Object> entity : entities) {
                    String entityType = (String) entity.get("type");

                    StringBuilder queryBuilder = new StringBuilder();
                    queryBuilder.append("MERGE (n:").append(entityType).append(" {cui: $cui}) ");
                    queryBuilder.append("ON CREATE SET n.name = $name");

                    for (String key : entity.keySet()) {
                        if (!key.equals("cui") && !key.equals("name") && !key.equals("type")) {
                            queryBuilder.append(", n.").append(key).append(" = $").append(key);
                        }
                    }

                    // הוספת מאפיין updated_at
                    queryBuilder.append(", n.updated_at = datetime()");

                    try {
                        tx.run(queryBuilder.toString(), entity);
                    } catch (Exception e) {
                        logger.warn("Error creating {}: {}", entity.get("cui"),e.getMessage());
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.warn("Error creating batch: {}", e.getMessage());
            e.printStackTrace();
        }
    }


    //פונקציות מיפוי לכל הסוגים
    private Map<String, Object> mapDisease(Disease disease){
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", disease.getCui());
        entity.put("name", disease.getName());
        entity.put("type", EntityTypes.DISEASE);

        return entity;
    }

    private Map<String, Object> mapMedication(Medication medication) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", medication.getCui());
        entity.put("name", medication.getName());
        entity.put("type", EntityTypes.MEDICATION);

        return entity;
    }

    private Map<String, Object> mapSymptom(Symptom symptom) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", symptom.getCui());
        entity.put("name", symptom.getName());
        entity.put("type", EntityTypes.SYMPTOM);


        return entity;
    }

    private Map<String, Object> mapRiskFactor(RiskFactor riskFactor) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", riskFactor.getCui());
        entity.put("name", riskFactor.getName());
        entity.put("type", EntityTypes.RISK_FACTOR);

        return entity;
    }

    private Map<String, Object> mapProcedure(Procedure procedure) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", procedure.getCui());
        entity.put("name", procedure.getName());
        entity.put("type", EntityTypes.PROCEDURE);

        return entity;
    }
    private Map<String, Object> mapAnatomicalStructure(AnatomicalStructure anatomicalStructure) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", anatomicalStructure.getCui());
        entity.put("name", anatomicalStructure.getName());
        entity.put("type", EntityTypes.ANATOMICAL_STRUCTURE);
        return entity;
    }

    private Map<String, Object> mapLabTest(LabTest labTest) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", labTest.getCui());
        entity.put("name", labTest.getName());
        entity.put("type", EntityTypes.LABORATORY_TEST);
        return entity;
    }

    private Map<String, Object> mapBiologicalFunction(BiologicalFunction biologicalFunction) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("cui", biologicalFunction.getCui());
        entity.put("name", biologicalFunction.getName());
        entity.put("type", EntityTypes.BIOLOGICAL_FUNCTION);
        return entity;
    }

    /**
     * בדיקת כמות הישויות שיובאו בכל סוג
     */
    public Map<String, Long> getImportStatistics() {
        Map<String, Long> stats = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                String[] entityTypes = {
                        EntityTypes.DISEASE, EntityTypes.MEDICATION, EntityTypes.SYMPTOM,
                        EntityTypes.RISK_FACTOR, EntityTypes.PROCEDURE, EntityTypes.ANATOMICAL_STRUCTURE,
                        EntityTypes.LABORATORY_TEST, EntityTypes.BIOLOGICAL_FUNCTION
                };

                for (String entityType : entityTypes) {
                    var result = tx.run("MATCH (n:" + entityType + ") RETURN count(n) as count");
                    if (result.hasNext()) {
                        long count = result.next().get("count").asLong();
                        if (count > 0) {
                            stats.put(entityType.toLowerCase() + "s", count);
                        }
                    }
                }

                return null;
            });
        }

        return stats;
    }

    /**
     * ייבוא סוג ישות מסוים בלבד
     */
    public <T> void importSpecificEntityType(String entityTypeName, JpaRepository<T, Long> repository,
                                             String neoEntityType, Function<T, Map<String, Object>> mapper) {

        EntityImportConfig<T> config = new EntityImportConfig<>(entityTypeName, repository, neoEntityType, mapper);
        importEntitiesGeneric(config);
    }



    /**
     * מחלקה לאחסון הגדרות ייבוא לכל סוג ישות
     */

    private static class EntityImportConfig<T>{
        final String displayName;
        final JpaRepository<T, Long> repository;
        final String entityType;
        final Function<T, Map<String, Object>> mapper;

        EntityImportConfig(String displayName, JpaRepository<T, Long> repository, String entityType, Function<T, Map<String, Object>> mapper){
            this.displayName = displayName;
            this.repository = repository;
            this.entityType = entityType;
            this.mapper = mapper;
        }
    }
    @Autowired
    private UmlsRelationshipRepository relationshipRepository;

    public void importRelationshipsFromDB() {
        logger.info("Starting relationship import from PostgreSQL to Neo4j");

        long totalRelationships = relationshipRepository.count();
        logger.info("Total relationships to import: {}", totalRelationships);

        if (totalRelationships == 0) {
            logger.info("No relationships found in PostgreSQL");
            return;
        }

        int pageNumber = 0;
        int importedCount = 0;
        List<Map<String, Object>> batch = new ArrayList<>();

        Page<UmlsRelationship> page;
        do {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            page = relationshipRepository.findAll(pageable);

            for (UmlsRelationship rel : page.getContent()) {
                Map<String, Object> relationshipData = new HashMap<>();
                relationshipData.put("cui1", rel.getCui1());
                relationshipData.put("cui2", rel.getCui2());
                relationshipData.put("relType", rel.getRelationshipType());
                relationshipData.put("weight", rel.getWeight());
                relationshipData.put("source", rel.getSource());

                batch.add(relationshipData);
                importedCount++;

                if (batch.size() >= BATCH_SIZE) {
                    createRelationshipsBatch(batch);
                    batch.clear();
                    logger.info("Imported {} relationships from {}", importedCount, totalRelationships);
                }
            }
            pageNumber++;
        } while (page.hasNext());

        if (!batch.isEmpty()) {
            createRelationshipsBatch(batch);
        }

        logger.info("Completed importing {} relationships to Neo4j", importedCount);
    }

//    private void createRelationshipsBatch(List<Map<String, Object>> relationships) {
//        try (Session session = neo4jDriver.session()) {
//            session.writeTransaction(tx -> {
//                int successCount = 0;
//                for (Map<String, Object> rel : relationships) {
//                    try {
//                        String query = "MATCH (n1), (n2) " +
//                                "WHERE n1.cui = $cui1 AND n2.cui = $cui2 " +
//                                "CREATE (n1)-[r:" + rel.get("relType") + " {" +
//                                "weight: $weight, source: $source}]->(n2) " +
//                                "RETURN 1 as created";
//
//                        var result = tx.run(query, rel);
//                        if (result.hasNext()) {
//                            successCount++;
//                        }
//                    } catch (Exception e) {
//                        logger.debug("Failed to create relationship '{}' -[{}]-> '{}': {}",
//                                rel.get("cui1"), rel.get("relType"), rel.get("cui2"), e.getMessage());
//                    }
//                }
//                logger.debug("Successfully created {} relationships in batch", successCount);
//                return null;
//            });
//        } catch (Exception e) {
//            logger.error("Error creating relationship batch: {}", e.getMessage());
//        }
//    }

    private void createRelationshipsBatch(List<Map<String, Object>> relationships) {
        if (relationships.isEmpty()) {
            return;
        }

        logger.info("🔄 Creating batch of {} relationships in Neo4j...", relationships.size());

        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                int successCount = 0;
                int errorCount = 0;

                for (Map<String, Object> rel : relationships) {
                    try {
                        String cui1 = (String) rel.get("cui1");
                        String cui2 = (String) rel.get("cui2");
                        String relType = (String) rel.get("relType");
                        Double weight = (Double) rel.get("weight");
                        String source = (String) rel.get("source");

                        // שאילתה פשוטה עם timeout
                        String query = "MATCH (n1 {cui: $cui1}), (n2 {cui: $cui2}) " +
                                "CREATE (n1)-[:" + relType + " {weight: $weight, source: $source}]->(n2)";

                        tx.run(query, Map.of(
                                "cui1", cui1,
                                "cui2", cui2,
                                "weight", weight != null ? weight : 0.5,
                                "source", source != null ? source : "UMLS"
                        ));

                        successCount++;

                        // הדפס התקדמות כל 100 קשרים
                        if (successCount % 100 == 0) {
                            logger.debug("Created {} relationships so far...", successCount);
                        }

                    } catch (Exception e) {
                        errorCount++;
                        if (errorCount <= 5) { // הדפס רק 5 שגיאות ראשונות
                            logger.debug("Failed to create relationship: {}", e.getMessage());
                        }
                    }
                }

                logger.info("✅ Batch completed: {} successful, {} errors", successCount, errorCount);
                return null;
            });

        } catch (Exception e) {
            logger.error("❌ Critical error in batch creation: {}", e.getMessage());
            // אל תזרוק חריגה - רק תתעד ותמשיך
        }
    }
    public Map<String, Long> getRelationshipStatistics() {
        Map<String, Long> stats = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                String[] relationshipTypes = {
                        RelationshipTypes.TREATS,
                        RelationshipTypes.INDICATES,
                        RelationshipTypes.HAS_SYMPTOM,
                        RelationshipTypes.CONTRAINDICATED_FOR,
                        RelationshipTypes.INTERACTS_WITH,
                        RelationshipTypes.SIDE_EFFECT_OF,
                        RelationshipTypes.CAUSES_SIDE_EFFECT,
                        RelationshipTypes.MAY_PREVENT,
                        RelationshipTypes.COMPLICATION_OF,
                        RelationshipTypes.AGGRAVATES,
                        RelationshipTypes.RISK_FACTOR_FOR,
                        RelationshipTypes.INCREASES_RISK_OF,
                        RelationshipTypes.DIAGNOSED_BY,
                        RelationshipTypes.DIAGNOSES,
                        RelationshipTypes.PRECEDES,
                        RelationshipTypes.LOCATED_IN,
                        RelationshipTypes.INHIBITS,
                        RelationshipTypes.STIMULATES
                };

                for (String relType : relationshipTypes) {
                    try {
                        var result = tx.run("MATCH ()-[r:" + relType + "]->() RETURN COUNT(r) as count");
                        if (result.hasNext()) {
                            long count = result.next().get("count").asLong();
                            if (count > 0) {
                                stats.put(relType, count);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error counting relationships of type {}: {}", relType, e.getMessage());
                    }
                }

                var totalResult = tx.run("MATCH ()-[r]->() RETURN COUNT(r) as total");
                if (totalResult.hasNext()) {
                    long total = totalResult.next().get("total").asLong();
                    stats.put("TOTAL_RELATIONSHIPS", total);
                }

                return null;
            });
        }

        return stats;
    }
    public void importDemoEntitiesFromDB() {
        if (!DemoMode.MODE) {
            logger.info("Not in demo mode - importing all entities");
            importAllEntitiesFromDB();
            return;
        }

        logger.info("Demo mode ON - importing only demo-relevant entities");

        try {
            // ייבוא מחלות רלוונטיות לדמו
            List<Disease> demoRelevantDiseases = diseaseRepository.findAll().stream()
                    .filter(d -> DemoMode.isRelevantForDemo(d.getCui()))
                    .toList();
            logger.info("Found {} demo-relevant diseases", demoRelevantDiseases.size());

            if (!demoRelevantDiseases.isEmpty()) {
                List<Map<String, Object>> diseaseBatch = demoRelevantDiseases.stream()
                        .map(this::mapDisease)
                        .toList();
                createEntitiesInBatch(diseaseBatch);
            }

            // ייבוא תרופות רלוונטיות לדמו
            List<Medication> demoRelevantMedications = medicationRepository.findAll().stream()
                    .filter(m -> DemoMode.isRelevantForDemo(m.getCui()))
                    .toList();
            logger.info("Found {} demo-relevant medications", demoRelevantMedications.size());

            if (!demoRelevantMedications.isEmpty()) {
                List<Map<String, Object>> medicationBatch = demoRelevantMedications.stream()
                        .map(this::mapMedication)
                        .toList();
                createEntitiesInBatch(medicationBatch);
            }

            // ייבוא סימפטומים רלוונטיים לדמו
            List<Symptom> demoRelevantSymptoms = symptomRepository.findAll().stream()
                    .filter(s -> DemoMode.isRelevantForDemo(s.getCui()))
                    .toList();
            logger.info("Found {} demo-relevant symptoms", demoRelevantSymptoms.size());

            if (!demoRelevantSymptoms.isEmpty()) {
                List<Map<String, Object>> symptomBatch = demoRelevantSymptoms.stream()
                        .map(this::mapSymptom)
                        .toList();
                createEntitiesInBatch(symptomBatch);
            }

            logger.info("Demo entity import completed successfully");

        } catch (Exception e) {
            logger.error("Error in demo entity import: {}", e.getMessage(), e);
            throw e;
        }
    }

}

