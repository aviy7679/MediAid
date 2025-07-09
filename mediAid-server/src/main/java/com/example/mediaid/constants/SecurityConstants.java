package com.example.mediaid.constants;

/**
 * קבועים הקשורים לאבטחה, הצפנה ו-JWT
 */
public class SecurityConstants {

    // =============== הגדרות הצפנה (Encryption) ===============

    /** סוג אלגוריתם הצפנה */
    public static final String KEY_ALGORITHM = "AES";
    // סיבה: AES הוא התקן הזהב להצפנה סימטרית

    /** גודל מפתח הצפנה בביטים */
    public static final int KEY_SIZE = 256;
    // סיבה: AES-256 נחשב לבטוח מספיק גם לנתונים רגישים

    /** סוג Keystore */
    public static final String KEY_STORE_TYPE = "PKCS12";
    // סיבה: PKCS12 הוא התקן מודרני ובטוח יותר מ-JKS

    /** אלגוריתם הצפנה מלא */
    public static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    // סיבה: GCM מספק גם הצפנה וגם אימות, NoPadding בטוח יותר

    /** גודל IV (Initialization Vector) */
    public static final int IV_SIZE = 12;
    // סיבה: גודל תקני עבור GCM mode

    /** גודל GCM Tag */
    public static final int GCM_TAG_SIZE = 128;
    // סיבה: גודל תקני המספק אבטחה מספקת

    // =============== הגדרות JWT ===============

    /** זמן תפוגה של JWT (מילישניות) */
    public static final long JWT_EXPIRATION_TIME = 86400000L;
    // סיבה: 24 שעות = זמן סביר לתוקף טוקן מבלי לחייב התחברות תכופה

    /** זמן תפוגה של Refresh Token (מילישניות) */
    public static final long REFRESH_TOKEN_EXPIRATION = 604800000L;
    // סיבה: 7 ימים = זמן סביר לרענון מבלי לחייב התחברות חוזרת

    /** אורך מינימלי של סיסמה */
    public static final int MIN_PASSWORD_LENGTH = 8;
    // סיבה: אורך מינימלי המומלץ לאבטחה בסיסית

    /** מספר מקסימלי של ניסיונות התחברות */
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    // סיבה: מספיק ניסיונות למשתמש לגיטימי, אבל לא יותר מדי לתוקף

    /** זמן חסימה לאחר ניסיונות התחברות כושלים (מילישניות) */
    public static final long LOCKOUT_DURATION = 900000L;
    // סיבה: 15 דקות = זמן מספיק להרתיע תוקף אבל לא יותר מדי למשתמש

    // =============== הגדרות BCrypt ===============

    /** חוזק BCrypt */
    public static final int BCRYPT_STRENGTH = 12;
    // סיבה: 12 מספק איזון בין אבטחה לביצועים (2^12 = 4096 איטרציות)

    // =============== הגדרות Session ===============

    /** זמן תפוגה של Session (מילישניות) */
    public static final long SESSION_TIMEOUT = 1800000L;
    // סיבה: 30 דקות = זמן סביר לחוסר פעילות

    /** זמן תפוגה של Remember Me (מילישניות) */
    public static final long REMEMBER_ME_EXPIRATION = 2592000000L;
    // סיבה: 30 ימים = זמן סביר לזכירת משתמש

    // =============== הגדרות CORS ===============

    /** זמן Cache של CORS preflight (שניות) */
    public static final long CORS_MAX_AGE = 3600L;
    // סיבה: שעה אחת = זמן מספיק לשמירת preflight מבלי לפגוע באבטחה

    /** רשימת Origins מותרים ל-CORS */
    public static final String[] CORS_ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://localhost:3000"
    };
    // סיבה: כתובות הפיתוח הנפוצות לפרונט-אנד (Vite ו-React)

    /** רשימת Methods מותרים ל-CORS */
    public static final String[] CORS_ALLOWED_METHODS = {
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };
    // סיבה: כל ה-HTTP methods הנחוצים לREST API

    /** רשימת Headers מותרים ל-CORS */
    public static final String[] CORS_ALLOWED_HEADERS = {
            "Authorization", "Content-Type", "X-Requested-With"
    };
    // סיבה: Headers הבסיסיים הנחוצים לפעולת האפליקציה

    // =============== הגדרות Rate Limiting ===============

    /** מספר מקסימלי של בקשות לדקה למשתמש */
    public static final int MAX_REQUESTS_PER_MINUTE = 100;
    // סיבה: מספיק לשימוש רגיל, אבל מגביל בוטים

    /** מספר מקסימלי של בקשות API לשעה */
    public static final int MAX_API_REQUESTS_PER_HOUR = 1000;
    // סיבה: מספיק לאפליקציה רגילה, מגביל שימוש לרעה

    // =============== הגדרות הצפנת נתונים ===============

    /** אורך מקסימלי של מחרוזת לפני הצפנה */
    public static final int MAX_ENCRYPT_STRING_LENGTH = 1000;
    // סיבה: הגבלה סבירה שמונעת הצפנה של נתונים גדולים מדי

    /** זמן תפוגה של מפתח הצפנה (מילישניות) */
    public static final long ENCRYPTION_KEY_EXPIRATION = 31536000000L;
    // סיבה: שנה אחת = זמן סביר לחידוש מפתח הצפנה

    // =============== הגדרות רגקס לוולידציה ===============

    /** רגקס לוולידציה של אימייל */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    // סיבה: רגקס תקני לוולידציה בסיסית של אימייל

    /** רגקס לוולידציה של סיסמה חזקה */
    public static final String STRONG_PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    // סיבה: לפחות 8 תווים עם אותיות קטנות, גדולות, ספרות ותווים מיוחדים

    // =============== הגדרות Headers ===============

    /** שם Header לטוקן JWT */
    public static final String JWT_HEADER_NAME = "Authorization";
    // סיבה: שם תקני לheader של authentication

    /** Prefix לטוקן JWT */
    public static final String JWT_TOKEN_PREFIX = "Bearer ";
    // סיבה: פרפיקס תקני לטוקן Bearer

    /** אורך Prefix של JWT */
    public static final int JWT_PREFIX_LENGTH = 7;
    // סיבה: אורך המחרוזת "Bearer " כדי לחתוך אותה

    /** תו מסיכה לנתונים רגישים */
    public static final char MASK_CHARACTER = '*';
    // סיבה: תו סטנדרטי למסיכה

    /** מספר תווים לא מוסתרים בתחילת מחרוזת */
    public static final int UNMASK_PREFIX_LENGTH = 3;
    // סיבה: מספיק כדי לזהות אבל לא לחשוף

    /** מספר תווים לא מוסתרים בסוף מחרוזת */
    public static final int UNMASK_SUFFIX_LENGTH = 3;
    // סיבה: מספיק כדי לזהות אבל לא לחשוף

    // =============== הגדרות Endpoints מותרים ללא אימות ===============

    /** רשימת endpoints שמותרים ללא אימות */
    public static final String[] PUBLIC_ENDPOINTS = {
            "api/user/logIn",
            "/api/user/create-account",
            "/error",
            "/api/auth/**",
            "/api/public/**",
            "/api/admin/**",
            "/api/medications/search",
            "/api/diseases/search"
    };
    // סיבה: endpoints שצריכים להיות פתוחים לגישה ללא אימות
}