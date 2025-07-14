package com.example.mediaid.constants;

/**
 * קבועים הקשורים לניתוח רפואי, גרף רפואי וחישוב סיכונים
 */
public class MedicalAnalysisConstants {

    // =============== הגדרות המרת יחידות ===============

    /** המרת סנטימטרים למטרים */
    public static final double CENTIMETERS_TO_METERS = 100.0;
    // סיבה: המרה סטנדרטית מסנטימטרים למטרים לחישוב BMI

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

    /** גורם דעיכה לביטחון במסלול */
    public static final double CONFIDENCE_DECAY_FACTOR = 0.9;
    // סיבה: ביטחון יורד עם המרחק אבל לאט יותר מסיכון

    /** משקל מינימלי לגורם סיכון */
    public static final double MIN_RISK_FACTOR_WEIGHT = 0.1;
    // סיבה: מתחת לזה גורם הסיכון לא משמעותי קלינית

    /** משקל מקסימלי לגורם סיכון */
    public static final double MAX_RISK_FACTOR_WEIGHT = 0.99;
    // סיבה: לא 1.0 כי זה יצור בעיות בחישובים לוגריתמיים

    /** רמת סיכון מינימלית לפרופגציה */
    public static final double MIN_RISK_PROPAGATION = 0.05;
    // סיבה: מתחת לזה הסיכון זניח מבחינה קלינית

    // =============== רמות חומרה וציוני סיכון ===============

    /** ציון סיכון למחלה חמורה - מבוסס על סטנדרט WHO */
    public static final double SEVERE_DISEASE_RISK = 0.9;

    /** ציון סיכון למחלה בינונית - מבוסס על סטנדרט WHO */
    public static final double MODERATE_DISEASE_RISK = 0.6;

    /** ציון סיכון למחלה קלה - מבוסס על סטנדרט WHO */
    public static final double MILD_DISEASE_RISK = 0.3;

    /** ציון סיכון ברירת מחדל למחלה לא ידועה */
    public static final double DEFAULT_DISEASE_RISK = 0.5;

    /** ציון סיכון ברירת מחדל לגורם סיכון */
    public static final double DEFAULT_RISK_FACTOR = 0.4;

    /** ציון סיכון ברירת מחדל לישות רפואית כללית */
    public static final double DEFAULT_ENTITY_RISK = 0.3;

    // =============== רמות חומרה (strings) ===============

    /** רמת חומרה: חמורה */
    public static final String SEVERITY_SEVERE = "severe";

    /** רמת חומרה: בינונית */
    public static final String SEVERITY_MODERATE = "moderate";

    /** רמת חומרה: קלה */
    public static final String SEVERITY_MILD = "mild";

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

    /** בסיס לחישוב cohesion score - מנורמל לפי גודל קהילה */
    public static final double COHESION_BASE_NORMALIZER = 10.0;
    // סיבה: נורמליזציה של cohesion score לפי גודל הקהילה

    /** בונוס לגיוון סוגי ישויות בקהילה - מעודד רב-תחומיות */
    public static final double DIVERSITY_BONUS = 0.2;
    // סיבה: קהילות מגוונות חשובות יותר רפואית

    /** ציון cohesion מקסימלי */
    public static final double MAX_COHESION_SCORE = 1.0;

    /** ציון cohesion ברירת מחדל לקהילה fallback */
    public static final double FALLBACK_COHESION_SCORE = 0.5;

    // =============== הגדרות מרכזיות רפואית (Medical Hubs) ===============


    /** ציון מרכזיות בינוני */
    public static final double MEDIUM_CENTRALITY_THRESHOLD = 5.0;
    // סיבה: top 20% של הצמתים

    /** ציון מרכזיות גבוה (להגדרה כ-"השפעה גבוהה") */
    public static final double HIGH_CENTRALITY_THRESHOLD = 20.0;
    // סיבה: כפל מהמינימום = השפעה גבוהה באמת, top 5% של הצמתים

    /** ציון מרכזיות גבוה מאוד */
    public static final double VERY_HIGH_CENTRALITY_THRESHOLD = 50.0;
    // סיבה: רק ישויות מרכזיות מאוד יגיעו לכאן, top 1% של הצמתים

    // =============== רמות השפעה (strings) ===============

    /** רמת השפעה: גבוהה מאוד */
    public static final String INFLUENCE_VERY_HIGH = "Very High";

    /** רמת השפעה: גבוהה */
    public static final String INFLUENCE_HIGH = "High";

    /** רמת השפעה: בינונית */
    public static final String INFLUENCE_MEDIUM = "Medium";

    /** רמת השפעה: נמוכה */
    public static final String INFLUENCE_LOW = "Low";


    /** סף לרמת דחיפות בינונית */
    public static final double MEDIUM_URGENCY_THRESHOLD = 0.5;
    // סיבה: נקודת אמצע סבירה לדחיפות בינונית

