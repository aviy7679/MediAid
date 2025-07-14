package com.example.mediaid.constants;

/**
 * קבועים הקשורים ל-API, endpoints, validation ו-HTTP responses
 */
public class ApiConstants {

    /** גודל עמוד מינימלי */
    public static final int MIN_PAGE_SIZE = 1;
    // סיבה: לפחות תוצאה אחת לעמוד

    // =============== הגדרות חיפוש ===============

    /** אורך מינימלי לחיפוש */
    public static final int MIN_SEARCH_LENGTH = 2;
    // סיבה: מתחת לזה החיפוש יחזיר יותר מדי תוצאות לא רלוונטיות

    /** אורך מקסימלי לחיפוש */
    public static final int MAX_SEARCH_LENGTH = 100;
    // סיבה: מספיק ארוך לחיפושים מורכבים אבל לא יגרום לבעיות

    /** מספר ברירת מחדל של תוצאות חיפוש */
    public static final int DEFAULT_SEARCH_LIMIT = 30;
    // סיבה: מספיק תוצאות למשתמש מבלי להציף אותו

    /** מספר מקסימלי של תוצאות חיפוש */
    public static final int MAX_SEARCH_RESULTS = 100;
    // סיבה: מספיק גם לצרכים מיוחדים

    /** קוד שגיאה לניתוח נתונים */
    public static final String DATA_ANALYSIS_ERROR = "DATA_ANALYSIS_ERROR";
    // סיבה: קוד ספציפי לבעיות בניתוח רפואי


    /** גובה מינימלי (ס"מ) */
    public static final int MIN_HEIGHT = 50;
    // סיבה: גובה מינימלי סביר

    /** גובה מקסימלי (ס"מ) */
    public static final int MAX_HEIGHT = 250;
    // סיבה: גובה מקסימלי סביר

    /** משקל מינימלי (ק"ג) */
    public static final int MIN_WEIGHT = 20;
    // סיבה: משקל מינימלי סביר

    /** משקל מקסימלי (ק"ג) */
    public static final int MAX_WEIGHT = 300;
    // סיבה: משקל מקסימלי סביר

    // =============== הגדרות מדדים רפואיים ===============

    /** רמת גלוקוז מינימלית (mg/dL) */
    public static final double MIN_BLOOD_GLUCOSE = 50;
    // סיבה: מתחת לזה היפוגליקמיה חמורה

    /** רמת גלוקוז מקסימלית (mg/dL) */
    public static final double MAX_BLOOD_GLUCOSE = 500;
    // סיבה: מעל זה היפרגליקמיה חמורה

    /** לחץ דם סיסטולי מינימלי (mmHg) */
    public static final int MIN_SYSTOLIC_BP = 70;
    // סיבה: מתחת לזה היפוטנזיה חמורה

    /** לחץ דם סיסטולי מקסימלי (mmHg) */
    public static final int MAX_SYSTOLIC_BP = 250;
    // סיבה: מעל זה היפרטנזיה חמורה

    /** לחץ דם דיאסטולי מינימלי (mmHg) */
    public static final int MIN_DIASTOLIC_BP = 40;
    // סיבה: מתחת לזה היפוטנזיה חמורה

    /** לחץ דם דיאסטולי מקסימלי (mmHg) */
    public static final int MAX_DIASTOLIC_BP = 150;
    // סיבה: מעל זה היפרטנזיה חמורה

    /** מספר שעות שינה מינימלי */
    public static final int MIN_SLEEP_HOURS = 3;
    // סיבה: פחות מזה לא תקין לטווח ארוך

    /** מספר שעות שינה מקסימלי */
    public static final int MAX_SLEEP_HOURS = 16;
    // סיבה: יותר מזה עלול להצביע על בעיה רפואית

    // =============== הגדרות UMLS ===============

    /** אורך מקסימלי של CUI */
    public static final int CUI_LENGTH = 8;
    // סיבה: אורך סטנדרטי של CUI ב-UMLS

    /** אורך מקסימלי לשם ישות רפואית */
    public static final int MAX_ENTITY_NAME_LENGTH = 250;
    // סיבה: מספיק לשמות רפואיים באנגלית

}