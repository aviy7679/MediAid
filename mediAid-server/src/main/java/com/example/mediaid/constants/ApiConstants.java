package com.example.mediaid.constants;

/**
 * קבועים הקשורים ל-API, endpoints, validation ו-HTTP responses
 */
public class ApiConstants {

    // =============== הגדרות Pagination ===============

    /** גודל עמוד ברירת מחדל */
    public static final int DEFAULT_PAGE_SIZE = 20;
    // סיבה: מספיק תוצאות לעמוד מבלי להאט את הטעינה

    /** גודל עמוד מקסימלי */
    public static final int MAX_PAGE_SIZE = 100;
    // סיבה: מספיק לצרכים מיוחדים אבל לא יגרום לבעיות ביצועים

    /** גודל עמוד מינימלי */
    public static final int MIN_PAGE_SIZE = 1;
    // סיבה: לפחות תוצאה אחת לעמוד

    /** מספר עמוד ברירת מחדל */
    public static final int DEFAULT_PAGE_NUMBER = 0;
    // סיבה: מתחילים מעמוד 0 כמו ברוב המערכות

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

    // =============== הגדרות Timeouts ===============

    /** timeout לשרת Python (מילישניות) */
    public static final int PYTHON_SERVER_TIMEOUT = 30000;
    // סיבה: ניתוח AI יכול לקחת זמן, אבל לא יותר מ-30 שניות

    /** timeout לבקשת HTTP רגילה (מילישניות) */
    public static final int HTTP_REQUEST_TIMEOUT = 5000;
    // סיבה: מספיק לבקשות רגילות

    /** timeout לבקשת upload (מילישניות) */
    public static final int UPLOAD_TIMEOUT = 60000;
    // סיבה: העלאת קבצים יכולה לקחת זמן, במיוחד תמונות

    /** timeout לניתוח מורכב (מילישניות) */
    public static final int COMPLEX_ANALYSIS_TIMEOUT = 120000;
    // סיבה: דקותיים לניתוח מורכב של גרף רפואי

    // =============== הגדרות קבצים ===============

    /** גודל מקסימלי של קובץ תמונה (בייטים) */
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    // סיבה: מספיק לתמונות באיכות גבוהה אבל לא יעמיס על השרת

    /** גודל מקסימלי של קובץ אודיו (בייטים) */
    public static final long MAX_AUDIO_SIZE = 50 * 1024 * 1024; // 50MB
    // סיבה: קבצי אודיו יכולים להיות גדולים יותר מתמונות

    /** גודל מקסימלי של טקסט (תווים) */
    public static final int MAX_TEXT_LENGTH = 5000;
    // סיבה: מספיק לתיאור מפורט של סימפטומים

    /** גודל מקסימלי של הערות (תווים) */
    public static final int MAX_NOTES_LENGTH = 1000;
    // סיבה: מספיק להערות מפורטות אבל לא יותר מדי

    // =============== הגדרות Headers ===============

    /** שם Header לזיהוי משתמש */
    public static final String USER_ID_HEADER = "X-User-ID";
    // סיבה: שם מותאם אישית לזיהוי משתמש

    /** שם Header לזיהוי Session */
    public static final String SESSION_ID_HEADER = "X-Session-ID";
    // סיבה: שם מותאם אישית לזיהוי session

    /** שם Header לזיהוי גרסת API */
    public static final String API_VERSION_HEADER = "X-API-Version";
    // סיבה: חשוב לזיהוי גרסה לתמיכה לאחור

    /** שם Header לזיהוי platform */
    public static final String PLATFORM_HEADER = "X-Platform";
    // סיבה: חשוב לסטטיסטיקות ותמיכה

    // =============== הגדרות Response Messages ===============

    /** הודעת הצלחה כללית */
    public static final String SUCCESS_MESSAGE = "Operation completed successfully";
    // סיבה: הודעה כללית להצלחה

    /** הודעת שגיאה כללית */
    public static final String ERROR_MESSAGE = "An error occurred while processing your request";
    // סיבה: הודעה כללית לשגיאה

    /** הודעת נתונים לא נמצאו */
    public static final String NOT_FOUND_MESSAGE = "Requested resource not found";
    // סיבה: הודעה סטנדרטית ל-404

    /** הודעת אימות נכשל */
    public static final String AUTHENTICATION_FAILED_MESSAGE = "Authentication failed";
    // סיבה: הודעה סטנדרטית לכשל אימות

    /** הודעת הרשאה נכשלה */
    public static final String AUTHORIZATION_FAILED_MESSAGE = "Insufficient permissions";
    // סיבה: הודעה סטנדרטית לכשל הרשאה

    // =============== הגדרות קודי שגיאה ===============

