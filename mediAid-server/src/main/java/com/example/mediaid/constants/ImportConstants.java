package com.example.mediaid.constants;

import com.example.mediaid.neo4j.RelationshipTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * קבועים עבור תהליך ייבוא הנתונים הרפואיים
 */
public class ImportConstants {

    // =============== גדלים וכמויות ===============

    /** גודל אצווה לעיבוד קשרים */
    public static final int RELATIONSHIP_BATCH_SIZE = DatabaseConstants.RELATIONSHIP_BATCH_SIZE;

    /** גודל עמוד לקריאת נתונים מ-PostgreSQL */
    public static final int PAGE_SIZE = DatabaseConstants.PAGE_SIZE;

    /** מספר מקסימלי של ניסיונות חוזרים */
    public static final int MAX_RETRIES = DatabaseConstants.MAX_RETRIES;

    /** זמן המתנה בין ניסיונות (מילישניות) */
    public static final long RETRY_DELAY_MS = DatabaseConstants.RETRY_DELAY_MS;

    // =============== מיפוי קשרים מורחב ===============

    /** מיפוי מקשרי UMLS לקשרים של Neo4j */
    public static final Map<String, String> UMLS_TO_NEO4J_RELATIONSHIPS = new HashMap<>();
    static {
        // קשרים בסיסיים מ- RelationshipTypes
        UMLS_TO_NEO4J_RELATIONSHIPS.put("treats", RelationshipTypes.TREATS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_treat", RelationshipTypes.TREATS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_finding", RelationshipTypes.HAS_SYMPTOM);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("finding_of", RelationshipTypes.INDICATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("contraindicated_with", RelationshipTypes.CONTRAINDICATED_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("interacts_with", RelationshipTypes.INTERACTS_WITH);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("side_effect_of", RelationshipTypes.SIDE_EFFECT_OF);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_adverse_effect", RelationshipTypes.CAUSES_SIDE_EFFECT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("prevents", RelationshipTypes.MAY_PREVENT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_prevent", RelationshipTypes.MAY_PREVENT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("risk_factor_for", RelationshipTypes.RISK_FACTOR_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("diagnosed_by", RelationshipTypes.DIAGNOSED_BY);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("diagnoses", RelationshipTypes.DIAGNOSES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_location", RelationshipTypes.LOCATED_IN);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_cause", RelationshipTypes.CAUSES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_causative_agent", RelationshipTypes.CAUSED_BY);

        // קשרים מ-MRREL
        UMLS_TO_NEO4J_RELATIONSHIPS.put("ro", "RELATED_TO");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("par", "PART_OF");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("chd", "HAS_PART");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("aq", "RELATED_TO");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("qb", "RELATED_TO");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("rn", "NARROWER_THAN");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("sy", "SYNONYM_OF");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("rt", "RELATED_TO");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("rl", "SIMILAR_TO");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("rb", "BROADER_THAN");

        // קשרים רפואיים ספציפיים
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_be_prevented_by", RelationshipTypes.MAY_PREVENT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("is_finding_of", RelationshipTypes.INDICATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_manifestation", RelationshipTypes.HAS_SYMPTOM);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("manifestation_of", RelationshipTypes.INDICATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("associated_with", "ASSOCIATED_WITH");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("occurs_in", RelationshipTypes.LOCATED_IN);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("part_of", "PART_OF");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_part", "HAS_PART");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("isa", "IS_A");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("inverse_isa", "SUBTYPE_OF");

        // קשרי תרופות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("ingredient_of", "INGREDIENT_OF");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_ingredient", "HAS_INGREDIENT");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("form_of", "FORM_OF");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_form", "HAS_FORM");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("dose_form_of", "DOSE_FORM_OF");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_active_ingredient", RelationshipTypes.HAS_ACTIVE_INGREDIENT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("active_ingredient_of", RelationshipTypes.ACTIVE_INGREDIENT_OF);

        // קשרי אבחון ובדיקות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("method_of", "METHOD_OF");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_method", "HAS_METHOD");
        UMLS_TO_NEO4J_RELATIONSHIPS.put("measurement_of", RelationshipTypes.DIAGNOSES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("measured_by", RelationshipTypes.DIAGNOSED_BY);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("requires_test", RelationshipTypes.REQUIRES_TEST);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("test_for", RelationshipTypes.REQUIRES_TEST);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("indicated_test", RelationshipTypes.REQUIRES_TEST);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("recommend_test", RelationshipTypes.REQUIRES_TEST);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("suggests_test", RelationshipTypes.REQUIRES_TEST);
    }

    /** מיפוי קודי REL לסוגי קשרים */
    public static final Map<String, String> REL_TO_RELATIONSHIP = Map.of(
            "RO", "RELATED_TO",
            "PAR", "PART_OF",
            "CHD", "HAS_PART",
            "RQ", "RELATED_TO",
            "RB", "BROADER_THAN",
            "RN", "NARROWER_THAN",
            "SY", "SYNONYM_OF",
            "RT", "RELATED_TO"
    );

    /** קשרים שלא רלוונטיים רפואית */
    public static final Set<String> EXCLUDED_RELATIONSHIP_TYPES = Set.of(
            "translation_of", "translated_into", "mapped_to", "mapped_from",
            "lexical_variant_of", "spelling_variant_of", "abbreviation_of",
            "expanded_form_of", "acronym_for", "short_form_of", "long_form_of"
    );

    // =============== הודעות מערכת ===============

    /** הודעות התחלה וסיום */
    public static final class Messages {
        public static final String IMPORT_STARTED = "Starting relationship import from MRREL file";
        public static final String IMPORT_COMPLETED = "Relationship import completed successfully";
        public static final String ANALYZING_FILE = "Analyzing MRREL file structure";
        public static final String LOADING_NODES = "Loading existing nodes from Neo4j";
        public static final String CREATING_BATCH = "Creating relationship batch";
        public static final String BATCH_COMPLETED = "Batch creation completed";
        public static final String CLEANUP_STARTED = "Starting graph cleanup";
        public static final String CLEANUP_COMPLETED = "Graph cleanup completed";

        /** הודעות שגיאה */
        public static final String ERROR_FILE_READ = "Error reading MRREL file";
        public static final String ERROR_BATCH_CREATE = "Error creating relationship batch";
        public static final String ERROR_NODE_MISSING = "Referenced node not found in graph";
        public static final String ERROR_INVALID_FORMAT = "Invalid line format in MRREL file";
    }

    // =============== הגדרות ביצועים ===============

    /** מספר קווים מקסימלי לניתוח מקדים */
    public static final int ANALYSIS_MAX_LINES = DatabaseConstants.ANALYSIS_MAX_LINES;

    /** תדירות דיווח התקדמות */
    public static final int PROGRESS_REPORT_INTERVAL = DatabaseConstants.PROGRESS_REPORT_INTERVAL;

    /** תדירות דיווח אצווה */
    public static final int BATCH_REPORT_INTERVAL = DatabaseConstants.BATCH_REPORT_INTERVAL;

    /** זמן המתנה בין אצוות (מילישניות) */
    public static final int BATCH_DELAY_MS = DatabaseConstants.BATCH_DELAY_MS;

    // =============== הגדרות אינדקסים ===============

    /** רשימת סוגי ישויות ליצירת אינדקסים */
    public static final String[] INDEXED_ENTITY_TYPES = DatabaseConstants.INDEXED_ENTITY_TYPES;

    // =============== הגדרות ולידציה ===============

    /** אורך מקסימלי לשם קשר */
    public static final int MAX_RELATIONSHIP_NAME_LENGTH = DatabaseConstants.MAX_RELATIONSHIP_NAME_LENGTH;

    /** ערך ברירת מחדל לקשר לא ידוע */
    public static final String DEFAULT_RELATIONSHIP_TYPE = DatabaseConstants.DEFAULT_RELATIONSHIP_TYPE;

    /** מספר מינימלי של שדות בשורת MRREL */
    public static final int MIN_MRREL_FIELDS = DatabaseConstants.MIN_MRREL_FIELDS;

    // =============== הגדרות Demo Mode ===============

    /** מספר מקסימלי של קשרים למושג Demo */
    public static final int MAX_DEMO_RELATIONSHIPS_PER_CONCEPT = DatabaseConstants.MAX_DEMO_RELATIONSHIPS_PER_CONCEPT;

    /** רמת ביטחון מינימלית במצב Demo */
    public static final double DEMO_MIN_CONFIDENCE = DatabaseConstants.DEMO_MIN_CONFIDENCE;

    /** מספר מקסימלי של תוצאות חיפוש */
    public static final int MAX_SEARCH_RESULTS = DatabaseConstants.MAX_SEARCH_RESULTS;

    /** מספר מקסימלי של קשרים לישות אחת */
    public static final int MAX_CONNECTIONS_PER_ENTITY = MedicalAnalysisConstants.MAX_CONNECTIONS_PER_ENTITY;
}