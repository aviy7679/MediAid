package com.example.mediaid.bl.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UmlsRelationshipImporter extends UmlsImporter{

    private static final Logger logger = LoggerFactory.getLogger(UmlsRelationshipImporter.class);
    private static final int BATCH_SIZE = 1000; // הקטנו מ-10,000 ל-1,000
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

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
     * ניתוח קובץ MRREL לפני הייבוא - לראות מה יש בפועל
     */
    private void analyzeFileBeforeImport(String mrrelPath) {
        logger.info("🔍 Analyzing MRREL file structure...");

        Map<String, Integer> sourceCount = new HashMap<>();
        Map<String, Integer> relCount = new HashMap<>();
        Map<String, Integer> relaCount = new HashMap<>();
        List<String> sampleLines = new ArrayList<>();

        int totalAnalyzed = 0;
        int maxToAnalyze = 100000;

        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))) {
            String line;
            while ((line = reader.readLine()) != null && totalAnalyzed < maxToAnalyze) {
                String[] parts = line.split("\\|");
                if (parts.length >= 16) {
                    String cui1 = parts[0];
                    String cui2 = parts[4];
                    String rel = parts[3];
                    String rela = parts[7];
                    String sab = parts[10];

                    // ספירת מקורות
                    sourceCount.put(sab, sourceCount.getOrDefault(sab, 0) + 1);

                    // ספירת סוגי קשרים
                    if (rel != null && !rel.trim().isEmpty()) {
                        relCount.put(rel, relCount.getOrDefault(rel, 0) + 1);
                    }
                    if (rela != null && !rela.trim().isEmpty()) {
                        relaCount.put(rela, relaCount.getOrDefault(rela, 0) + 1);
                    }

                    // דוגמאות לשורות (רק הראשונות)
                    if (sampleLines.size() < 20) {
                        sampleLines.add(String.format("'%s' -> '%s' [%s/%s] from %s",
                                cui1, cui2, rel, rela, sab));
                    }
                }
                totalAnalyzed++;
            }

            logger.info("=== MRREL FILE ANALYSIS (from {} lines) ===", totalAnalyzed);

            // טופ 10 מקורות
            logger.info("TOP 10 SOURCES:");
            sourceCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(10)
                    .forEach(entry -> logger.info("  {} : {} occurrences", entry.getKey(), entry.getValue()));

            // טופ 10 REL
            logger.info("TOP 10 REL types:");
            relCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(10)
                    .forEach(entry -> logger.info("  '{}' : {} occurrences", entry.getKey(), entry.getValue()));

            // טופ 10 RELA
            logger.info("TOP 10 RELA types:");
            relaCount.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(10)
                    .forEach(entry -> logger.info("  '{}' : {} occurrences", entry.getKey(), entry.getValue()));

            // דוגמאות לשורות
            logger.info("SAMPLE RELATIONSHIPS:");
            sampleLines.forEach(sample -> logger.info("  {}", sample));

            logger.info("=== END ANALYSIS ===");

        } catch (IOException e) {
            logger.error("Error analyzing file", e);
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
     * מבצע ייבוא מקובץ MRREL.RRF - עם טיפול משופר בטרנזקציות
     */
    private void importRelationshipsFromMrrel(String mrrelPath) throws IOException {
        logger.info("🚀 Starting OPTIMIZED relationship import from {}", mrrelPath);

        // ניתוח הקובץ תחילה
        analyzeFileBeforeImport(mrrelPath);

        logger.info("=== OPTIMIZED IMPORT CONFIG ===");
        logger.info("✅ FILTERING ENABLED");
        logger.info("✅ DUPLICATE PREVENTION ENABLED");
        logger.info("✅ SELF-LOOP PREVENTION ENABLED");
        logger.info("Batch size: {} relationships per transaction", BATCH_SIZE);
        logger.info("==============================");

        // הקשרים שנמצאו ואושרו לייבוא
        List<Map<String, Object>> validRelationships = new ArrayList<>();

        // מונים לסטטיסטיקה
        int totalLines = 0;
        int skippedSourceNotPreferred = 0;
        int skippedRelationshipNotTargeted = 0;
        int skippedMissingNodes = 0;
        int skippedInvalidNodeTypes = 0;
        int skippedSelfLoops = 0;
        int skippedDuplicates = 0;
        int acceptedRelationships = 0;
        int totalCreatedInDb = 0;

        // Cache לקשרים קיימים - לשיפור ביצועים
        Set<String> existingRelationships = loadExistingRelationships();
        logger.info("Loaded {} existing relationships to avoid duplicates", existingRelationships.size());

        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))) {
            String line;
            logger.info("🔍 File opened successfully, starting optimized processing...");

            while ((line = reader.readLine()) != null) {
                totalLines++;

                if (totalLines % 1000000 == 0) {
                    logger.info("📊 Processed {} million lines, accepted {} relationships so far",
                            totalLines / 1000000, acceptedRelationships);
                }

                // פיצול השורה מ-MRREL.RRF (מופרד ע"י |)
                String[] fields = line.split("\\|");
                if (fields.length < 15) continue;

                String cui1 = fields[0];
                String cui2 = fields[4];
                String rel = fields[3];
                String rela = fields[7];
                String sab = fields[10];

                // ===== סינונים מופעלים =====

                // 1. מניעת לולאות עצמיות - בדיקה ראשונה ומהירה
                if (cui1.equals(cui2)) {
                    skippedSelfLoops++;
                    continue;
                }

                // 2. סינון לפי מקור מועדף
                if (!EntityTypes.PREFERRED_SOURCES.contains(sab)) {
                    skippedSourceNotPreferred++;
                    continue;
                }

                // 3. סינון לפי סוג היחס
                // 3. סינון לפי סוג היחס - זמנית מושבת
                String relationshipType = determineRelationshipType(rel, rela);
// קבל הכל חוץ מקשרים ריקים
                if (relationshipType == null || relationshipType.trim().isEmpty() || relationshipType.equals("unknown")) {
                    skippedRelationshipNotTargeted++;
                    continue;
                }

                // 4. בדיקת קיום הצמתים
                if (!nodeCuiExists(cui1) || !nodeCuiExists(cui2)) {
                    skippedMissingNodes++;
                    continue;
                }

                // 5. בדיקת תאימות סוגי הצמתים
                String neoRelType = RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.get(relationshipType.toLowerCase());
                if (!RelationshipTypes.isValidRelationshipForNodeTypes(
                        diseaseCuis, medicationCuis, symptomCuis,
                        riskFactorCuis, procedureCuis, anatomicalCuis,
                        labTestCuis, biologicalFunctionCuis,
                        cui1, cui2, neoRelType)) {
                    skippedInvalidNodeTypes++;
                    continue;
                }

                // 6. בדיקת קשר כפול - מניעת יצירת קשרים קיימים
                String relationshipKey = createRelationshipKey(cui1, cui2, neoRelType);
                if (existingRelationships.contains(relationshipKey)) {
                    skippedDuplicates++;
                    continue;
                }
                // DEBUG - הדפס כמה דוגמאות של קשרים שנדחים
                if (skippedRelationshipNotTargeted <= 20) {
                    logger.info("🔍 REJECTED REL: '{}' / RELA: '{}' -> determined: '{}' (line {})",
                            rel, rela, relationshipType, totalLines);
                }

                // הקשר עבר את כל הבדיקות!
                double weight = RelationshipTypes.calculateRelationshipWeight(relationshipType, sab);
                Map<String, Object> relationship = new HashMap<>();
                relationship.put("cui1", cui1);
                relationship.put("cui2", cui2);
                relationship.put("relType", neoRelType);
                relationship.put("weight", weight);
                relationship.put("source", sab);
                validRelationships.add(relationship);
                acceptedRelationships++;

                // הוספה לcache כדי למנוע כפילויות
                existingRelationships.add(relationshipKey);

                // הדפסת קשרים מתקבלים (רק הראשונים)
                if (acceptedRelationships <= 20) {
                    logger.info("✅ ACCEPTED #{}: '{}' -[{}]-> '{}' (source: {}, weight: {:.2f})",
                            acceptedRelationships, cui1, neoRelType, cui2, sab, weight);
                } else if (acceptedRelationships % 5000 == 0) {
                    logger.info("📈 Processed {} relationships (last: '{}' -[{}]-> '{}')",
                            acceptedRelationships, cui1, neoRelType, cui2);
                }

                // יצירת קשרים ב-Neo4j כשמגיעים לגודל האצווה
                if (validRelationships.size() >= BATCH_SIZE) {
                    int createdCount = createOptimizedRelationshipsBatch(validRelationships);
                    totalCreatedInDb += createdCount;
                    logger.info("💾 Created {} relationships in Neo4j (batch {}, total: {})",
                            createdCount, (totalCreatedInDb / BATCH_SIZE), totalCreatedInDb);
                    validRelationships.clear();
                }
            }

            // טיפול באצווה האחרונה
            if (!validRelationships.isEmpty()) {
                int createdCount = createOptimizedRelationshipsBatch(validRelationships);
                totalCreatedInDb += createdCount;
                logger.info("💾 Created {} relationships in Neo4j (final batch, total: {})",
                        createdCount, totalCreatedInDb);
            }

            // הדפסת סיכום מפורט
            logger.info("====== OPTIMIZED IMPORT SUMMARY ======");
            logger.info("📊 Total lines read: {}", totalLines);
            logger.info("🚫 Skipped - Non-preferred source: {}", skippedSourceNotPreferred);
            logger.info("🚫 Skipped - Unsupported relationship: {}", skippedRelationshipNotTargeted);
            logger.info("🚫 Skipped - Missing nodes: {}", skippedMissingNodes);
            logger.info("🚫 Skipped - Invalid node types: {}", skippedInvalidNodeTypes);
            logger.info("🔄 Skipped - Self-loops prevented: {}", skippedSelfLoops);
            logger.info("📋 Skipped - Duplicates prevented: {}", skippedDuplicates);
            logger.info("✅ Total relationships accepted: {}", acceptedRelationships);
            logger.info("💾 Total relationships created in Neo4j: {}", totalCreatedInDb);

            // חישוב אחוזים
            if (totalLines > 0) {
                double rate = (double) acceptedRelationships / totalLines * 100;
                logger.info("📈 Acceptance rate: {:.4f}% ({} out of {} lines)",
                        rate, acceptedRelationships, totalLines);
            }

            logger.info("🎉 OPTIMIZED IMPORT COMPLETED SUCCESSFULLY!");

        } catch (IOException e) {
            logger.error("💥 ERROR reading file: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("💥 ERROR during import: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    /**
     * יצירת באצ' של קשרים עם retry logic ומניעת timeout
     */
    private int createRelationshipsBatch(List<Map<String, Object>> relationships) {
        if (relationships.isEmpty()) {
            return 0;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return executeRelationshipBatch(relationships);
            } catch (ServiceUnavailableException | TransientException e) {
                logger.warn("Database connection issue on attempt {} of {}: {}",
                        attempt, MAX_RETRIES, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    logger.error("Failed to create batch after {} attempts, skipping {} relationships",
                            MAX_RETRIES, relationships.size());
                    return 0;
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            } catch (Exception e) {
                logger.error("Unexpected error creating relationship batch: {}", e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    /**
     * מבצע את הכנסת הקשרים בפועל לNeo4j
     */
    private int executeRelationshipBatch(List<Map<String, Object>> relationships) {
        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                int successCount = 0;

                for (Map<String, Object> rel : relationships) {
                    String cui1 = (String) rel.get("cui1");
                    String cui2 = (String) rel.get("cui2");
                    String relType = (String) rel.get("relType");
                    double weight = (double) rel.get("weight");
                    String source = (String) rel.get("source");

                    try {
                        // שאילתה ליצירת קשר - פשוטה ויעילה
                        String query = "MATCH (n1), (n2) " +
                                "WHERE n1.cui = $cui1 AND n2.cui = $cui2 " +
                                "MERGE (n1)-[r:" + relType + "]->(n2) " +
                                "SET r.weight = $weight, r.source = $source " +
                                "RETURN COUNT(r) as created";

                        var result = tx.run(query, Map.of(
                                "cui1", cui1,
                                "cui2", cui2,
                                "weight", weight,
                                "source", source
                        ));

                        if (result.hasNext()) {
                            successCount++;
                        }

                    } catch (Exception e) {
                        logger.debug("Failed to create relationship '{}' -[{}]-> '{}': {}",
                                cui1, relType, cui2, e.getMessage());
                    }
                }

                return successCount;
            });
        }
    }

    // ===== פונקציות סינון - זמנית מושבתות =====

    private boolean isPreferredSource(String source) {
        // זמני - קבל הכל
        return true;

        // המקורי:
        // return EntityTypes.PREFERRED_SOURCES.contains(source);
    }

    private boolean isSupportedRelationship(String relationship) {
        // זמני - קבל כל קשר שאינו ריק
        return relationship != null && !relationship.trim().isEmpty();

        // המקורי:
        // return RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(relationship.toLowerCase());
    }

    private boolean areNodeTypesValid(String cui1, String cui2, String relationshipType) {
        // זמני - אל תבדוק תאימות סוגים
        return true;

        // המקורי:
        // return RelationshipTypes.isValidRelationshipForNodeTypes(...);
    }

    private String determineRelationshipType(String rel, String rela) {
        // תחילה נסה rela (זה יותר ספציפי)
        if (rela != null && !rela.trim().isEmpty()) {
            String normalized = rela.trim().toLowerCase();
            // בדיקה ישירה במיפוי
            if (RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(normalized)) {
                return normalized;
            }
            // אם לא נמצא, החזר את rela כמו שהוא (פחות סינון)
            return normalized;
        }

        // אם אין rela, השתמש ב-rel
        if (rel != null && !rel.trim().isEmpty()) {
            String normalized = rel.trim().toLowerCase();
            if (RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS.containsKey(normalized)) {
                return normalized;
            }
            // אם לא נמצא, החזר את rel כמו שהוא (פחות סינון)
            return normalized;
        }

        return "related_to"; // ברירת מחדל שמקבלת
    }    private String normalizeRelationshipType(String type) {
        // המרה פשוטה - נקה והחלף
        return type.toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("[^a-z0-9_]", "")
                .toUpperCase();
    }

    private double calculateRelationshipWeight(String relationshipType, String source) {
        // משקל בסיסי לפי מקור
        double baseWeight = 0.5;

        if ("MSH".equals(source) || "SNOMEDCT_US".equals(source)) {
            baseWeight = 0.9;
        } else if ("RXNORM".equals(source) || "NCI".equals(source)) {
            baseWeight = 0.8;
        } else if ("LNC".equals(source) || "ICD10".equals(source)) {
            baseWeight = 0.7;
        }

        return baseWeight;
    }

    // ===== פונקציות עזר =====

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

    // ===== פונקציות ולידציה =====

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
                                    "ORDER BY count DESC LIMIT 20"
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
     * טוען קשרים קיימים מהגרף למניעת כפילויות
     */
    private Set<String> loadExistingRelationships() {
        logger.info("Loading existing relationships to prevent duplicates...");
        Set<String> existing = new HashSet<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(
                        "MATCH (n1)-[r]->(n2) " +
                                "WHERE n1.cui IS NOT NULL AND n2.cui IS NOT NULL " +
                                "RETURN n1.cui as cui1, n2.cui as cui2, type(r) as relType " +
                                "LIMIT 1000000"  // מגביל ל-1 מליון כדי לא לטעון יותר מדי
                );

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
     * יוצר מפתח ייחודי לקשר
     */
    private String createRelationshipKey(String cui1, String cui2, String relType) {
        return cui1 + "|" + relType + "|" + cui2;
    }

    /**
     * יצירת באצ' מהיר ומותאם של קשרים
     */
    private int createOptimizedRelationshipsBatch(List<Map<String, Object>> relationships) {
        if (relationships.isEmpty()) {
            return 0;
        }

        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                int successCount = 0;

                for (Map<String, Object> rel : relationships) {
                    String cui1 = (String) rel.get("cui1");
                    String cui2 = (String) rel.get("cui2");
                    String relType = (String) rel.get("relType");
                    double weight = (double) rel.get("weight");
                    String source = (String) rel.get("source");

                    try {
                        // שאילתה מותאמת עם מניעת כפילויות
                        String query = "MATCH (n1), (n2) " +
                                "WHERE n1.cui = $cui1 AND n2.cui = $cui2 " +
                                "AND NOT EXISTS((n1)-[:" + relType + "]->(n2)) " +  // מניעת כפילויות
                                "CREATE (n1)-[r:" + relType + " {weight: $weight, source: $source}]->(n2) " +
                                "RETURN 1 as created";

                        var result = tx.run(query, Map.of(
                                "cui1", cui1,
                                "cui2", cui2,
                                "weight", weight,
                                "source", source
                        ));

                        if (result.hasNext()) {
                            successCount++;
                        }

                    } catch (Exception e) {
                        logger.debug("Failed to create relationship '{}' -[{}]-> '{}': {}",
                                cui1, relType, cui2, e.getMessage());
                    }
                }

                return successCount;
            });
        } catch (Exception e) {
            logger.error("Error in optimized batch creation: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * ניקוי לולאות עצמיות קיימות מהגרף
     */
    public int cleanSelfLoops() {
        logger.info("🧹 Starting cleanup of self-loops in the graph...");

        try (Session session = neo4jDriver.session()) {
            return session.writeTransaction(tx -> {
                // ספירת לולאות עצמיות לפני הניקוי
                var countResult = tx.run("MATCH (n)-[r]->(n) RETURN count(r) as total");
                int totalSelfLoops = countResult.hasNext() ?
                        (int) countResult.next().get("total").asLong() : 0;

                logger.info("Found {} self-loops to clean", totalSelfLoops);

                if (totalSelfLoops == 0) {
                    return 0;
                }

                // מחיקת כל הלולאות העצמיות
                var deleteResult = tx.run("MATCH (n)-[r]->(n) DELETE r");

                logger.info("✅ Successfully cleaned {} self-loops from the graph", totalSelfLoops);
                return totalSelfLoops;
            });

        } catch (Exception e) {
            logger.error("❌ Error cleaning self-loops: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * ניתוח ובדיקת הגרף הנוכחי
     */
    public Map<String, Object> analyzeCurrentGraph() {
        Map<String, Object> analysis = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                // ספירת לולאות עצמיות
                var selfLoopsResult = tx.run("MATCH (n)-[r]->(n) RETURN count(r) as total, collect(distinct type(r)) as types");
                if (selfLoopsResult.hasNext()) {
                    var record = selfLoopsResult.next();
                    analysis.put("self_loops_count", record.get("total").asLong());
                    analysis.put("self_loops_types", record.get("types").asList());
                }

                // ספירת כלל הקשרים
                var totalRelsResult = tx.run("MATCH ()-[r]->() RETURN count(r) as total");
                if (totalRelsResult.hasNext()) {
                    analysis.put("total_relationships", totalRelsResult.next().get("total").asLong());
                }

                // ספירת קשרים כפולים
                var duplicatesResult = tx.run(
                        "MATCH (n1)-[r1]->(n2), (n1)-[r2]->(n2) " +
                                "WHERE r1 <> r2 AND type(r1) = type(r2) " +
                                "RETURN count(*) as duplicates"
                );
                if (duplicatesResult.hasNext()) {
                    analysis.put("duplicate_relationships", duplicatesResult.next().get("duplicates").asLong());
                }

                // ספירת צמתים
                var nodesResult = tx.run("MATCH (n) RETURN count(n) as total");
                if (nodesResult.hasNext()) {
                    analysis.put("total_nodes", nodesResult.next().get("total").asLong());
                }

                return null;
            });

            logger.info("📊 Graph Analysis Results:");
            analysis.forEach((key, value) ->
                    logger.info("   {}: {}", key, value));

        } catch (Exception e) {
            logger.error("Error analyzing graph: {}", e.getMessage());
            analysis.put("error", e.getMessage());
        }

        return analysis;
    }

    /**
     * פונקציה מקיפה לניקוי הגרף
     */
    public Map<String, Object> performFullGraphCleanup() {
        logger.info("🧹 Starting FULL graph cleanup...");

        Map<String, Object> results = new HashMap<>();

        // 1. ניתוח ראשוני
        Map<String, Object> beforeAnalysis = analyzeCurrentGraph();
        results.put("before_cleanup", beforeAnalysis);

        // 2. ניקוי לולאות עצמיות
        int selfLoopsRemoved = cleanSelfLoops();
        results.put("self_loops_removed", selfLoopsRemoved);

        // 3. ניתוח סופי
        Map<String, Object> afterAnalysis = analyzeCurrentGraph();
        results.put("after_cleanup", afterAnalysis);

        // 4. סיכום
        long totalRelsBefore = (Long) beforeAnalysis.getOrDefault("total_relationships", 0L);
        long totalRelsAfter = (Long) afterAnalysis.getOrDefault("total_relationships", 0L);
        long totalRemoved = totalRelsBefore - totalRelsAfter;

        results.put("total_relationships_removed", totalRemoved);
        results.put("cleanup_completed", true);

        logger.info("🎉 CLEANUP COMPLETED!");
        logger.info("   📊 Relationships before: {}", totalRelsBefore);
        logger.info("   📊 Relationships after: {}", totalRelsAfter);
        logger.info("   🗑️ Total removed: {}", totalRemoved);
        logger.info("   🔄 Self-loops removed: {}", selfLoopsRemoved);

        return results;
    }
}