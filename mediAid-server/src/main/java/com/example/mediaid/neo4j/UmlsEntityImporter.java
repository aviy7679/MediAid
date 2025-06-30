package com.example.mediaid.neo4j;


import com.example.mediaid.bl.DemoMode;
import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import com.example.mediaid.dal.UMLS_terms.Symptom;
import com.example.mediaid.dal.UMLS_terms.SymptomRepository;

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


    @Autowired
    public UmlsEntityImporter(Driver neo4jDriver,
                              DiseaseRepository diseaseRepository,
                              MedicationRepository medicationRepository,
                              SymptomRepository symptomRepository) {
        super(neo4jDriver);
        this.diseaseRepository = diseaseRepository;
        this.medicationRepository = medicationRepository;
        this.symptomRepository = symptomRepository;
    }

    //ייבוא כל הישויות מהפוסטרגס ל-NEO4J
    public void importAllEntitiesFromDB(){
        logger.info("Start importing all entities from PostGreSQL");
        try{
            List<EntityImportConfig<?>> importConfig = Arrays.asList(
                    new EntityImportConfig<>("Diseases",diseaseRepository, EntityTypes.DISEASE, this::mapDisease),
                    new EntityImportConfig<>("Medications", medicationRepository, EntityTypes.MEDICATION, this::mapMedication),
                    new EntityImportConfig<>("Symptoms", symptomRepository, EntityTypes.SYMPTOM, this::mapSymptom)
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
            //דפדוף לחיסכון בזיכרון
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
                    // אם צומת חדשה
                    queryBuilder.append("ON CREATE SET n.name = $name");

                    //ערכים נוספים אם מוגדרים
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
                    //אם יש לפחות רשומה אחת בתוצאה
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

    //למצב דמו
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

