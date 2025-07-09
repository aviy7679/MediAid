package com.example.mediaid.constants;

/**
 * קבועים הקשורים לעבודה עם מסדי נתונים (PostgreSQL ו-Neo4j)
 */
public class DatabaseConstants {

    // =============== אצוות עיבוד (Batch Processing) ===============

    /** גודל אצווה לייבוא ישויות ל-Neo4j */
    public static final int ENTITY_IMPORT_BATCH_SIZE = 5000;
    // סיבה: איזון בין זיכרון לביצועים - 5000 רשומות לא יגרמו ל-OutOfMemory

    /** גודל אצווה לייבוא קשרים ל-Neo4j */
    public static final int RELATIONSHIP_BATCH_SIZE = 1000;
    // סיבה: קשרים צורכים יותר זיכרון מישויות, לכן אצווה קטנה יותר

    /** גודל אצווה קטנה למניעת timeout */
    public static final int SMALL_BATCH_SIZE = 50;
    // סיבה: למניעת timeout בחיבורים איטיים

    /** גודל עמוד לקריאה מ-PostgreSQL */
    public static final int PAGE_SIZE = 1000;
    // סיבה: מספר רשומות סביר לעמוד אחד ללא עומס יתר על הזיכרון

    // =============== חיבורים ו-Timeouts ===============

    /** מספר מקסימלי של ניסיונות חוזרים */
    public static final int MAX_RETRIES = 3;
    // סיבה: מספר סביר של ניסיונות חוזרים במקרה של תקלות זמניות

    /** זמן המתנה בין ניסיונות חוזרים (מילישניות) */
    public static final long RETRY_DELAY_MS = 2000;
    // סיבה: 2 שניות מספיקות לכל מערכת להתאושש מתקלה זמנית

    /** זמן המתנה בין אצוות (מילישניות) */
    public static final int BATCH_DELAY_MS = 10;
    // סיבה: הפסקה קצרה כדי לא להציף את מסד הנתונים

    /** מספר מקסימלי של חיבורים ל-Neo4j */
    public static final int NEO4J_MAX_CONNECTIONS = 50;
    // סיבה: מספר חיבורים סביר לאפליקציה בגודל בינוני

    /** timeout לחיבור Neo4j (מילישניות) */
    public static final int NEO4J_CONNECTION_TIMEOUT = 30000;
    // סיבה: 30 שניות מספיקות לחיבור מסד נתונים מרוחק

    /** timeout לשאילתות Neo4j (מילישניות) */
    public static final int NEO4J_QUERY_TIMEOUT = 60000;
    // סיבה: שאילתות מורכבות יכולות לקחת זמן, אבל לא יותר מדקה

    // =============== הגדרות ניתוח קבצים ===============

    /** מספר קווים מקסימלי לניתוח מקדים של קובץ MRREL */
    public static final int ANALYSIS_MAX_LINES = 100000;
    // סיבה: מספיק כדי לקבל מושג על מבנה הקובץ מבלי לעבור על כל הקובץ

    /** מספר מינימלי של שדות בשורת MRREL */
    public static final int MIN_MRREL_FIELDS = 15;
    // סיבה: פורמט קובץ MRREL דורש לפחות 15 שדות לשורה תקינה

    /** תדירות דיווח התקדמות עיבוד */
    public static final int PROGRESS_REPORT_INTERVAL = 1000000;
    // סיבה: מיליון שורות = דיווח שלא יספם את הלוג אבל יתן מידע על התקדמות

    /** תדירות דיווח אצווה */
    public static final int BATCH_REPORT_INTERVAL = 10;
    // סיבה: כל 10 אצוות = דיווח לא תכוף מדי

    // =============== הגבלות נתונים ===============

    /** אורך מקסימלי לשם בישות רפואית */
    public static final int MAX_ENTITY_NAME_LENGTH = 250;
    // סיבה: מספיק לשמות רפואיים באנגלית, תואם להגבלות PostgreSQL

    /** אורך מקסימלי לשם קשר */
    public static final int MAX_RELATIONSHIP_NAME_LENGTH = 50;
    // סיבה: שמות קשרים צריכים להיות קצרים וברורים

    /** מספר מקסימלי של קשרים למושג במצב Demo */
    public static final int MAX_DEMO_RELATIONSHIPS_PER_CONCEPT = 1000;
    // סיבה: הגבלה למצב Demo כדי לא להציף את הגרף

    /** מספר מקסימלי של תוצאות חיפוש */
    public static final int MAX_SEARCH_RESULTS = 100;
    // סיבה: מספיק תוצאות למשתמש מבלי להחזיר אלפי תוצאות