    /** סף לרמת דחיפות גבוהה */
    public static final double HIGH_URGENCY_THRESHOLD = 0.8;
    // סיבה: מעל זה המצב דורש תשומת לב מיידית

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

    /** מספר מקסימלי של קשרים לישות אחת */
    public static final int MAX_CONNECTIONS_PER_ENTITY = 100;
    // סיבה: מספיק קשרים לניתוח מבלי להציף את המערכת


    // =============== סוגי ישויות רפואיות ===============

    /** סוג ישות: מחלה */
    public static final String ENTITY_TYPE_DISEASE = "disease";

    /** סוג ישות: גורם סיכון */
    public static final String ENTITY_TYPE_RISK_FACTOR = "riskfactor";

    /** סוג ישות: תרופה */
    public static final String ENTITY_TYPE_MEDICATION = "medication";

    /** סוג ישות: סימפטום */
    public static final String ENTITY_TYPE_SYMPTOM = "symptom";

    // =============== סוגי קשרים רפואיים ===============

    /** סוג קשר: תופעת לוואי */
    public static final String CONNECTION_SIDE_EFFECT = "SIDE_EFFECT";

    /** סוג קשר: מחלה-סימפטום */
    public static final String CONNECTION_DISEASE_SYMPTOM = "DISEASE_SYMPTOM";

    /** סוג קשר: טיפול */
    public static final String CONNECTION_TREATMENT = "TREATMENT";

    // =============== ערכי fallback ===============

    /** ערך fallback לשם ישות */
    public static final String FALLBACK_ENTITY_NAME = "Unknown Entity";

    /** ערך fallback לסוג קהילה */
    public static final String FALLBACK_COMMUNITY_TYPE = "Mixed";

    /** תיאור fallback לקהילה */
    public static final String FALLBACK_COMMUNITY_DESCRIPTION = "User medical profile community (fallback)";

    // =============== הגדרות למצב Demo ===============

    /** מספר מקסימלי של CUIs במצב Demo */
    public static final int MAX_DEMO_CUIS = 200;
    // סיבה: מספיק CUIs להדגמה מבלי להקטין ביצועים
    // =============== פונקציות עזר ===============

    /**
     * מחזיר ציון סיכון לפי רמת חומרה
     * @param severity רמת החומרה
     * @return ציון הסיכון
     */
    public static double getRiskBySeverity(String severity) {
        if (severity == null) return DEFAULT_DISEASE_RISK;

        return switch (severity.toLowerCase()) {
            case SEVERITY_SEVERE -> SEVERE_DISEASE_RISK;
            case SEVERITY_MODERATE -> MODERATE_DISEASE_RISK;
            case SEVERITY_MILD -> MILD_DISEASE_RISK;
            default -> DEFAULT_DISEASE_RISK;
        };
    }

    /**
     * מחזיר רמת השפעה לפי ציון מרכזיות
     * @param centralityScore ציון המרכזיות
     * @return רמת ההשפעה
     */
    public static String getInfluenceLevel(double centralityScore) {
        if (centralityScore > VERY_HIGH_CENTRALITY_THRESHOLD) return INFLUENCE_VERY_HIGH;
        if (centralityScore > HIGH_CENTRALITY_THRESHOLD) return INFLUENCE_HIGH;
        if (centralityScore > MEDIUM_CENTRALITY_THRESHOLD) return INFLUENCE_MEDIUM;
        return INFLUENCE_LOW;
    }

    /**
     * חישוב decay factor לפי אורך מסלול
     * @param pathLength אורך המסלול
     * @return decay factor
     */
    public static double calculateDecayFactor(int pathLength) {
        return Math.pow(RISK_DECAY_FACTOR, pathLength);
    }

    /**
     * חישוב confidence לפי risk score ואורך מסלול
     * @param riskScore ציון הסיכון
     * @param pathLength אורך המסלול
     * @return ציון הביטחון
     */
    public static double calculatePathwayConfidence(double riskScore, int pathLength) {
        return riskScore * Math.pow(CONFIDENCE_DECAY_FACTOR, pathLength - 1);
    }

    /**
     * חישוב cohesion score לקהילה
     * @param communitySize גודל הקהילה
     * @param hasDiversity האם יש גיוון בסוגי ישויות
     * @return ציון cohesion
     */
    public static double calculateCohesionScore(int communitySize, boolean hasDiversity) {
        double baseScore = Math.min(MAX_COHESION_SCORE, communitySize / COHESION_BASE_NORMALIZER);
        double diversityBonus = hasDiversity ? DIVERSITY_BONUS : 0.0;
        return Math.min(MAX_COHESION_SCORE, baseScore + diversityBonus);
    }

    // Constructor פרטי למניעת יצירת אובייקטים
    private MedicalAnalysisConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}