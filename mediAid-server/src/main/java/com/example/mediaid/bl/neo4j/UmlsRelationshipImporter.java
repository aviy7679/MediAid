package com.example.mediaid.bl.neo4j;

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

@Service
public class UmlsRelationshipImporter extends UmlsImporter{

    private static final Logger logger = LoggerFactory.getLogger(UmlsRelationshipImporter.class);
    private static final int BATCH_SIZE = 10000;

    // סטים של מזהי CUI של כל סוגי הצמתים - יטענו בתחילת התהליך
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

    public void importRelationships(String mrrelPath) {
        loadAllCuisFromGraph();

        try {
            importRelationshipsFromMrrel(mrrelPath);
        } catch (Exception e) {
            logger.error("Error importing relationships: {}", e.getMessage(), e);
        }

    }

    /**
     * טוען את כל מזהי ה-CUI מהגרף הקיים, כדי שנוכל לסנן קשרים שאין להם התאמה בצמתים שלנו
     */
    private void loadAllCuisFromGraph() {
        try (Session session = neo4jDriver.session()) {
            // Load disease CUIs
            diseaseCuis = loadCuisForType(session, EntityTypes.DISEASE);
            logger.info("Loaded {} disease CUI identifiers", diseaseCuis.size());

            // Load medication CUIs
            medicationCuis = loadCuisForType(session, EntityTypes.MEDICATION);
            logger.info("Loaded {} medication CUI identifiers", medicationCuis.size());

            // Load symptom CUIs
            symptomCuis = loadCuisForType(session, EntityTypes.SYMPTOM);
            logger.info("Loaded {} symptom CUI identifiers", symptomCuis.size());

            // Load risk factor CUIs
            riskFactorCuis = loadCuisForType(session, EntityTypes.RISK_FACTOR);
            logger.info("Loaded {} risk factor CUI identifiers", riskFactorCuis.size());

            // Load procedure CUIs
            procedureCuis = loadCuisForType(session, EntityTypes.PROCEDURE);
            logger.info("Loaded {} procedure CUI identifiers", procedureCuis.size());

            // Load anatomical structure CUIs
            anatomicalCuis = loadCuisForType(session, EntityTypes.ANATOMICAL_STRUCTURE);
            logger.info("Loaded {} anatomical structure CUI identifiers", anatomicalCuis.size());

            // Load laboratory test CUIs
            labTestCuis = loadCuisForType(session, EntityTypes.LABORATORY_TEST);
            logger.info("Loaded {} laboratory test CUI identifiers", labTestCuis.size());

            // Load biological function CUIs
            biologicalFunctionCuis = loadCuisForType(session, EntityTypes.BIOLOGICAL_FUNCTION);
            logger.info("Loaded {} biological function CUI identifiers", biologicalFunctionCuis.size());
        }
    }
    /**
     * בדיקת תקינות הקשרים שנוצרו וחזרת סטטיסטיקות
     */
    public Map<String, Object> validateImportedRelationships() {
        logger.info("Validating imported relationships...");

        Map<String, Object> validation = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                // ספירת קשרים לפי סוג
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
                                validation.put(relType.toLowerCase() + "_count", count);
                                logger.debug("Found {} relationships of type {}", count, relType);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error counting relationships of type {}: {}", relType, e.getMessage());
                    }
                }

                // ספירת סה"כ קשרים
                try {
                    var totalResult = tx.run("MATCH ()-[r]->() RETURN COUNT(r) as total");
                    if (totalResult.hasNext()) {
                        long total = totalResult.next().get("total").asLong();
                        validation.put("total_relationships", total);
                        logger.info("Total relationships in graph: {}", total);
                    }
                } catch (Exception e) {
                    logger.warn("Error counting total relationships: {}", e.getMessage());
                }

                // ספירת קשרים לפי מקור נתונים
                try {
                    var sourceResult = tx.run(
                            "MATCH ()-[r]->() " +
                                    "WHERE r.source IS NOT NULL " +
                                    "RETURN r.source as source, COUNT(r) as count " +
                                    "ORDER BY count DESC"
                    );

                    Map<String, Object> sourceStats = new HashMap<>();
                    sourceResult.forEachRemaining(record -> {
                        String source = record.get("source").asString();
                        long count = record.get("count").asLong();
                        sourceStats.put(source, count);
                    });

                    if (!sourceStats.isEmpty()) {
                        validation.put("relationships_by_source", sourceStats);
                    }
                } catch (Exception e) {
                    logger.warn("Error counting relationships by source: {}", e.getMessage());
                }

                // בדיקת איכות הקשרים
                try {
                    var qualityResult = tx.run(
                            "MATCH ()-[r]->() " +
                                    "WHERE r.weight IS NOT NULL " +
                                    "RETURN AVG(r.weight) as avg_weight, " +
                                    "       MIN(r.weight) as min_weight, " +
                                    "       MAX(r.weight) as max_weight, " +
                                    "       COUNT(r) as count_with_weight"
                    );

                    if (qualityResult.hasNext()) {
                        var record = qualityResult.next();
                        Map<String, Object> qualityStats = new HashMap<>();
                        qualityStats.put("average_weight", record.get("avg_weight").asDouble());
                        qualityStats.put("min_weight", record.get("min_weight").asDouble());
                        qualityStats.put("max_weight", record.get("max_weight").asDouble());
                        qualityStats.put("relationships_with_weight", record.get("count_with_weight").asLong());
                        validation.put("quality_stats", qualityStats);
                    }
                } catch (Exception e) {
                    logger.warn("Error calculating quality statistics: {}", e.getMessage());
                }

                return null;
            });

            // הוספת מידע על זמן הבדיקה
            validation.put("validation_timestamp", System.currentTimeMillis());
            validation.put("validation_status", "completed");

            logger.info("Relationship validation completed. Found {} different relationship types",
                    validation.entrySet().stream()
                            .filter(entry -> entry.getKey().endsWith("_count"))
                            .count());

        } catch (Exception e) {
            logger.error("Error during relationship validation: {}", e.getMessage(), e);
            validation.put("validation_status", "error");
            validation.put("error_message", e.getMessage());
        }

        return validation;
    }

    /**
     * בדיקת קשרים ספציפיים בין שני CUIs
     */
    public List<Map<String, Object>> findRelationshipsBetweenCuis(String cui1, String cui2) {
        List<Map<String, Object>> relationships = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(
                        "MATCH (n1)-[r]->(n2) " +
                                "WHERE n1.cui = $cui1 AND n2.cui = $cui2 " +
                                "RETURN type(r) as relationship_type, " +
                                "       r.weight as weight, " +
                                "       r.source as source, " +
                                "       labels(n1)[0] as from_type, " +
                                "       labels(n2)[0] as to_type",
                        Map.of("cui1", cui1, "cui2", cui2)
                );

                result.forEachRemaining(record -> {
                    Map<String, Object> rel = new HashMap<>();
                    rel.put("relationship_type", record.get("relationship_type").asString());
                    rel.put("weight", record.get("weight").asDouble(0.0));
                    rel.put("source", record.get("source").asString(""));
                    rel.put("from_type", record.get("from_type").asString());
                    rel.put("to_type", record.get("to_type").asString());
                    relationships.add(rel);
                });

                return null;
            });
        } catch (Exception e) {
            logger.error("Error finding relationships between {} and {}: {}", cui1, cui2, e.getMessage());
        }

        return relationships;
    }

    /**
     * בדיקת התפלגות משקלים בקשרים
     */
    public Map<String, Object> analyzeRelationshipWeights() {
        Map<String, Object> analysis = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                // התפלגות משקלים
                var result = tx.run(
                        "MATCH ()-[r]->() " +
                                "WHERE r.weight IS NOT NULL " +
                                "WITH r.weight as weight " +
                                "RETURN " +
                                "  COUNT(CASE WHEN weight >= 0.9 THEN 1 END) as high_confidence, " +
                                "  COUNT(CASE WHEN weight >= 0.7 AND weight < 0.9 THEN 1 END) as medium_confidence, " +
                                "  COUNT(CASE WHEN weight >= 0.5 AND weight < 0.7 THEN 1 END) as low_confidence, " +
                                "  COUNT(CASE WHEN weight < 0.5 THEN 1 END) as very_low_confidence, " +
                                "  COUNT(*) as total"
                );

                if (result.hasNext()) {
                    var record = result.next();
                    analysis.put("high_confidence_relationships", record.get("high_confidence").asLong());
                    analysis.put("medium_confidence_relationships", record.get("medium_confidence").asLong());
                    analysis.put("low_confidence_relationships", record.get("low_confidence").asLong());
                    analysis.put("very_low_confidence_relationships", record.get("very_low_confidence").asLong());
                    analysis.put("total_relationships_with_weight", record.get("total").asLong());
                }

                return null;
            });
        } catch (Exception e) {
            logger.error("Error analyzing relationship weights: {}", e.getMessage());
        }

        return analysis;
    }

    /**
     * מציאת הקשרים החזקים ביותר במערכת
     */
    public List<Map<String, Object>> findStrongestRelationships(int limit) {
        List<Map<String, Object>> strongestRels = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(
                        "MATCH (n1)-[r]->(n2) " +
                                "WHERE r.weight IS NOT NULL " +
                                "RETURN n1.name as from_name, n1.cui as from_cui, " +
                                "       n2.name as to_name, n2.cui as to_cui, " +
                                "       type(r) as relationship_type, " +
                                "       r.weight as weight, " +
                                "       r.source as source " +
                                "ORDER BY r.weight DESC " +
                                "LIMIT $limit",
                        Map.of("limit", limit)
                );

                result.forEachRemaining(record -> {
                    Map<String, Object> rel = new HashMap<>();
                    rel.put("from_name", record.get("from_name").asString());
                    rel.put("from_cui", record.get("from_cui").asString());
                    rel.put("to_name", record.get("to_name").asString());
                    rel.put("to_cui", record.get("to_cui").asString());
                    rel.put("relationship_type", record.get("relationship_type").asString());
                    rel.put("weight", record.get("weight").asDouble());
                    rel.put("source", record.get("source").asString());
                    strongestRels.add(rel);
                });

                return null;
            });
        } catch (Exception e) {
            logger.error("Error finding strongest relationships: {}", e.getMessage());
        }

        return strongestRels;
    }
    /**
     * טוען את כל מזהי ה-CUI עבור סוג ישות מסוים
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
     * בדיקה אם ה-CUI קיים באחד הסטים שלנו
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
     * מבצע ייבוא מדויק מקובץ MRREL.RRF - עם סינון קפדני של קשרים רלוונטיים בלבד
     */
    private void importRelationshipsFromMrrel(String mrrelPath) throws IOException {
        logger.info("Starting precise relationship import from {}", mrrelPath);

        // הקשרים שנמצאו ואושרו לייבוא
        List<Map<String, Object>> validRelationships = new ArrayList<>();

        // מונים לסטטיסטיקה
        int totalLines = 0;
        int skippedSourceNotPreferred = 0;
        int skippedRelationshipNotTargeted = 0;
        int skippedMissingNodes = 0;
        int skippedInvalidNodeTypes = 0;
        int acceptedRelationships = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                totalLines++;

                if (totalLines % 1000000 == 0) {
                    logger.debug("Read {} lines...", totalLines);
                }

                // פיצול השורה מ-MRREL.RRF (מופרד ע"י |)
                String[] fields = line.split("\\|");

                // וידוא שיש מספיק שדות
                if (fields.length < 16) continue;

                String cui1 = fields[0];  // CUI ראשון
                String cui2 = fields[4];  // CUI שני
                String rel = fields[3];   // סוג יחס כללי
                String rela = fields[7];  // סוג יחס ספציפי
                String sab = fields[10];  // מקור הנתונים

                // 1. סינון לפי מקור מועדף - דילוג על מקורות לא אמינים
                if (!EntityTypes.PREFERRED_SOURCES.contains(sab)) {
                    skippedSourceNotPreferred++;
                    continue;
                }

                // 2. סינון לפי סוג היחס - בדיקה אם זה יחס שמעניין אותנו
                if (rela == null || rela.isEmpty() ||
                        !RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(rela.toLowerCase())) {
                    skippedRelationshipNotTargeted++;
                    continue;
                }

                // 3. סינון לפי קיום הצמתים - האם יש לנו צמתים עם ה-CUI הזה
                if (!nodeCuiExists(cui1) || !nodeCuiExists(cui2)) {
                    skippedMissingNodes++;
                    continue;
                }

                // 4. סינון לפי טיפוסי הצמתים - בדיקה אם הקשר הזה מתאים לסוגי הצמתים
                String neoRelType = RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.get(rela.toLowerCase());
                if (!RelationshipTypes.isValidRelationshipForNodeTypes(
                        diseaseCuis, medicationCuis, symptomCuis,
                        riskFactorCuis, procedureCuis, anatomicalCuis,
                        labTestCuis, biologicalFunctionCuis,
                        cui1, cui2, neoRelType)) {
                    skippedInvalidNodeTypes++;
                    continue;
                }

                // הקשר עבר את כל הבדיקות - הוא תקין!
                Map<String, Object> relationship = new HashMap<>();
                relationship.put("cui1", cui1);
                relationship.put("cui2", cui2);
                relationship.put("relType", neoRelType);
                relationship.put("weight", RelationshipTypes.calculateRelationshipWeight(rela, sab));
                relationship.put("source", sab);
                validRelationships.add(relationship);
                acceptedRelationships++;

                // בדיקה אם הגענו לגודל האצווה, ואם כן - שמירה לגרף
                if (validRelationships.size() >= BATCH_SIZE) {
                    batchCreateRelationships(validRelationships);
                    logger.info("Created {} new relationships in graph", validRelationships.size());
                    validRelationships.clear();
                }
            }

            // טיפול באצווה האחרונה אם נשארו קשרים
            if (!validRelationships.isEmpty()) {
                batchCreateRelationships(validRelationships);
                logger.info("Created {} new relationships in graph (final batch)", validRelationships.size());
            }

            // הדפסת סיכום
            logger.info("==== Relationship Import Summary ====");
            logger.info("Total lines read: {}", totalLines);
            logger.info("Skipped due to non-preferred source: {}", skippedSourceNotPreferred);
            logger.info("Skipped due to unwanted relationship type: {}", skippedRelationshipNotTargeted);
            logger.info("Skipped due to missing nodes: {}", skippedMissingNodes);
            logger.info("Skipped due to invalid node types mismatch: {}", skippedInvalidNodeTypes);
            logger.info("Total relationships accepted: {}", acceptedRelationships);
        }
    }

    /**
     * יצירת קשרים באצווה בגרף Neo4j
     */
    private void batchCreateRelationships(List<Map<String, Object>> relationships) {
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                for (Map<String, Object> rel : relationships) {
                    String cui1 = (String) rel.get("cui1");
                    String cui2 = (String) rel.get("cui2");
                    String relType = (String) rel.get("relType");
                    double weight = (double) rel.get("weight");
                    String source = (String) rel.get("source");

                    // שאילתה ליצירת קשר
                    String query =
                            "MATCH (n1), (n2) " +
                                    "WHERE n1.cui = $cui1 AND n2.cui = $cui2 " +
                                    "MERGE (n1)-[r:" + relType + " {weight: $weight, source: $source}]->(n2)";

                    // ניסיון ליצירת הקשר
                    try {
                        tx.run(query, Map.of(
                                "cui1", cui1,
                                "cui2", cui2,
                                "weight", weight,
                                "source", source
                        ));
                    } catch (Exception e) {
                        logger.error("שגיאה ביצירת קשר: {}", e.getMessage(), e);
                    }
                }
                return null;
            });
        }
    }
}