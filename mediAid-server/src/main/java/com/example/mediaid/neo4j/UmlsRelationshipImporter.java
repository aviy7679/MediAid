//package com.example.mediaid.neo4j;
//
//import com.example.mediaid.bl.DemoMode;
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.Session;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.*;
//
///**
// * שירות לייבוא קשרים רפואיים ישירות מקובץ MRREL
// */
//@Service
//public class UmlsRelationshipImporter extends UmlsImporter {
//
//    private static final Logger logger = LoggerFactory.getLogger(UmlsRelationshipImporter.class);
//
//    // סטים של CUI עבור כל סוג ישות
//    private Set<String> diseaseCuis = new HashSet<>();
//    private Set<String> medicationCuis = new HashSet<>();
//    private Set<String> symptomCuis = new HashSet<>();
//    private Set<String> riskFactorCuis = new HashSet<>();
//    private Set<String> procedureCuis = new HashSet<>();
//    private Set<String> anatomicalCuis = new HashSet<>();
//    private Set<String> labTestCuis = new HashSet<>();
//    private Set<String> biologicalFunctionCuis = new HashSet<>();
//
//    @Autowired
//    public UmlsRelationshipImporter(Driver driver) {
//        super(driver);
//    }
//
//    /**
//     * נקודת כניסה עיקרית לייבוא קשרים
//     */
//    public void importRelationships(String mrrelPath) {
//        logger.info(ImportConstants.Messages.IMPORT_STARTED);
//        logger.info("MRREL file path: {}", mrrelPath);
//
//        try {
//            // שלב הכנה
//            loadAllCuisFromGraph();
//            ensureIndexesExist();
//
//            // שלב ייבוא
//            importRelationshipsFromMrrel(mrrelPath);
//
//            logger.info(ImportConstants.Messages.IMPORT_COMPLETED);
//
//        } catch (Exception e) {
//            logger.error("Critical error in relationship import: {}", e.getMessage(), e);
//            throw new RuntimeException("Relationship import failed", e);
//        }
//    }
//
//    /**
//     * עיבוד קובץ MRREL וייבוא הקשרים
//     */
//    private void importRelationshipsFromMrrel(String mrrelPath) throws IOException {
//        logger.info("Processing MRREL file for relationship extraction");
//
//        // הגדרת משתני מעקב
//        RelationshipImportTracker tracker = new RelationshipImportTracker();
//        List<Map<String, Object>> relationshipBatch = new ArrayList<>();
//        Set<String> existingRelationships = loadExistingRelationships();
//
//        logger.info("Loaded {} existing relationships to prevent duplicates", existingRelationships.size());
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))) {
//            String line;
//            logger.info("Starting relationship processing");
//
//            while ((line = reader.readLine()) != null) {
//                tracker.totalLines++;
//
//                // דיווח התקדמות
//                if (tracker.totalLines % ImportConstants.PROGRESS_REPORT_INTERVAL == 0) {
//                    reportProgress(tracker);
//                }
//
//                // עיבוד השורה
//                RelationshipCandidate candidate = processLine(line, tracker, existingRelationships);
//
//                if (candidate != null) {
//                    relationshipBatch.add(candidate.toMap());
//                    tracker.acceptedRelationships++;
//                    existingRelationships.add(candidate.getRelationshipKey());
//
//                    // יצירת אצווה כשמגיעים לגודל המטרה
//                    if (relationshipBatch.size() >= ImportConstants.RELATIONSHIP_BATCH_SIZE) {
//                        int created = createRelationshipsBatch(relationshipBatch);
//                        tracker.totalCreated += created;
//                        relationshipBatch.clear();
//
//                        if (tracker.batchCount % ImportConstants.BATCH_REPORT_INTERVAL == 0) {
//                            logger.info("Processed {} batches, created {} relationships total",
//                                    tracker.batchCount, tracker.totalCreated);
//                        }
//                        tracker.batchCount++;
//                    }
//                }
//            }
//
//            // עיבוד אצווה אחרונה
//            if (!relationshipBatch.isEmpty()) {
//                int created = createRelationshipsBatch(relationshipBatch);
//                tracker.totalCreated += created;
//                tracker.batchCount++;
//            }
//
//            // הצגת סיכום מפורט
//            printImportSummary(tracker);
//
//        } catch (IOException e) {
//            logger.error(ImportConstants.Messages.ERROR_FILE_READ + ": {}", e.getMessage());
//            throw e;
//        }
//    }
//
//    /**
//     * עיבוד שורה בודדת מקובץ MRREL
//     */
//    private RelationshipCandidate processLine(String line, RelationshipImportTracker tracker,
//                                              Set<String> existingRelationships) {
//
//        String[] fields = line.split("\\|");
//        if (fields.length < ImportConstants.MIN_MRREL_FIELDS) {
//            tracker.skippedInvalidFormat++;
//            return null;
//        }
//
//        String cui1 = fields[0];
//        String cui2 = fields[4];
//        String rel = fields[3];
//        String rela = fields[7];
//        String sab = fields[10];
//
//        // בדיקות סינון
//
//        // 1. מניעת לולאות עצמיות
//        if (cui1.equals(cui2)) {
//            tracker.skippedSelfLoops++;
//            return null;
//        }
//
//        // 2. בדיקת מצב Demo
//        if (DemoMode.MODE && !DemoMode.isRelationshipRelevantForDemo(cui1, cui2)) {
//            tracker.skippedNonDemo++;
//            return null;
//        }
//
//        // 3. קביעת סוג הקשר
//        String relationshipType = determineRelationshipType(rel, rela);
//        if (relationshipType == null ||
//                ImportConstants.EXCLUDED_RELATIONSHIP_TYPES.contains(relationshipType.toLowerCase())) {
//            tracker.skippedInvalidRelType++;
//            return null;
//        }
//
//        // 4. בדיקת קיום צמתים
//        if (!nodeCuiExists(cui1) || !nodeCuiExists(cui2)) {
//            tracker.skippedMissingNodes++;
//            return null;
//        }
//
//        // 5. בדיקת כפילויות
//        String relationshipKey = createRelationshipKey(cui1, cui2, relationshipType);
//        if (existingRelationships.contains(relationshipKey)) {
//            tracker.skippedDuplicates++;
//            return null;
//        }
//
//        // יצירת מועמד קשר
//        double weight = RelationshipTypes.calculateRelationshipWeight(
//                rela != null && !rela.trim().isEmpty() ? rela : rel, sab);
//
//        return new RelationshipCandidate(cui1, cui2, relationshipType, weight, sab, rel, rela);
//    }
//
//    /**
//     * קביעת סוג הקשר מ-REL ו-RELA
//     */
//    private String determineRelationshipType(String rel, String rela) {
//        // בדיקת RELA תחילה (ספציפי יותר)
//        if (rela != null && !rela.trim().isEmpty()) {
//            String normalized = rela.trim().toLowerCase();
//            if (ImportConstants.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(normalized)) {
//                return ImportConstants.UMLS_TO_NEO4J_RELATIONSHIPS.get(normalized);
//            }
//            return normalizeRelationshipName(normalized);
//        }
//
//        // אם אין RELA, השתמש ב- REL
//        if (rel != null && !rel.trim().isEmpty()) {
//            String normalized = rel.trim().toUpperCase();
//            if (ImportConstants.REL_TO_RELATIONSHIP.containsKey(normalized)) {
//                return ImportConstants.REL_TO_RELATIONSHIP.get(normalized);
//            }
//            return normalizeRelationshipName(rel.trim().toLowerCase());
//        }
//
//        return null;
//    }
//
//    /**
//     * נרמול שם קשר לפורמט תקין של Neo4j
//     */
//    private String normalizeRelationshipName(String name) {
//        if (name == null || name.trim().isEmpty()) {
//            return ImportConstants.DEFAULT_RELATIONSHIP_TYPE;
//        }
//
//        String normalized = name.toUpperCase()
//                .replace(" ", "_")
//                .replace("-", "_")
//                .replaceAll("[^A-Z0-9_]", "")
//                .replaceAll("_+", "_")
//                .replaceAll("^_+|_+$", "");
//
//        if (normalized.isEmpty() || !Character.isLetter(normalized.charAt(0))) {
//            return ImportConstants.DEFAULT_RELATIONSHIP_TYPE;
//        }
//
//        if (normalized.length() > ImportConstants.MAX_RELATIONSHIP_NAME_LENGTH) {
//            normalized = normalized.substring(0, ImportConstants.MAX_RELATIONSHIP_NAME_LENGTH);
//        }
//
//        return normalized;
//    }
//
//    /**
//     * יצירת אצווה של קשרים ב-Neo4j
//     */
//    private int createRelationshipsBatch(List<Map<String, Object>> relationships) {
//        if (relationships.isEmpty()) return 0;
//
//        logger.debug(ImportConstants.Messages.CREATING_BATCH + " of {} relationships", relationships.size());
//
//        int successCount = 0;
//        int smallBatchSize = 50; // אצוות קטנות למניעת timeout
//
//        for (int i = 0; i < relationships.size(); i += smallBatchSize) {
//            int endIndex = Math.min(i + smallBatchSize, relationships.size());
//            List<Map<String, Object>> smallBatch = relationships.subList(i, endIndex);
//
//            successCount += createSmallBatch(smallBatch);
//
//            // הפסקה קטנה בין אצוות
//            if (i > 0 && i % 500 == 0) {
//                try {
//                    Thread.sleep(ImportConstants.BATCH_DELAY_MS);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        }
//
//        logger.debug(ImportConstants.Messages.BATCH_COMPLETED + " - {} relationships created", successCount);
//        return successCount;
//    }
//
//    /**
//     * יצירת אצווה קטנה של קשרים
//     */
//    private int createSmallBatch(List<Map<String, Object>> relationships) {
//        try (Session session = neo4jDriver.session()) {
//            return session.writeTransaction(tx -> {
//                int count = 0;
//
//                for (Map<String, Object> rel : relationships) {
//                    try {
//                        String cui1 = (String) rel.get("cui1");
//                        String cui2 = (String) rel.get("cui2");
//                        String relType = (String) rel.get("relType");
//                        double weight = (double) rel.get("weight");
//                        String source = (String) rel.get("source");
//
//                        String query = "MATCH (n1 {cui: $cui1}), (n2 {cui: $cui2}) " +
//                                "CREATE (n1)-[:" + relType + " {weight: $weight, source: $source}]->(n2)";
//
//                        tx.run(query, Map.of(
//                                "cui1", cui1,
//                                "cui2", cui2,
//                                "weight", weight,
//                                "source", source
//                        ));
//
//                        count++;
//
//                    } catch (Exception e) {
//                        logger.debug("Failed to create relationship: {}", e.getMessage());
//                    }
//                }
//
//                return count;
//            });
//
//        } catch (Exception e) {
//            logger.warn("Small batch creation failed: {}", e.getMessage());
//            return createRelationshipsIndividually(relationships);
//        }
//    }
//
//    /**
//     * יצירת קשרים אחד אחד במקרה של כשל באצווה
//     */
//    private int createRelationshipsIndividually(List<Map<String, Object>> relationships) {
//        int successCount = 0;
//
//        for (Map<String, Object> rel : relationships) {
//            try (Session session = neo4jDriver.session()) {
//                session.writeTransaction(tx -> {
//                    String cui1 = (String) rel.get("cui1");
//                    String cui2 = (String) rel.get("cui2");
//                    String relType = (String) rel.get("relType");
//                    double weight = (double) rel.get("weight");
//                    String source = (String) rel.get("source");
//
//                    String query = "MATCH (n1 {cui: $cui1}), (n2 {cui: $cui2}) " +
//                            "CREATE (n1)-[:" + relType + " {weight: $weight, source: $source}]->(n2)";
//
//                    tx.run(query, Map.of(
//                            "cui1", cui1,
//                            "cui2", cui2,
//                            "weight", weight,
//                            "source", source
//                    ));
//
//                    return null;
//                });
//
//                successCount++;
//
//            } catch (Exception e) {
//                logger.debug("Failed individual relationship creation: {}", e.getMessage());
//            }
//        }
//
//        return successCount;
//    }
//
//    /**
//     * טעינת כל ה-CUI מהגרף
//     */
//    private void loadAllCuisFromGraph() {
//        logger.info(ImportConstants.Messages.LOADING_NODES);
//
//        try (Session session = neo4jDriver.session()) {
//            for (String entityType : ImportConstants.INDEXED_ENTITY_TYPES) {
//                Set<String> cuis = loadCuisForType(session, entityType);
//                assignCuisToSet(entityType, cuis);
//                logger.debug("Loaded {} CUIs for entity type: {}", cuis.size(), entityType);
//            }
//
//            int totalCuis = diseaseCuis.size() + medicationCuis.size() + symptomCuis.size() +
//                    riskFactorCuis.size() + procedureCuis.size() + anatomicalCuis.size() +
//                    labTestCuis.size() + biologicalFunctionCuis.size();
//
//            logger.info("Loaded total {} CUIs from graph", totalCuis);
//        }
//    }
//
//    /**
//     * הקצאת CUI לסט המתאים
//     */
//    private void assignCuisToSet(String entityType, Set<String> cuis) {
//        switch (entityType) {
//            case EntityTypes.DISEASE -> diseaseCuis = cuis;
//            case EntityTypes.MEDICATION -> medicationCuis = cuis;
//            case EntityTypes.SYMPTOM -> symptomCuis = cuis;
//            case EntityTypes.RISK_FACTOR -> riskFactorCuis = cuis;
//            case EntityTypes.PROCEDURE -> procedureCuis = cuis;
//            case EntityTypes.ANATOMICAL_STRUCTURE -> anatomicalCuis = cuis;
//            case EntityTypes.LABORATORY_TEST -> labTestCuis = cuis;
//            case EntityTypes.BIOLOGICAL_FUNCTION -> biologicalFunctionCuis = cuis;
//        }
//    }
//
//    /**
//     * טעינת CUI עבור סוג ישות מסוים
//     */
//    private Set<String> loadCuisForType(Session session, String entityType) {
//        return new HashSet<>(
//                session.readTransaction(tx -> {
//                    var result = tx.run("MATCH (n:" + entityType + ") RETURN n.cui AS cui");
//                    List<String> cuis = new ArrayList<>();
//                    result.forEachRemaining(record -> {
//                        if (record.get("cui") != null) {
//                            cuis.add(record.get("cui").asString());
//                        }
//                    });
//                    return cuis;
//                })
//        );
//    }
//
//    /**
//     * בדיקת קיום CUI בגרף
//     */
//    private boolean nodeCuiExists(String cui) {
//        return diseaseCuis.contains(cui) ||
//                medicationCuis.contains(cui) ||
//                symptomCuis.contains(cui) ||
//                riskFactorCuis.contains(cui) ||
//                procedureCuis.contains(cui) ||
//                anatomicalCuis.contains(cui) ||
//                labTestCuis.contains(cui) ||
//                biologicalFunctionCuis.contains(cui);
//    }
//
//    /**
//     * יצירת אינדקסים לביצועים
//     */
//    private void ensureIndexesExist() {
//        logger.info("Creating performance indexes");
//
//        try (Session session = neo4jDriver.session()) {
//            session.writeTransaction(tx -> {
//                for (String entityType : ImportConstants.INDEXED_ENTITY_TYPES) {
//                    try {
//                        String indexQuery = "CREATE INDEX IF NOT EXISTS FOR (n:" + entityType + ") ON (n.cui)";
//                        tx.run(indexQuery);
//                    } catch (Exception e) {
//                        logger.debug("Index for {} might already exist", entityType);
//                    }
//                }
//                return null;
//            });
//
//            logger.info("Performance indexes created successfully");
//
//        } catch (Exception e) {
//            logger.warn("Could not create indexes: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * טעינת קשרים קיימים למניעת כפילויות
//     */
//    private Set<String> loadExistingRelationships() {
//        logger.debug("Loading existing relationships to prevent duplicates");
//        Set<String> existing = new HashSet<>();
//
//        try (Session session = neo4jDriver.session()) {
//            session.readTransaction(tx -> {
//                var result = tx.run(
//                        "MATCH (n1)-[r]->(n2) " +
//                                "WHERE n1.cui IS NOT NULL AND n2.cui IS NOT NULL " +
//                                "RETURN n1.cui as cui1, n2.cui as cui2, type(r) as relType " +
//                                "LIMIT 500000"
//                );
//
//                //יצירת מחרוזת שמבטאת את הקשר
//                result.forEachRemaining(record -> {
//                    String cui1 = record.get("cui1").asString();
//                    String cui2 = record.get("cui2").asString();
//                    String relType = record.get("relType").asString();
//                    existing.add(createRelationshipKey(cui1, cui2, relType));
//                });
//
//                return null;
//            });
//        } catch (Exception e) {
//            logger.warn("Could not load existing relationships: {}", e.getMessage());
//        }
//
//        return existing;
//    }
//
//    /**
//     * יצירת מפתח ייחודי לקשר לבדיקת כפילויות
//     */
//    private String createRelationshipKey(String cui1, String cui2, String relType) {
//        return cui1 + "|" + relType + "|" + cui2;
//    }
//
//    /**
//     * דיווח התקדמות
//     */
//    private void reportProgress(RelationshipImportTracker tracker) {
//        double acceptanceRate = (double) tracker.acceptedRelationships / tracker.totalLines * 100;
//        logger.info("Progress: {} lines processed, {} relationships accepted ({:.2f}% acceptance rate)",
//                tracker.totalLines, tracker.acceptedRelationships, acceptanceRate);
//    }
//
//    /**
//     * הצגת סיכום הייבוא
//     */
//    private void printImportSummary(RelationshipImportTracker tracker) {
//        logger.info("=== Relationship Import Summary ===");
//        logger.info("Total lines processed: {}", tracker.totalLines);
//        logger.info("Relationships accepted: {}", tracker.acceptedRelationships);
//        logger.info("Relationships created in Neo4j: {}", tracker.totalCreated);
//        logger.info("Batches processed: {}", tracker.batchCount);
//
//        logger.info("--- Skipped Relationships Breakdown ---");
//        logger.info("Self-loops prevented: {}", tracker.skippedSelfLoops);
//        logger.info("Non-demo relationships (Demo mode): {}", tracker.skippedNonDemo);
//        logger.info("Invalid relationship types: {}", tracker.skippedInvalidRelType);
//        logger.info("Missing nodes: {}", tracker.skippedMissingNodes);
//        logger.info("Duplicates prevented: {}", tracker.skippedDuplicates);
//        logger.info("Invalid format: {}", tracker.skippedInvalidFormat);
//
//        if (tracker.totalLines > 0) {
//            double acceptanceRate = (double) tracker.acceptedRelationships / tracker.totalLines * 100;
//            logger.info("Overall acceptance rate: {:.2f}%", acceptanceRate);
//        }
//
//        logger.info("=== Import Process Completed ===");
//    }
//
//    // =============== מחלקות עזר  ===============
//
//    /**
//     * מחלקה לעקיב אחר סטטיסטיקות הייבוא
//     */
//    private static class RelationshipImportTracker {
//        int totalLines = 0;
//        int acceptedRelationships = 0;
//        int totalCreated = 0;
//        int batchCount = 0;
//        int skippedSelfLoops = 0;
//        int skippedNonDemo = 0;
//        int skippedInvalidRelType = 0;
//        int skippedMissingNodes = 0;
//        int skippedDuplicates = 0;
//        int skippedInvalidFormat = 0;
//    }
//
//    /**
//     * מחלקה לייצוג מועמד קשר
//     */
//    private static class RelationshipCandidate {
//        final String cui1;
//        final String cui2;
//        final String relationshipType;
//        final double weight;
//        final String source;
//        final String originalRel;
//        final String originalRela;
//
//        RelationshipCandidate(String cui1, String cui2, String relationshipType, double weight,
//                              String source, String originalRel, String originalRela) {
//            this.cui1 = cui1;
//            this.cui2 = cui2;
//            this.relationshipType = relationshipType;
//            this.weight = weight;
//            this.source = source;
//            this.originalRel = originalRel;
//            this.originalRela = originalRela;
//        }
//
//        Map<String, Object> toMap() {
//            Map<String, Object> map = new HashMap<>();
//            map.put("cui1", cui1);
//            map.put("cui2", cui2);
//            map.put("relType", relationshipType);
//            map.put("weight", weight);
//            map.put("source", source);
//            map.put("originalRel", originalRel);
//            map.put("originalRela", originalRela);
//            return map;
//        }
//
//        String getRelationshipKey() {
//            return cui1 + "|" + relationshipType + "|" + cui2;
//        }
//    }
//
//}

package com.example.mediaid.neo4j;

import com.example.mediaid.bl.DemoMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static com.example.mediaid.constants.DatabaseConstants.*;
import static com.example.mediaid.constants.MedicalAnalysisConstants.*;

/**
 * שירות לייבוא קשרים רפואיים ישירות מקובץ MRREL
 */
@Service
public class UmlsRelationshipImporter extends UmlsImporter {

    private static final Logger logger = LoggerFactory.getLogger(UmlsRelationshipImporter.class);

    // סטים של CUI עבור כל סוג ישות
    private Set<String> diseaseCuis = new HashSet<>();
    private Set<String> medicationCuis = new HashSet<>();
    private Set<String> symptomCuis = new HashSet<>();
    private Set<String> riskFactorCuis = new HashSet<>();
    private Set<String> procedureCuis = new HashSet<>();
    private Set<String> anatomicalCuis = new HashSet<>();
    private Set<String> labTestCuis = new HashSet<>();
    private Set<String> biologicalFunctionCuis = new HashSet<>();

    @Autowired
    public UmlsRelationshipImporter(Driver driver) {
        super(driver);
    }

    /**
     * נקודת כניסה עיקרית לייבוא קשרים
     */
    public void importRelationships(String mrrelPath) {
        logger.info(ImportConstants.Messages.IMPORT_STARTED);
        logger.info("MRREL file path: {}", mrrelPath);

        try {
            // שלב הכנה
            loadAllCuisFromGraph();
            ensureIndexesExist();

            // שלב ייבוא
            importRelationshipsFromMrrel(mrrelPath);

            logger.info(ImportConstants.Messages.IMPORT_COMPLETED);

        } catch (Exception e) {
            logger.error("Critical error in relationship import: {}", e.getMessage(), e);
            throw new RuntimeException("Relationship import failed", e);
        }
    }

    /**
     * עיבוד קובץ MRREL וייבוא הקשרים
     */
    private void importRelationshipsFromMrrel(String mrrelPath) throws IOException {
        logger.info("Processing MRREL file for relationship extraction");

        // הגדרת משתני מעקב
        RelationshipImportTracker tracker = new RelationshipImportTracker();
        List<Map<String, Object>> relationshipBatch = new ArrayList<>();
        Set<String> existingRelationships = loadExistingRelationships();

        logger.info("Loaded {} existing relationships to prevent duplicates", existingRelationships.size());

        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))) {
            String line;
            logger.info("Starting relationship processing");

            while ((line = reader.readLine()) != null) {
                tracker.totalLines++;

                // דיווח התקדמות
                if (tracker.totalLines % PROGRESS_REPORT_INTERVAL == 0) {
                    reportProgress(tracker);
                }

                // עיבוד השורה
                RelationshipCandidate candidate = processLine(line, tracker, existingRelationships);

                if (candidate != null) {
                    relationshipBatch.add(candidate.toMap());
                    tracker.acceptedRelationships++;
                    existingRelationships.add(candidate.getRelationshipKey());

                    // יצירת אצווה כשמגיעים לגודל המטרה
                    if (relationshipBatch.size() >= RELATIONSHIP_BATCH_SIZE) {
                        int created = createRelationshipsBatch(relationshipBatch);
                        tracker.totalCreated += created;
                        relationshipBatch.clear();

                        if (tracker.batchCount % BATCH_REPORT_INTERVAL == 0) {
                            logger.info("Processed {} batches, created {} relationships total",
                                    tracker.batchCount, tracker.totalCreated);
                        }
                        tracker.batchCount++;
                    }
                }
            }

            // עיבוד אצווה אחרונה
            if (!relationshipBatch.isEmpty()) {
                int created = createRelationshipsBatch(relationshipBatch);
                tracker.totalCreated += created;
                tracker.batchCount++;
            }

            // הצגת סיכום מפורט
            printImportSummary(tracker);

        } catch (IOException e) {
            logger.error(ImportConstants.Messages.ERROR_FILE_READ + ": {}", e.getMessage());
            throw e;
        }
    }

    /**
     * עיבוד שורה בודדת מקובץ MRREL
     */
    private RelationshipCandidate processLine(String line, RelationshipImportTracker tracker,
                                              Set<String> existingRelationships) {

        String[] fields = line.split("\\|");
        if (fields.length < MIN_MRREL_FIELDS) {
            tracker.skippedInvalidFormat++;
            return null;
        }

        String cui1 = fields[0];
        String cui2 = fields[4];
        String rel = fields[3];
        String rela = fields[7];
        String sab = fields[10];

        // בדיקות סינון

        // 1. מניעת לולאות עצמיות
        if (cui1.equals(cui2)) {
            tracker.skippedSelfLoops++;
            return null;
        }

        // 2. בדיקת מצב Demo
        if (DemoMode.MODE && !DemoMode.isRelationshipRelevantForDemo(cui1, cui2)) {
            tracker.skippedNonDemo++;
            return null;
        }

        // 3. קביעת סוג הקשר
        String relationshipType = determineRelationshipType(rel, rela);
        if (relationshipType == null ||
                ImportConstants.EXCLUDED_RELATIONSHIP_TYPES.contains(relationshipType.toLowerCase())) {
            tracker.skippedInvalidRelType++;
            return null;
        }

        // 4. בדיקת קיום צמתים
        if (!nodeCuiExists(cui1) || !nodeCuiExists(cui2)) {
            tracker.skippedMissingNodes++;
            return null;
        }

        // 5. בדיקת כפילויות
        String relationshipKey = createRelationshipKey(cui1, cui2, relationshipType);
        if (existingRelationships.contains(relationshipKey)) {
            tracker.skippedDuplicates++;
            return null;
        }

        // יצירת מועמד קשר
        double weight = RelationshipTypes.calculateRelationshipWeight(
                rela != null && !rela.trim().isEmpty() ? rela : rel, sab);

        return new RelationshipCandidate(cui1, cui2, relationshipType, weight, sab, rel, rela);
    }

    /**
     * קביעת סוג הקשר מ-REL ו-RELA
     */
    private String determineRelationshipType(String rel, String rela) {
        // בדיקת RELA תחילה (ספציפי יותר)
        if (rela != null && !rela.trim().isEmpty()) {
            String normalized = rela.trim().toLowerCase();
            if (ImportConstants.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(normalized)) {
                return ImportConstants.UMLS_TO_NEO4J_RELATIONSHIPS.get(normalized);
            }
            return normalizeRelationshipName(normalized);
        }

        // אם אין RELA, השתמש ב- REL
        if (rel != null && !rel.trim().isEmpty()) {
            String normalized = rel.trim().toUpperCase();
            if (ImportConstants.REL_TO_RELATIONSHIP.containsKey(normalized)) {
                return ImportConstants.REL_TO_RELATIONSHIP.get(normalized);
            }
            return normalizeRelationshipName(rel.trim().toLowerCase());
        }

        return null;
    }

    /**
     * נרמול שם קשר לפורמט תקין של Neo4j
     */
    private String normalizeRelationshipName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_RELATIONSHIP_TYPE;
        }

        String normalized = name.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("[^A-Z0-9_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isEmpty() || !Character.isLetter(normalized.charAt(0))) {
            return DEFAULT_RELATIONSHIP_TYPE;
        }

        if (normalized.length() > MAX_RELATIONSHIP_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_RELATIONSHIP_NAME_LENGTH);
        }

        return normalized;
    }

    /**
     * יצירת אצווה של קשרים ב-Neo4j
     */
    private int createRelationshipsBatch(List<Map<String, Object>> relationships) {
        if (relationships.isEmpty()) return 0;

        logger.debug(ImportConstants.Messages.CREATING_BATCH + " of {} relationships", relationships.size());

        int successCount = 0;

        for (int i = 0; i < relationships.size(); i += SMALL_BATCH_SIZE) {
            int endIndex = Math.min(i + SMALL_BATCH_SIZE, relationships.size());
            List<Map<String, Object>> smallBatch = relationships.subList(i, endIndex);

            successCount += createSmallBatch(smallBatch);

            // הפסקה קטנה בין אצוות
            if (i > 0 && i % 500 == 0) {
                try {
                    Thread.sleep(BATCH_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.debug(ImportConstants.Messages.BATCH_COMPLETED + " - {} relationships created", successCount);
        return successCount;
    }

    /**
     * יצירת אצווה קטנה של קשרים
     */
    private int createSmallBatch(List<Map<String, Object>> relationships) {
        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                int count = 0;

                for (Map<String, Object> rel : relationships) {
                    try {
                        String cui1 = (String) rel.get("cui1");
                        String cui2 = (String) rel.get("cui2");
                        String relType = (String) rel.get("relType");
                        double weight = (double) rel.get("weight");
                        String source = (String) rel.get("source");

                        String query = "MATCH (n1 {cui: $cui1}), (n2 {cui: $cui2}) " +
                                "CREATE (n1)-[:" + relType + " {weight: $weight, source: $source}]->(n2)";

                        tx.run(query, Map.of(
                                "cui1", cui1,
                                "cui2", cui2,
                                "weight", weight,
                                "source", source
                        ));

                        count++;

                    } catch (Exception e) {
                        logger.debug("Failed to create relationship: {}", e.getMessage());
                    }
                }

                return count;
            });

        } catch (Exception e) {
            logger.warn("Small batch creation failed: {}", e.getMessage());
            return createRelationshipsIndividually(relationships);
        }
    }

    /**
     * יצירת קשרים אחד אחד במקרה של כשל באצווה
     */
    private int createRelationshipsIndividually(List<Map<String, Object>> relationships) {
        int successCount = 0;

        for (Map<String, Object> rel : relationships) {
            int retryCount = 0;
            boolean success = false;

            while (retryCount < MAX_RETRIES && !success) {
                try (Session session = neo4jDriver.session()) {
                    session.writeTransaction(tx -> {
                        String cui1 = (String) rel.get("cui1");
                        String cui2 = (String) rel.get("cui2");
                        String relType = (String) rel.get("relType");
                        double weight = (double) rel.get("weight");
                        String source = (String) rel.get("source");

                        String query = "MATCH (n1 {cui: $cui1}), (n2 {cui: $cui2}) " +
                                "CREATE (n1)-[:" + relType + " {weight: $weight, source: $source}]->(n2)";

                        tx.run(query, Map.of(
                                "cui1", cui1,
                                "cui2", cui2,
                                "weight", weight,
                                "source", source
                        ));

                        return null;
                    });

                    success = true;
                    successCount++;

                } catch (Exception e) {
                    retryCount++;
                    if (retryCount < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        logger.debug("Failed individual relationship creation after {} retries: {}",
                                MAX_RETRIES, e.getMessage());
                    }
                }
            }
        }

        return successCount;
    }

    /**
     * טעינת כל ה-CUI מהגרף
     */
    private void loadAllCuisFromGraph() {
        logger.info(ImportConstants.Messages.LOADING_NODES);

        try (Session session = neo4jDriver.session()) {
            for (String entityType : INDEXED_ENTITY_TYPES) {
                Set<String> cuis = loadCuisForType(session, entityType);
                assignCuisToSet(entityType, cuis);
                logger.debug("Loaded {} CUIs for entity type: {}", cuis.size(), entityType);
            }

            int totalCuis = diseaseCuis.size() + medicationCuis.size() + symptomCuis.size() +
                    riskFactorCuis.size() + procedureCuis.size() + anatomicalCuis.size() +
                    labTestCuis.size() + biologicalFunctionCuis.size();

            logger.info("Loaded total {} CUIs from graph", totalCuis);
        }
    }

    /**
     * הקצאת CUI לסט המתאים
     */
    private void assignCuisToSet(String entityType, Set<String> cuis) {
        switch (entityType) {
            case EntityTypes.DISEASE -> diseaseCuis = cuis;
            case EntityTypes.MEDICATION -> medicationCuis = cuis;
            case EntityTypes.SYMPTOM -> symptomCuis = cuis;
            case EntityTypes.RISK_FACTOR -> riskFactorCuis = cuis;
            case EntityTypes.PROCEDURE -> procedureCuis = cuis;
            case EntityTypes.ANATOMICAL_STRUCTURE -> anatomicalCuis = cuis;
            case EntityTypes.LABORATORY_TEST -> labTestCuis = cuis;
            case EntityTypes.BIOLOGICAL_FUNCTION -> biologicalFunctionCuis = cuis;
        }
    }

    /**
     * טעינת CUI עבור סוג ישות מסוים
     */
    private Set<String> loadCuisForType(Session session, String entityType) {
        return new HashSet<>(
                session.readTransaction(tx -> {
                    var result = tx.run("MATCH (n:" + entityType + ") RETURN n.cui AS cui");
                    List<String> cuis = new ArrayList<>();
                    result.forEachRemaining(record -> {
                        if (record.get("cui") != null) {
                            cuis.add(record.get("cui").asString());
                        }
                    });
                    return cuis;
                })
        );
    }

    /**
     * בדיקת קיום CUI בגרף
     */
    private boolean nodeCuiExists(String cui) {
        return diseaseCuis.contains(cui) ||
                medicationCuis.contains(cui) ||
                symptomCuis.contains(cui) ||
                riskFactorCuis.contains(cui) ||
                procedureCuis.contains(cui) ||
                anatomicalCuis.contains(cui) ||
                labTestCuis.contains(cui) ||
                biologicalFunctionCuis.contains(cui);
    }

    /**
     * יצירת אינדקסים לביצועים
     */
    private void ensureIndexesExist() {
        logger.info("Creating performance indexes");

        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                for (String entityType : INDEXED_ENTITY_TYPES) {
                    try {
                        String indexQuery = "CREATE INDEX IF NOT EXISTS FOR (n:" + entityType + ") ON (n.cui)";
                        tx.run(indexQuery);
                    } catch (Exception e) {
                        logger.debug("Index for {} might already exist", entityType);
                    }
                }
                return null;
            });

            logger.info("Performance indexes created successfully");

        } catch (Exception e) {
            logger.warn("Could not create indexes: {}", e.getMessage());
        }
    }

    /**
     * טעינת קשרים קיימים למניעת כפילויות
     */
    private Set<String> loadExistingRelationships() {
        logger.debug("Loading existing relationships to prevent duplicates");
        Set<String> existing = new HashSet<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(
                        "MATCH (n1)-[r]->(n2) " +
                                "WHERE n1.cui IS NOT NULL AND n2.cui IS NOT NULL " +
                                "RETURN n1.cui as cui1, n2.cui as cui2, type(r) as relType " +
                                "LIMIT " + MAX_CONNECTIONS_PER_ENTITY * 1000
                );

                //יצירת מחרוזת שמבטאת את הקשר
                result.forEachRemaining(record -> {
                    String cui1 = record.get("cui1").asString();
                    String cui2 = record.get("cui2").asString();
                    String relType = record.get("relType").asString();
                    existing.add(createRelationshipKey(cui1, cui2, relType));
                });

                return null;
            });
        } catch (Exception e) {
            logger.warn("Could not load existing relationships: {}", e.getMessage());
        }

        return existing;
    }

    /**
     * יצירת מפתח ייחודי לקשר לבדיקת כפילויות
     */
    private String createRelationshipKey(String cui1, String cui2, String relType) {
        return cui1 + "|" + relType + "|" + cui2;
    }

    /**
     * דיווח התקדמות
     */
    private void reportProgress(RelationshipImportTracker tracker) {
        double acceptanceRate = (double) tracker.acceptedRelationships / tracker.totalLines * 100;
        logger.info("Progress: {} lines processed, {} relationships accepted ({:.2f}% acceptance rate)",
                tracker.totalLines, tracker.acceptedRelationships, acceptanceRate);
    }

    /**
     * הצגת סיכום הייבוא
     */
    private void printImportSummary(RelationshipImportTracker tracker) {
        logger.info("=== Relationship Import Summary ===");
        logger.info("Total lines processed: {}", tracker.totalLines);
        logger.info("Relationships accepted: {}", tracker.acceptedRelationships);
        logger.info("Relationships created in Neo4j: {}", tracker.totalCreated);
        logger.info("Batches processed: {}", tracker.batchCount);

        logger.info("--- Skipped Relationships Breakdown ---");
        logger.info("Self-loops prevented: {}", tracker.skippedSelfLoops);
        logger.info("Non-demo relationships (Demo mode): {}", tracker.skippedNonDemo);
        logger.info("Invalid relationship types: {}", tracker.skippedInvalidRelType);
        logger.info("Missing nodes: {}", tracker.skippedMissingNodes);
        logger.info("Duplicates prevented: {}", tracker.skippedDuplicates);
        logger.info("Invalid format: {}", tracker.skippedInvalidFormat);

        if (tracker.totalLines > 0) {
            double acceptanceRate = (double) tracker.acceptedRelationships / tracker.totalLines * 100;
            logger.info("Overall acceptance rate: {:.2f}%", acceptanceRate);
        }

        logger.info("=== Import Process Completed ===");
    }

    // =============== מחלקות עזר  ===============

    /**
     * מחלקה לעקיב אחר סטטיסטיקות הייבוא
     */
    private static class RelationshipImportTracker {
        int totalLines = 0;
        int acceptedRelationships = 0;
        int totalCreated = 0;
        int batchCount = 0;
        int skippedSelfLoops = 0;
        int skippedNonDemo = 0;
        int skippedInvalidRelType = 0;
        int skippedMissingNodes = 0;
        int skippedDuplicates = 0;
        int skippedInvalidFormat = 0;
    }

    /**
     * מחלקה לייצוג מועמד קשר
     */
    private static class RelationshipCandidate {
        final String cui1;
        final String cui2;
        final String relationshipType;
        final double weight;
        final String source;
        final String originalRel;
        final String originalRela;

        RelationshipCandidate(String cui1, String cui2, String relationshipType, double weight,
                              String source, String originalRel, String originalRela) {
            this.cui1 = cui1;
            this.cui2 = cui2;
            this.relationshipType = relationshipType;
            this.weight = weight;
            this.source = source;
            this.originalRel = originalRel;
            this.originalRela = originalRela;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("cui1", cui1);
            map.put("cui2", cui2);
            map.put("relType", relationshipType);
            map.put("weight", weight);
            map.put("source", source);
            map.put("originalRel", originalRel);
            map.put("originalRela", originalRela);
            return map;
        }

        String getRelationshipKey() {
            return cui1 + "|" + relationshipType + "|" + cui2;
        }
    }
}