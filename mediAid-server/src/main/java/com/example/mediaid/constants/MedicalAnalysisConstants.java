package com.example.mediaid.constants;

/**
 * קבועים הקשורים לניתוח רפואי, גרף רפואי וחישוב סיכונים
 */
public class MedicalAnalysisConstants {

    // =============== הגדרות ניתוח מסלולים רפואיים ===============

    /** עומק מקסימלי לחיפוש מסלולים בגרף */
    public static final int MAX_PATH_DEPTH = 5;
    // סיבה: מעל 5 צעדים החיבור הרפואי הופך להיות לא רלוונטי קלינית

    /** מספר מקסימלי של מסלולים לחזור במסלול אחד */
    public static final int MAX_PATHWAYS_PER_SOURCE = 15;
    // סיבה: מספיק מסלולים לניתוח מבלי להציף את המערכת

    /** רמת ביטחון מינימלית למסלול רפואי */
    public static final double MIN_PATHWAY_CONFIDENCE = 0.1;
    // סיבה: סף נמוך מספיק שלא יפסיד מידע, אבל יסנן רעש

    /** משקל ברירת מחדל לקשר רפואי */
    public static final double DEFAULT_MEDICAL_WEIGHT = 0.5;
    // סיבה: נקודת אמצע כאשר אין מידע ספציפי על חוזק הקשר

    // =============== הגדרות גורמי סיכון ===============

    /** גורם דעיכה לחישוב סיכונים במסלולים */
    public static final double RISK_DECAY_FACTOR = 0.85;
    // סיבה: כל צעד במסלול מקטין את הסיכון ב-15%, מספיק כדי לתת משקל למרחק

    /** משקל מינימלי לגורם סיכון */
    public static final double MIN_RISK_FACTOR_WEIGHT = 0.1;
    // סיבה: מתחת לזה גורם הסיכון לא משמעותי קלינית

    /** משקל מקסימלי לגורם סיכון */
    public static final double MAX_RISK_FACTOR_WEIGHT = 0.99;
    // סיבה: לא 1.0 כי זה יצור בעיות בחישובים לוגריתמיים

    /** רמת סיכון מינימלית לפרופגציה */
    public static final double MIN_RISK_PROPAGATION = 0.05;
    // סיבה: מתחת לזה הסיכון זניח מבחינה קלינית

    // =============== הגדרות קהילות רפואיות ===============

    /** גודל מינימלי לקהילה רפואית */
    public static final int MIN_COMMUNITY_SIZE = 2;
    // סיבה: קהילה חייבת לכלול לפחות 2 ישויות

    /** מספר מקסימלי של קהילות להחזיר */
    public static final int MAX_COMMUNITIES_TO_RETURN = 10;
    // סיבה: מספיק קהילות לניתוח מבלי להציף את המשתמש

    /** רמת לכידות מינימלית לקהילה */
    public static final double MIN_COMMUNITY_COHESION = 0.3;
    // סיבה: מתחת לזה הקהילה לא מספיק קשורה פנימית

    /** מספר מקסימלי של איטרציות לזיהוי קהילות */
    public static final int MAX_COMMUNITY_DETECTION_ITERATIONS = 100;
    // סיבה: מספיק איטרציות להתכנסות מבלי לתקוע את המערכת

    // =============== הגדרות מרכזיות רפואית (Medical Hubs) ===============

    /** ציון מרכזיות מינימלי להגדרה כ-hub */
    public static final double MIN_CENTRALITY_SCORE = 10.0;
    // סיבה: רק ישויות עם השפעה משמעותית נחשבות ל-hub

    /** מספר מקסימלי של hubs להחזיר */
    public static final int MAX_HUBS_TO_RETURN = 5;
    // סיבה: מספיק hubs לניתוח מבלי להציף את המשתמש

    /** ציון מרכזיות גבוה (להגדרה כ-"השפעה גבוהה") */
    public static final double HIGH_CENTRALITY_THRESHOLD = 20.0;
    // סיבה: כפל מהמינימום = השפעה גבוהה באמת

    /** ציון מרכזיות גבוה מאוד */
    public static final double VERY_HIGH_CENTRALITY_THRESHOLD = 50.0;
    // סיבה: רק ישויות מרכזיות מאוד יגיעו לכאן

    // =============== הגדרות רמות דחיפות ===============

    /** סף לרמת דחיפות נמוכה */
    public static final double LOW_URGENCY_THRESHOLD = 0.3;
    // סיבה: מתחת לזה הסיכון נמוך מספיק שלא דורש פעולה מיידית

    /** סף לרמת דחיפות בינונית */
    public static final double MEDIUM_URGENCY_THRESHOLD = 0.5;
    // סיבה: נקודת אמצע סבירה לדחיפות בינונית

    /** סף לרמת דחיפות גבוהה */
    public static final double HIGH_URGENCY_THRESHOLD = 0.8;
    // סיבה: מעל זה המצב דורש תשומת לב מיידית

    /** מכפיל דחיפות מינימלי */
    public static final double MIN_URGENCY_MULTIPLIER = 1.0;
    // סיבה: לא יכול להיות מכפיל נמוך מ-1

    /** מכפיל דחיפות מקסימלי */
    public static final double MAX_URGENCY_MULTIPLIER = 2.0;
    // סיבה: מכפיל גבוה מדי יכול ליצור false alarms

    // =============== הגדרות ביטחון לסימפטומים ===============

    /** רמת ביטחון מינימלית לזיהוי סימפטום */
    public static final double MIN_SYMPTOM_CONFIDENCE = 0.3;
    // סיבה: מתחת לזה הזיהוי לא מספיק אמין

