package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.BaseUmlsEntity;
import com.example.mediaid.dal.UMLS_terms.UmlsTerm;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Function;

import static com.example.mediaid.constants.DatabaseConstants.*;

public abstract class GenericUmlsProcessor<T extends BaseUmlsEntity> {
    private static final Logger logger = LoggerFactory.getLogger(GenericUmlsProcessor.class);

    @PersistenceContext
    protected EntityManager entityManager;  //אוביקט עבודה מול מסד נתונים

    @Autowired
    protected PlatformTransactionManager transactionManager; //מנהל הטרנזקציה - האפשרות להרצת טרנזקציות

    protected TransactionTemplate transactionTemplate; //עטיפה לכתיבת הקוד בתוך טרנזקציה
    protected final JpaRepository<T, Long> repository;
    protected final Set<String> semanticTypes;
    protected final List<String> preferredSources;
    protected final Function<String,T> entityCreator;
    protected final String entityTypeName;

    /**
     * יוצר מעבד גנרי חדש
     * @param repository - מאגר לשמירת הישויות
     * @param semanticTypes - סוגים סמנטיים שיש לחפש
     * @param preferredSources - מקורות מועדפים לבחירת מונחים
     * @param entityCreator - פונקציה ליצירת ישות חדשה
     * @param entityTypeName - שם סוג הישות (למטרות לוג)
     */
    public GenericUmlsProcessor(
            JpaRepository<T, Long> repository,
            Set<String> semanticTypes,
            List<String> preferredSources,
            Function<String, T> entityCreator,
            String entityTypeName){
        this.repository = repository;
        this.semanticTypes = semanticTypes;
        this.preferredSources = preferredSources;
        this.entityCreator = entityCreator;
        this.entityTypeName = entityTypeName;
    }

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    protected void insertIntoDatabase(Map<String, String> terms){
        final int totalSize = terms.size();
        int processedCount = 0;
        int skippedCount = 0;

        List<List<Map.Entry<String, String>>> batches = new ArrayList<>();
        List<Map.Entry<String, String>> currentBatch = new ArrayList<>();

        for (Map.Entry<String, String> entry : terms.entrySet()) {
            currentBatch.add(entry);

            if(currentBatch.size() >= SMALL_BATCH_SIZE){
                batches.add(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }

        //השארית
        if(currentBatch.size() > 0){
            batches.add(currentBatch);
        }

        //עיבוד כל אצווה בטרנזקציה נפרדת
        for (List<Map.Entry<String, String>> batch : batches) {
            final List<Map.Entry<String, String>> batchToProcess = batch;
            //צריך לקבל אובייקט TransactionCallback שמממש doInTransaction
            Integer batchResult = transactionTemplate.execute(new TransactionCallback<Integer>() {
                public Integer doInTransaction(TransactionStatus status) {
                    int localSkipped = 0;
                    try{
                        for(Map.Entry<String, String> entry : batchToProcess){
                            String cui = entry.getKey();
                            String name = entry.getValue();

                            //קיצור שם אם הוא ארוך מדי למסד
                            if(name != null && name.length() > MAX_ENTITY_NAME_LENGTH){
                                logger.warn("Shortening long name: " + name);
                                name = name.substring(0, MAX_ENTITY_NAME_LENGTH);
                                localSkipped++;
                            }

                            //אובייקט חדש
                            T entity = entityCreator.apply(name);
                            entity.setCui(cui);
                            entity.setName(name);

                            entityManager.persist(entity);
                        }
                        return localSkipped;
                    }catch(Exception e){
                        logger.warn("Error in batch processing");
                        status.setRollbackOnly();
                        throw e;
                    }
                }
            });

            if (batchResult != null)
                skippedCount += batchResult;

            processedCount += batch.size();

            // דיווח התקדמות
            if (processedCount % (BATCH_REPORT_INTERVAL * SMALL_BATCH_SIZE) == 0) {
                logger.info("Processed {} entities of type {} from {}", processedCount, entityTypeName, totalSize);
            }

            // הפסקה קטנה בין אצוות
            try {
                Thread.sleep(BATCH_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("{} entities of type {} were inserted into the database", processedCount, entityTypeName);
        if (skippedCount > 0)
            logger.info("Skipped {} entities due to name length issues", skippedCount);
    }

    public void processAndSave(){
        try{
            if(repository.count() > 0){
                logger.info("The data for {} already exists in the database. Skipping import.", entityTypeName);
                return;
            }

            logger.info("Processing data for {} from file MRSTY", entityTypeName);
            Set<String> entityCuis = UmlsTermHelper.loadCuisBySemanticTypes(semanticTypes);
            logger.info("Loaded {} cuis", entityCuis.size());

            logger.info("Processing data for {} from file MRCONSO", entityTypeName);
            Map<String,List<UmlsTerm>> allTerms = UmlsTermHelper.loadTermsForCuis(entityCuis);

            Map<String,String> selectedTerms = new HashMap<>();
            for(Map.Entry<String,List<UmlsTerm>> entry : allTerms.entrySet()){
                String bestName = UmlsTermHelper.chooseBestTerm(entry.getValue(), preferredSources);
                if(bestName != null){
                    selectedTerms.put(entry.getKey(), bestName);
                }
            }
            logger.info("Selected {} favorite terms", selectedTerms.size());

            logger.info("Inserting data into the database.");
            insertIntoDatabase(selectedTerms);

            logger.info("The loading process {} completed successfully.", entityTypeName);

        }catch (Exception e){
            logger.error("Error processing {}: {}", entityTypeName, e.getMessage(), e);
        }
    }
}