    /** קוד שגיאה לנתונים לא תקינים */
    public static final String INVALID_DATA_ERROR = "INVALID_DATA";
    // סיבה: קוד מובן לשגיאות validation

    /** קוד שגיאה לנתונים חסרים */
    public static final String MISSING_DATA_ERROR = "MISSING_DATA";
    // סיבה: קוד מובן לשדות חסרים

    /** קוד שגיאה לפעולה לא מורשית */
    public static final String UNAUTHORIZED_ERROR = "UNAUTHORIZED";
    // סיבה: קוד מובן לכשל הרשאה

    /** קוד שגיאה לשרת Python */
    public static final String PYTHON_SERVER_ERROR = "PYTHON_SERVER_ERROR";
    // סיבה: קוד ספציפי לבעיות עם שרת הניתוח

    /** קוד שגיאה לניתוח נתונים */
    public static final String DATA_ANALYSIS_ERROR = "DATA_ANALYSIS_ERROR";
    // סיבה: קוד ספציפי לבעיות בניתוח רפואי

    // =============== הגדרות Content Types ===============

    /** Content Type לJSON */
    public static final String JSON_CONTENT_TYPE = "application/json";
    // סיבה: סוג תוכן סטנדרטי לAPI

    /** Content Type לתמונה */
    public static final String IMAGE_CONTENT_TYPE = "image/*";
    // סיבה: מקבל כל סוג תמונה

    /** Content Type לאודיו */
    public static final String AUDIO_CONTENT_TYPE = "audio/*";
    // סיבה: מקבל כל סוג אודיו

    /** Content Type לטקסט */
    public static final String TEXT_CONTENT_TYPE = "text/plain";
    // סיבה: סוג תוכן לטקסט רגיל

    // =============== הגדרות CORS ===============

    /** רשימת Origins מותרים */
    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080"
    };
    // סיבה: Origins נפוצים לפיתוח

    /** רשימת Methods מותרים */
    public static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };
    // סיבה: Methods רגילים לREST API

    /** רשימת Headers מותרים */
    public static final String[] ALLOWED_HEADERS = {
            "Authorization", "Content-Type", "X-Requested-With",
            "X-User-ID", "X-Session-ID", "X-API-Version"
    };
    // סיבה: Headers נפוצים לשימוש

    // =============== הגדרות Rate Limiting ===============

    /** מספר מקסימלי של בקשות לדקה */
    public static final int MAX_REQUESTS_PER_MINUTE = 100;
    // סיבה: מספיק לשימוש רגיל אבל מגביל דואמים

    /** מספר מקסימלי של בקשות upload לדקה */
    public static final int MAX_UPLOADS_PER_MINUTE = 10;
    // סיבה: uploads צורכים יותר משאבים

    /** מספר מקסימלי של בקשות ניתוח לדקה */
    public static final int MAX_ANALYSIS_REQUESTS_PER_MINUTE = 20;
    // סיבה: ניתוח צורך יותר משאבים

    /** זמן חסימה לאחר חריגה מגבולות (מילישניות) */
    public static final long RATE_LIMIT_BLOCK_DURATION = 60000L;
    // סיבה: דקה אחת של חסימה

    // =============== הגדרות Caching ===============

    /** זמן cache לתוצאות חיפוש (שניות) */
    public static final int SEARCH_CACHE_DURATION = 300;
    // סיבה: 5 דקות - מספיק לשפר ביצועים אבל לא יותר מדי

    /** זמן cache לנתונים סטטיים (שניות) */
    public static final int STATIC_DATA_CACHE_DURATION = 3600;
    // סיבה: שעה אחת - נתונים סטטיים לא משתנים הרבה

    /** זמן cache לפרופיל משתמש (שניות) */
    public static final int USER_PROFILE_CACHE_DURATION = 1800;
    // סיבה: 30 דקות - איזון בין ביצועים לעדכונים

    // =============== הגדרות Validation ===============

    /** אורך מינימלי לשם משתמש */
    public static final int MIN_USERNAME_LENGTH = 3;
    // סיבה: מינימום סביר לשם משתמש

    /** אורך מקסימלי לשם משתמש */
    public static final int MAX_USERNAME_LENGTH = 50;
    // סיבה: מספיק לשמות מורכבים

    /** אורך מינימלי לאימייל */
    public static final int MIN_EMAIL_LENGTH = 5;
    // סיבה: מינימום לאימייל תקני (a@b.c)

    /** אורך מקסימלי לאימייל */
    public static final int MAX_EMAIL_LENGTH = 100;
    // סיבה: מספיק לאימיילים מורכבים

    /** גיל מינימלי למשתמש */
    public static final int MIN_USER_AGE = 13;
    // סיבה: דרישות COPPA

    /** גיל מקסימלי למשתמש */
    public static final int MAX_USER_AGE = 120;
    // סיבה: גיל מקסימלי סביר

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
}