package com.example.mediaid.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class UmlsImporter {

    private static final Logger logger = LoggerFactory.getLogger(UmlsImporter.class);

    protected final Driver neo4jDriver;

    public UmlsImporter(Driver driver) {
        this.neo4jDriver = driver;
    }

    //עיבוד אצווה
    protected <T> void processBatch(List<T> batch, int batchSize,ProcessBatchFunction<T> processor) {
        try {
            if(batch.size() >= batchSize) {
                processor.process(batch);
                batch.clear();
            }
        }catch (Exception e) {
            logger.error("Error processing batch: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @FunctionalInterface //ממשק עם מתודה אחת
    protected interface ProcessBatchFunction<T> {
        void process(List<T> batch) throws Exception;
    }

    protected void executeNeo4jQuery(String query, Map<String, Object> params) {
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx->{
                tx.run(query, params);
                return null;
            });
        }catch (Exception e) {
            logger.error("Error executing query: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}