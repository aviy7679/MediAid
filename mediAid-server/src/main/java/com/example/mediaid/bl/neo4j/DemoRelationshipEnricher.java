package com.example.mediaid.bl.neo4j;

import com.example.mediaid.dal.UMLS_terms.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
public class DemoRelationshipEnricher {

    private static final Logger logger = LoggerFactory.getLogger(DemoRelationshipEnricher.class);
    private static final int BATCH_SIZE = 1000;

    @Autowired
    private Driver neo4jDriver;

    @Autowired
    private Environment environment;

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private SymptomRepository symptomRepository;

    @Autowired
    private RiskFactorRepository riskFactorRepository;

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private AnatomicalStructureRepository anatomicalStructureRepository;

    @Autowired
    private LabTestRepository labTestRepository;

    @Autowired
    private BiologicalFunctionRepository biologicalFunctionRepository;

    // הרחבת המיפויים לקשרים רלוונטיים רפואית
    private static final Map<String, String> RELATIONSHIP_MAPPING = new HashMap<>();
    static {
        // כל הקשרים הקיימים
        RELATIONSHIP_MAPPING.putAll(RelationshipTypes.UMLS_TO_NEO4J_RELATIONSHIPS);

        // הוספת קשרים נוספים
        RELATIONSHIP_MAPPING.put("ro", "RELATED_TO");
        RELATIONSHIP_MAPPING.put("par", "PART_OF");
        RELATIONSHIP_MAPPING.put("chd", "HAS_PART");
        RELATIONSHIP_MAPPING.put("aq", "RELATED_TO");
        RELATIONSHIP_MAPPING.put("qb", "RELATED_TO");
        RELATIONSHIP_MAPPING.put("rn", "NARROWER_THAN");
        RELATIONSHIP_MAPPING.put("sy", "SYNONYM_OF");
        RELATIONSHIP_MAPPING.put("rt", "RELATED_TO");
        RELATIONSHIP_MAPPING.put("rl", "SIMILAR_TO");
        RELATIONSHIP_MAPPING.put("rb", "BROADER_THAN");
        RELATIONSHIP_MAPPING.put("may_be_prevented_by", RelationshipTypes.MAY_PREVENT);
        RELATIONSHIP_MAPPING.put("is_finding_of", RelationshipTypes.INDICATES);
        RELATIONSHIP_MAPPING.put("has_finding", RelationshipTypes.HAS_SYMPTOM);
        RELATIONSHIP_MAPPING.put("associated_with", "ASSOCIATED_WITH");
        RELATIONSHIP_MAPPING.put("occurs_in", RelationshipTypes.LOCATED_IN);
        RELATIONSHIP_MAPPING.put("part_of", "PART_OF");
        RELATIONSHIP_MAPPING.put("has_part", "HAS_PART");
        RELATIONSHIP_MAPPING.put("isa", "IS_A");
        RELATIONSHIP_MAPPING.put("inverse_isa", "SUBTYPE_OF");
        RELATIONSHIP_MAPPING.put("ingredient_of", "INGREDIENT_OF");
        RELATIONSHIP_MAPPING.put("has_ingredient", "HAS_INGREDIENT");
        RELATIONSHIP_MAPPING.put("form_of", "FORM_OF");
        RELATIONSHIP_MAPPING.put("has_form", "HAS_FORM");
        RELATIONSHIP_MAPPING.put("dose_form_of", "DOSE_FORM_OF");
        RELATIONSHIP_MAPPING.put("method_of", "METHOD_OF");
        RELATIONSHIP_MAPPING.put("has_method", "HAS_METHOD");
        RELATIONSHIP_MAPPING.put("measurement_of", RelationshipTypes.DIAGNOSES);
        RELATIONSHIP_MAPPING.put("measured_by", RelationshipTypes.DIAGNOSED_BY);
    }

    // REL mappings
    private static final Map<String, String> REL_TO_RELATIONSHIP = Map.of(
            "RO", "RELATED_TO",
            "PAR", "PART_OF",
            "CHD", "HAS_PART",
            "RQ", "RELATED_TO",
            "RB", "BROADER_THAN",
            "RN", "NARROWER_THAN",
            "SY", "SYNONYM_OF",
            "RT", "RELATED_TO"
    );