    /** רמת ביטחון גבוהה לזיהוי סימפטום */
    public static final double HIGH_SYMPTOM_CONFIDENCE = 0.7;
    // סיבה: מעל זה הזיהוי אמין מספיק לפעולה

    /** רמת ביטחון מקסימלית מעשית */
    public static final double MAX_PRACTICAL_CONFIDENCE = 0.95;
    // סיבה: 100% ביטחון לא מעשי במערכות רפואיות

    // =============== הגדרות חיפוש וסינון ===============

    /** מספר מקסימלי של תוצאות חיפוש רפואי */
    public static final int MAX_MEDICAL_SEARCH_RESULTS = 30;
    // סיבה: מספיק תוצאות למשתמש מבלי להציף אותו

    /** מספר מקסימלי של קשרים לישות אחת */
    public static final int MAX_CONNECTIONS_PER_ENTITY = 100;
    // סיבה: מספיק קשרים לניתוח מבלי להציף את המערכת

    /** אורך מינימלי לחיפוש טקסט רפואי */
    public static final int MIN_MEDICAL_SEARCH_LENGTH = 2;
    // סיבה: מתחת לזה החיפוש יחזיר יותר מדי תוצאות לא רלוונטיות

    // =============== הגדרות תופעות לוואי ואינטראקציות ===============

    /** רמת ביטחון מינימלית לתופעת לוואי */
    public static final double MIN_SIDE_EFFECT_CONFIDENCE = 0.6;
    // סיבה: תופעות לוואי רגישות יותר, צריך רמת ביטחון גבוהה יותר

    /** רמת ביטחון מינימלית לאינטראקציה בין תרופות */
    public static final double MIN_DRUG_INTERACTION_CONFIDENCE = 0.7;
    // סיבה: אינטראקציות בין תרופות יכולות להיות מסוכנות

    /** רמת ביטחון מינימלית להתוויית נגד */
    public static final double MIN_CONTRAINDICATION_CONFIDENCE = 0.8;
    // סיבה: התוויות נגד חייבות להיות אמינות מאוד

    // =============== הגדרות זמן ופרפורמנס ===============

    /** זמן timeout לניתוח גרף מורכב (מילישניות) */
    public static final long COMPLEX_ANALYSIS_TIMEOUT = 60000L;
    // סיבה: דקה אחת מספיקה לניתוח מורכב, מעל זה יגרום לחוויית משתמש גרועה

    /** זמן timeout לחיפוש פשוט (מילישניות) */
    public static final long SIMPLE_SEARCH_TIMEOUT = 10000L;
    // סיבה: 10 שניות מספיקות לחיפוש פשוט

    /** מספר מקסימלי של workers לעיבוד מקבילי */
    public static final int MAX_PARALLEL_WORKERS = 4;
    // סיבה: מספיק עבור רוב המערכות מבלי לצרוך יותר מדי משאבים

    // =============== משקלים לסוגי קשרים רפואיים ===============

    /** משקל בסיסי לקשר טיפול */
    public static final double TREATMENT_RELATIONSHIP_WEIGHT = 0.9;
    // סיבה: קשרי טיפול הם חזקים וחשובים קלינית

    /** משקל בסיסי לקשר סימפטום-מחלה */
    public static final double SYMPTOM_DISEASE_WEIGHT = 0.8;
    // סיבה: קשר חזק אבל יכול להיות גם סימפטום של מחלות אחרות

    /** משקל בסיסי לקשר תופעת לוואי */
    public static final double SIDE_EFFECT_WEIGHT = 0.7;
    // סיבה: לא כל המטופלים יחוו תופעות לוואי

    /** משקל בסיסי לקשר גורם סיכון */
    public static final double RISK_FACTOR_WEIGHT = 0.6;
    // סיבה: גורמי סיכון מגבירים סיכויים אבל לא מבטיחים מחלה

    /** משקל בסיסי לקשר מניעה */
    public static final double PREVENTION_WEIGHT = 0.5;
    // סיבה: מניעה יעילה אבל לא תמיד מושלמת

    // =============== סוגי מקורות נתונים ומשקליהם ===============

    /** משקל למקור SNOMEDCT_US */
    public static final double SNOMED_WEIGHT_MULTIPLIER = 1.2;
    // סיבה: המקור הרפואי האמין ביותר

    /** משקל למקור RXNORM */
    public static final double RXNORM_WEIGHT_MULTIPLIER = 1.1;
    // סיבה: מקור מצוין למידע על תרופות

    /** משקל למקור MSH */
    public static final double MSH_WEIGHT_MULTIPLIER = 1.05;
    // סיבה: מקור טוב למונחים

    /** משקל למקור לא מוכר */
    public static final double UNKNOWN_SOURCE_WEIGHT = 0.8;
    // סיבה: מקור לא מוכר מקבל משקל נמוך יותר

    // =============== הגדרות למצב Demo ===============

    /** מספר מקסימלי של CUIs במצב Demo */
    public static final int MAX_DEMO_CUIS = 200;
    // סיבה: מספיק CUIs להדגמה מבלי להקטין ביצועים

    /** רמת ביטחון מינימלית למצב Demo */
    public static final double DEMO_MIN_CONFIDENCE = 0.3;
    // סיבה: רמה נמוכה יותר להדגמה טובה יותר

    /** מספר מקסימלי של קשרים להדגמה */
    public static final int MAX_DEMO_CONNECTIONS = 50;
    // סיבה: מספיק קשרים להדגמה מבלי להציף את המשתמש
}