    // =============== הגדרות אינדקסים ===============

    /** רשימת סוגי ישויות ליצירת אינדקסים */
    public static final String[] INDEXED_ENTITY_TYPES = {
            "Disease", "Medication", "Symptom", "RiskFactor",
            "Procedure", "AnatomicalStructure", "LaboratoryTest", "BiologicalFunction"
    };
    // סיבה: הישויות המרכזיות שצריכות אינדקסים לחיפוש מהיר

    /** ערך ברירת מחדל לקשר לא ידוע */
    public static final String DEFAULT_RELATIONSHIP_TYPE = "RELATED_TO";
    // סיבה: קשר כללי כשלא מצליחים לזהות את הסוג הספציפי

    // =============== הגדרות Demo Mode ===============

    /** רמת ביטחון מינימלית במצב Demo */
    public static final double DEMO_MIN_CONFIDENCE = 0.3;
    // סיבה: סף נמוך מספיק שיאפשר תוצאות, אבל לא יכלול רעש רב

    // =============== נתיבי קבצים של UMLS ===============

    /** נתיב לקובץ MRCONSO של UMLS */
    public static final String MRCONSO_FILE_PATH = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRCONSO.RRF";
    // סיבה: נתיב קבוע לקובץ המונחים הראשי של UMLS

    /** נתיב לקובץ MRSTY של UMLS */
    public static final String MRSTY_FILE_PATH = "D:\\MediAid\\umls-2024AB-full\\2024AB-full\\2024AB\\META\\MRSTY.RRF";
    // סיבה: נתיב קבוע לקובץ הסוגים הסמנטיים של UMLS

    /** נתיב לקובץ config.properties */
    public static final String CONFIG_FILE_PATH = "D:\\MediAid\\mediAid-server\\src\\main\\resources\\config.properties";
    // סיבה: נתיב קבוע לקובץ ההגדרות של האפליקציה

    // =============== קבועי UMLS Processors ===============

    /** סוגים סמנטיים למחלות ב-UMLS */
    public static final String[] DISEASE_SEMANTIC_TYPES = {
            "T046", // Disease or Syndrome
            "T047", // Disease or Finding
            "T191"  // Neoplastic Process
    };
    // סיבה: הסוגים הסמנטיים של UMLS שמזהים מחלות

    /** סוגים סמנטיים לתרופות ב-UMLS */
    public static final String[] MEDICATION_SEMANTIC_TYPES = {
            "T121", // Pharmacologic Substance
            "T200", // Clinical Drug
            "T195", // Antibiotic
            "T125"  // Hormone
    };
    // סיבה: הסוגים הסמנטיים של UMLS שמזהים תרופות

    /** סוגים סמנטיים לסימפטומים ב-UMLS */
    public static final String[] SYMPTOM_SEMANTIC_TYPES = {
            "T184"  // Sign or Symptom
    };
    // סיבה: הסוג הסמנטי של UMLS שמזהה סימפטומים

    /** מקורות מידע מועדפים למחלות */
    public static final String[] DISEASE_PREFERRED_SOURCES = {
            "SNOMEDCT_US", // SNOMED CT
            "MSH",        // Medical Subject Headings
            "ICD10CM",    // ICD-10 Clinical Modification
            "LNC",        // Logical Observation Identifiers Names and Codes
            "MEDDRA"      // Medical Dictionary for Regulatory Activities
    };
    // סיבה: מקורות המידע המהימנים ביותר למחלות ב-UMLS

    /** מקורות מידע מועדפים לתרופות */
    public static final String[] MEDICATION_PREFERRED_SOURCES = {
            "RXNORM",     // RxNorm
            "SNOMEDCT_US", // SNOMED CT
            "MSH",        // Medical Subject Headings
            "ATC",        // Anatomical Therapeutic Chemical
            "MEDDRA",     // Medical Dictionary for Regulatory Activities
            "NDFRT"       // National Drug File - Reference Terminology
    };
    // סיבה: מקורות המידע המהימנים ביותר לתרופות ב-UMLS

    /** מקורות מידע מועדפים לסימפטומים */
    public static final String[] SYMPTOM_PREFERRED_SOURCES = {
            "SNOMEDCT_US", // SNOMED CT
            "MSH",        // Medical Subject Headings
            "MEDDRA",     // Medical Dictionary for Regulatory Activities
            "ICD10CM"     // ICD-10 Clinical Modification
    };
    // סיבה: מקורות המידע המהימנים ביותר לסימפטומים ב-UMLS
}