    // סינון קשרים לא רלוונטיים
    private static final Set<String> EXCLUDED_RELATIONSHIP_TYPES = Set.of(
            "translation_of", "translated_into", "mapped_to", "mapped_from",
            "lexical_variant_of", "spelling_variant_of", "abbreviation_of",
            "expanded_form_of", "acronym_for", "short_form_of", "long_form_of"
    );

    private Set<String> allExistingCuis;
    private Set<String> existingRelationships;

    /**
     * המתודה הראשית - העשרת קשרים למושגי דמו
     */
    public Map<String, Object> enrichDemoRelationships() {
        logger.info("🚀 Starting Demo Relationship Enrichment");
        logger.info("📋 Demo concepts to enrich: {}", DemoMode.DEMO_CUIS.size());

        Map<String, Object> results = new HashMap<>();
        results.put("startTime", System.currentTimeMillis());

        try {
            // שלב 1: טעינת נתונים קיימים
            loadExistingData();

            // שלב 2: קבלת נתיב קובץ MRREL
            String mrrelPath = environment.getProperty("mediaid.umls.mrrel.path");
            if (mrrelPath == null || mrrelPath.isEmpty()) {
                throw new RuntimeException("MRREL file path not configured");
            }

            // שלב 3: עיבוד קובץ MRREL למושגי דמו
            Map<String, Integer> enrichmentResults = processMrrelForDemoCuis(mrrelPath);

            // שלב 4: החזרת תוצאות
            results.put("success", true);
            results.put("demoCuiResults", enrichmentResults);
            results.put("totalNewRelationships", enrichmentResults.values().stream().mapToInt(Integer::intValue).sum());
            results.put("endTime", System.currentTimeMillis());
            results.put("duration", (Long)results.get("endTime") - (Long)results.get("startTime"));

            logger.info("✅ Demo Relationship Enrichment completed successfully");
            logger.info("📊 Total new relationships added: {}", results.get("totalNewRelationships"));

        } catch (Exception e) {
            logger.error("❌ Error in demo relationship enrichment: {}", e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        return results;
    }

    /**
     * טעינת נתונים קיימים מהמסד
     */
    private void loadExistingData() {
        logger.info("📚 Loading existing data from database...");

        // טעינת כל הCUIים הקיימים
        allExistingCuis = new HashSet<>();

        diseaseRepository.findAll().forEach(d -> allExistingCuis.add(d.getCui()));
        medicationRepository.findAll().forEach(m -> allExistingCuis.add(m.getCui()));
        symptomRepository.findAll().forEach(s -> allExistingCuis.add(s.getCui()));
        riskFactorRepository.findAll().forEach(rf -> allExistingCuis.add(rf.getCui()));
        procedureRepository.findAll().forEach(p -> allExistingCuis.add(p.getCui()));
        anatomicalStructureRepository.findAll().forEach(as -> allExistingCuis.add(as.getCui()));
        labTestRepository.findAll().forEach(lt -> allExistingCuis.add(lt.getCui()));
        biologicalFunctionRepository.findAll().forEach(bf -> allExistingCuis.add(bf.getCui()));

        logger.info("📊 Loaded {} existing CUIs from database", allExistingCuis.size());

        // ספירת כמה מושגי דמו קיימים במסד
        long demoInDatabase = DemoMode.DEMO_CUIS.stream()
                .mapToLong(cui -> allExistingCuis.contains(cui) ? 1 : 0)
                .sum();
        logger.info("🎯 Demo CUIs in database: {} out of {}", demoInDatabase, DemoMode.DEMO_CUIS.size());

        // טעינת קשרים קיימים מNeo4j
        loadExistingRelationships();
    }

    /**
     * טעינת קשרים קיימים מNeo4j למניעת כפילויות
     */
    private void loadExistingRelationships() {
        logger.info("🔗 Loading existing relationships from Neo4j...");
        existingRelationships = new HashSet<>();

        try (Session session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(
                        "MATCH (n1)-[r]->(n2) " +
                                "WHERE n1.cui IS NOT NULL AND n2.cui IS NOT NULL " +
                                "RETURN n1.cui as cui1, n2.cui as cui2, type(r) as relType " +
                                "LIMIT 1000000"
                );

                result.forEachRemaining(record -> {
                    String cui1 = record.get("cui1").asString();
                    String cui2 = record.get("cui2").asString();
                    String relType = record.get("relType").asString();
                    existingRelationships.add(createRelationshipKey(cui1, cui2, relType));
                });

                return null;
            });
        } catch (Exception e) {
            logger.warn("⚠️ Could not load existing relationships: {}", e.getMessage());
        }

        logger.info("📊 Loaded {} existing relationships", existingRelationships.size());
    }

    /**
     * עיבוד קובץ MRREL עבור מושגי הדמו בלבד
     */
    private Map<String, Integer> processMrrelForDemoCuis(String mrrelPath) throws IOException {
        logger.info("🔍 Processing MRREL file for demo CUIs: {}", mrrelPath);

        Map<String, Integer> demoCuiResults = new HashMap<>();
        List<Map<String, Object>> pendingRelationships = new ArrayList<>();

        // אתחול מונים למושגי דמו
        for (String demoCui : DemoMode.DEMO_CUIS) {
            demoCuiResults.put(demoCui, 0);
        }

        int totalLinesProcessed = 0;
        int relationshipsFound = 0;
        int relationshipsAdded = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                totalLinesProcessed++;

                if (totalLinesProcessed % 1000000 == 0) {
                    logger.info("📊 Processed {} lines, found {} demo relationships, added {} to Neo4j",
                            totalLinesProcessed, relationshipsFound, relationshipsAdded);
                }

                // עיבוד השורה
                RelationshipCandidate candidate = processLineForDemoCuis(line);
                if (candidate != null) {
                    relationshipsFound++;

                    // הוספה לרשימת הקשרים הממתינים
                    Map<String, Object> relationship = createRelationshipMap(candidate);
                    pendingRelationships.add(relationship);

                    // עדכון מונה למושג הדמו
                    if (DemoMode.DEMO_CUIS.contains(candidate.cui1)) {
                        demoCuiResults.put(candidate.cui1, demoCuiResults.get(candidate.cui1) + 1);
                    }
                    if (DemoMode.DEMO_CUIS.contains(candidate.cui2)) {
                        demoCuiResults.put(candidate.cui2, demoCuiResults.get(candidate.cui2) + 1);
                    }

                    // יצירת באצ' כשמגיעים לגודל מסוים
                    if (pendingRelationships.size() >= BATCH_SIZE) {
                        int created = createRelationshipsBatch(pendingRelationships);
                        relationshipsAdded += created;
                        pendingRelationships.clear();
                        logger.debug("💾 Created {} relationships in batch", created);
                    }
                }
            }

            // טיפול בבאצ' האחרון
            if (!pendingRelationships.isEmpty()) {
                int created = createRelationshipsBatch(pendingRelationships);
                relationshipsAdded += created;
                logger.info("💾 Created {} relationships in final batch", created);
            }

            logger.info("📈 MRREL Processing Summary:");
            logger.info("   📊 Total lines processed: {}", totalLinesProcessed);
            logger.info("   🎯 Demo relationships found: {}", relationshipsFound);
            logger.info("   ✅ Relationships added to Neo4j: {}", relationshipsAdded);

            // הדפסת תוצאות למושגי דמו עם הכי הרבה קשרים
            logger.info("🏆 Top demo concepts by new relationships:");
            demoCuiResults.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(10)
                    .forEach(entry -> logger.info("   {} ({}): {} relationships",
                            entry.getKey(),
                            getDemoConceptName(entry.getKey()),
                            entry.getValue()));

        } catch (IOException e) {
            logger.error("💥 Error reading MRREL file: {}", e.getMessage());
            throw e;
        }

        return demoCuiResults;
    }

    /**
     * עיבוד שורה אחת מMRREL עבור מושגי דמו
     */
    private RelationshipCandidate processLineForDemoCuis(String line) {
        String[] fields = line.split("\\|");
        if (fields.length < 15) {
            return null;
        }

        String cui1 = fields[0];
        String cui2 = fields[4];
        String rel = fields[3];
        String rela = fields[7];
        String sab = fields[10];

        // בדיקה שלפחות אחד מהמושגים הוא דמו
        boolean isDemoRelevant = DemoMode.DEMO_CUIS.contains(cui1) || DemoMode.DEMO_CUIS.contains(cui2);
        if (!isDemoRelevant) {
            return null;
        }

        // בדיקה ששני המושגים קיימים במסד
        if (!allExistingCuis.contains(cui1) || !allExistingCuis.contains(cui2)) {
            return null;
        }

        // מניעת לולאות עצמיות
        if (cui1.equals(cui2)) {
            return null;
        }

        // קביעת סוג הקשר
        String relationshipType = determineRelationshipType(rel, rela);
        if (relationshipType == null || EXCLUDED_RELATIONSHIP_TYPES.contains(relationshipType.toLowerCase())) {
            return null;
        }

        // בדיקת כפילויות
        String relationshipKey = createRelationshipKey(cui1, cui2, relationshipType);
        if (existingRelationships.contains(relationshipKey)) {
            return null;
        }

        // חישוב משקל
        double weight = RelationshipTypes.calculateRelationshipWeight(
                rela != null && !rela.trim().isEmpty() ? rela : rel, sab);

        return new RelationshipCandidate(cui1, cui2, relationshipType, weight, sab, rel, rela);
    }

    /**
     * קביעת סוג הקשר מREL ו-RELA
     */
    private String determineRelationshipType(String rel, String rela) {
        // בדיקה אם יש RELA (ספציפי יותר)
        if (rela != null && !rela.trim().isEmpty()) {
            String normalized = rela.trim().toLowerCase();
            if (RELATIONSHIP_MAPPING.containsKey(normalized)) {
                return RELATIONSHIP_MAPPING.get(normalized);
            }
            return normalizeRelationshipName(normalized);
        }

        // אם אין RELA, השתמש ב-REL
        if (rel != null && !rel.trim().isEmpty()) {
            String normalized = rel.trim().toUpperCase();
            if (REL_TO_RELATIONSHIP.containsKey(normalized)) {
                return REL_TO_RELATIONSHIP.get(normalized);
            }
            return normalizeRelationshipName(rel.trim().toLowerCase());
        }

        return null;
    }

    /**
     * נרמול שם קשר
     */
    private String normalizeRelationshipName(String name) {
        return name.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("[^A-Z0-9_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * יצירת מפה לקשר
     */
    private Map<String, Object> createRelationshipMap(RelationshipCandidate candidate) {
        Map<String, Object> relationship = new HashMap<>();
        relationship.put("cui1", candidate.cui1);
        relationship.put("cui2", candidate.cui2);
        relationship.put("relType", candidate.relationshipType);
        relationship.put("weight", candidate.weight);
        relationship.put("source", candidate.source);
        relationship.put("originalRel", candidate.originalRel);
        relationship.put("originalRela", candidate.originalRela);
        return relationship;
    }

    /**
     * יצירת מפתח ייחודי לקשר
     */
    private String createRelationshipKey(String cui1, String cui2, String relType) {
        return cui1 + "|" + relType + "|" + cui2;
    }

    /**
     * יצירת באצ' של קשרים בNeo4j
     */
    private int createRelationshipsBatch(List<Map<String, Object>> relationships) {
        if (relationships.isEmpty()) return 0;

        int successCount = 0;

        try (Session session = neo4jDriver.session()) {
            successCount = session.writeTransaction(tx -> {
                int count = 0;

                for (Map<String, Object> rel : relationships) {
                    try {
                        String cui1 = (String) rel.get("cui1");
                        String cui2 = (String) rel.get("cui2");
                        String relType = normalizeRelationshipTypeForNeo4j((String) rel.get("relType"));
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

                        // הוספה לקאש כדי למנוע כפילויות
                        existingRelationships.add(createRelationshipKey(cui1, cui2, relType));

                    } catch (Exception e) {
                        logger.debug("Failed to create relationship: {}", e.getMessage());
                    }
                }

                return count;
            });

        } catch (Exception e) {
            logger.warn("Batch creation failed: {}", e.getMessage());
        }

        return successCount;
    }

    /**
     * נרמול שם קשר עבור Neo4j
     */
    private String normalizeRelationshipTypeForNeo4j(String relType) {
        if (relType == null || relType.trim().isEmpty()) {
            return "RELATED_TO";
        }

        String normalized = relType.trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("[^A-Z0-9_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isEmpty() || !Character.isLetter(normalized.charAt(0))) {
            return "RELATED_TO";
        }

        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }

        return normalized;
    }

    /**
     * קבלת שם מושג דמו לתצוגה
     */
    private String getDemoConceptName(String cui) {
        // זוהי מתודה פשוטה - ניתן להרחיב עם מיפוי מלא
        switch (cui) {
            case "C0004057": return "Aspirin";
            case "C0025598": return "Metformin";
            case "C4013416": return "Diabetes";
            case "C0004096": return "Asthma";
            case "C3807341": return "Chest pain";
            case "C0018681": return "Headache";
            default: return "Unknown";
        }
    }

    /**
     * מחלקה פנימית לייצוג מועמד לקשר
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
    }
}