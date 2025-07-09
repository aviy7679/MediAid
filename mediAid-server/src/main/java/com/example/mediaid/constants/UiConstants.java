package com.example.mediaid.constants;

/**
 * קבועים הקשורים לממשק משתמש, הודעות ותצוגה
 */
public class UiConstants {

    // =============== הגדרות תצוגה ===============

    /** מספר מקסימלי של פריטים לתצוגה בדף */
    public static final int MAX_ITEMS_PER_PAGE = 50;
    // סיבה: מספיק פריטים לתצוגה נוחה מבלי להאט את הטעינה

    /** מספר ברירת מחדל של פריטים לתצוגה */
    public static final int DEFAULT_ITEMS_PER_PAGE = 10;
    // סיבה: מספר נוח לתצוגה רגילה

    /** מספר מקסימלי של תווים לתצוגה קצרה */
    public static final int MAX_SHORT_TEXT_LENGTH = 100;
    // סיבה: מספיק תווים לתצוגה מקוצרת

    /** מספר מקסימלי של תווים לתצוגה בינונית */
    public static final int MAX_MEDIUM_TEXT_LENGTH = 250;
    // סיבה: מספיק תווים לתיאור קצר

    /** מספר מקסימלי של תווים לתצוגה ארוכה */
    public static final int MAX_LONG_TEXT_LENGTH = 500;
    // סיבה: מספיק תווים לתיאור מפורט

    // =============== הגדרות צבעים ויזואליים ===============

    /** צבע לרמת סיכון נמוכה */
    public static final String LOW_RISK_COLOR = "#4CAF50";
    // סיבה: ירוק = בטוח, צבע מוכר למשתמשים

    /** צבע לרמת סיכון בינונית */
    public static final String MEDIUM_RISK_COLOR = "#FF9800";
    // סיבה: כתום = זהירות, צבע מוכר למשתמשים

    /** צבע לרמת סיכון גבוהה */
    public static final String HIGH_RISK_COLOR = "#F44336";
    // סיבה: אדום = סכנה, צבע מוכר למשתמשים

    /** צבע לרמת סיכון קריטית */
    public static final String CRITICAL_RISK_COLOR = "#8B0000";
    // סיבה: אדום כהה = מצב קריטי

    /** צבע ברירת מחדל */
    public static final String DEFAULT_COLOR = "#757575";
    // סיבה: אפור = ניטרלי

    // =============== הגדרות אייקונים ===============

    /** אייקון לרמת דחיפות נמוכה */
    public static final String LOW_URGENCY_ICON = "ℹ️";

    /** אייקון לרמת דחיפות בינונית */
    public static final String MEDIUM_URGENCY_ICON = "⚠️";

    /** אייקון לרמת דחיפות גבוהה */
    public static final String HIGH_URGENCY_ICON = "🚨";

    /** אייקון לחירום */
    public static final String EMERGENCY_ICON = "🆘";

    /** אייקון להצלחה */
    public static final String SUCCESS_ICON = "✅";

    /** אייקון לשגיאה */
    public static final String ERROR_ICON = "❌";

    // =============== הגדרות הודעות משתמש ===============

    /** הודעת טעינה */
    public static final String LOADING_MESSAGE = "מעבד את המידע שלך...";
    // סיבה: הודעה בעברית מתאימה לקהל היעד

    /** הודעת שמירה */
    public static final String SAVING_MESSAGE = "שומר נתונים...";
    // סיבה: הודעה מובנת למשתמש

    /** הודעת עדכון */
    public static final String UPDATING_MESSAGE = "מעדכן נתונים...";
    // סיבה: הודעה מובנת למשתמש

    /** הודעת מחיקה */
    public static final String DELETING_MESSAGE = "מוחק נתונים...";
    // סיבה: הודעה מובנת למשתמש

    /** הודעת הצלחה בשמירה */
    public static final String SAVE_SUCCESS_MESSAGE = "הנתונים נשמרו בהצלחה";
    // סיבה: הודעת הצלחה מובנת

    /** הודעת שגיאה בשמירה */
    public static final String SAVE_ERROR_MESSAGE = "אירעה שגיאה בשמירת הנתונים";
    // סיבה: הודעת שגיאה מובנת

    // =============== הגדרות animations ו-transitions ===============

    /** זמן אנימציה קצר (מילישניות) */
    public static final int SHORT_ANIMATION_DURATION = 150;
    // סיבה: מספיק קצר שלא יגרום לתחושת איטיות

    /** זמן אנימציה בינוני (מילישניות) */
    public static final int MEDIUM_ANIMATION_DURATION = 300;
    // סיבה: זמן סטנדרטי לאנימציות UI

    /** זמן אנימציה ארוך (מילישניות) */
    public static final int LONG_ANIMATION_DURATION = 500;
    // סיבה: מספיק ארוך לאנימציות מורכבות

    /** זמן fade in/out (מילישניות) */
    public static final int FADE_DURATION = 200;
    // סיבה: זמן סטנדרטי לhide/show

    // =============== הגדרות מרווחים ================================

    /** מרווח קטן בין אלמנטים (פיקסלים) */
    public static final int SMALL_SPACING = 8;
    // סיבה: מרווח בסיסי בין אלמנטים

    /** מרווח בינוני בין אלמנטים (פיקסלים) */
    public static final int MEDIUM_SPACING = 16;
    // סיבה: מרווח סטנדרטי בין קבוצות

    /** מרווח גדול בין אלמנטים (פיקסלים) */
    public static final int LARGE_SPACING = 24;
    // סיבה: מרווח לחלוקה ברורה

    /** מרווח גדול מאוד (פיקסלים) */
    public static final int EXTRA_LARGE_SPACING = 32;
    // סיבה: מרווח לחלוקה משמעותית

    // =============== הגדרות גופנים ===============

    /** גודל טקסט קטן */
    public static final int SMALL_TEXT_SIZE = 12;
    // סיבה: גודל לטקסט משני

    /** גודל טקסט רגיל */
    public static final int REGULAR_TEXT_SIZE = 14;
    // סיבה: גודל סטנדרטי לטקסט

    /** גודל טקסט בינוני */
    public static final int MEDIUM_TEXT_SIZE = 16;
    // סיבה: גודל לטקסט חשוב

    /** גודל טקסט גדול */
    public static final int LARGE_TEXT_SIZE = 18;
    // סיבה: גודל לכותרות משנה

    /** גודל טקסט כותרת */
    public static final int HEADER_TEXT_SIZE = 24;
    // סיבה: גודל לכותרות ראשיות

    // =============== הגדרות כפתורים ===============

    /** גובה כפתור רגיל (פיקסלים) */
    public static final int BUTTON_HEIGHT = 40;
    // סיבה: גובה נוח למגע בנייד וקליק בדסקטופ

    /** גובה כפתור קטן (פיקסלים) */
    public static final int SMALL_BUTTON_HEIGHT = 32;
    // סיבה: גובה לכפתורים משניים

    /** גובה כפתור גדול (פיקסלים) */
    public static final int LARGE_BUTTON_HEIGHT = 48;
    // סיבה: גובה לכפתורים ראשיים

    /** רוחב כפתור מינימלי (פיקסלים) */
    public static final int MIN_BUTTON_WIDTH = 80;
    // סיבה: רוחב מינימלי לכפתור קריא

    // =============== הגדרות שדות קלט ===============

    /** גובה שדה קלט רגיל (פיקסלים) */
    public static final int INPUT_HEIGHT = 40;
    // סיבה: גובה נוח להקלדה

    /** גובה שדה קלט רב-שורות (פיקסלים) */
    public static final int TEXTAREA_HEIGHT = 80;
    // סיבה: גובה נוח לטקסט רב-שורות

    /** רוחב שדה קלט מינימלי (פיקסלים) */
    public static final int MIN_INPUT_WIDTH = 200;
    // סיבה: רוחב מינימלי לשדה קלט שימושי

    // =============== הגדרות טבלאות ===============

    /** גובה שורה בטבלה (פיקסלים) */
    public static final int TABLE_ROW_HEIGHT = 48;
    // סיבה: גובה נוח לקריאה

    /** גובה כותרת טבלה (פיקסלים) */
    public static final int TABLE_HEADER_HEIGHT = 56;
    // סיבה: גובה מעט גדול יותר לכותרת

    /** רוחב עמודה מינימלי (פיקסלים) */
    public static final int MIN_COLUMN_WIDTH = 100;
    // סיבה: רוחב מינימלי לעמודה שימושית

    // =============== הגדרות מודאלים ופופאפים ===============

    /** רוחב מודאל רגיל (פיקסלים) */
    public static final int MODAL_WIDTH = 600;
    // סיבה: רוחב נוח לרוב המודאלים

    /** רוחב מודאל קטן (פיקסלים) */
    public static final int SMALL_MODAL_WIDTH = 400;
    // סיבה: רוחב למודאלים פשוטים

    /** רוחב מודאל גדול (פיקסלים) */
    public static final int LARGE_MODAL_WIDTH = 800;
    // סיבה: רוחב למודאלים מורכבים

    /** גובה מודאל מקסימלי (פיקסלים) */
    public static final int MAX_MODAL_HEIGHT = 600;
    // סיבה: גובה מקסימלי שלא יחרוג מהמסך

    // =============== הגדרות רספונסיביות ===============

    /** רוחב breakpoint למובייל (פיקסלים) */
    public static final int MOBILE_BREAKPOINT = 768;
    // סיבה: breakpoint סטנדרטי למובייל

    /** רוחב breakpoint לטאבלט (פיקסלים) */
    public static final int TABLET_BREAKPOINT = 1024;
    // סיבה: breakpoint סטנדרטי לטאבלט

    /** רוחב breakpoint לדסקטופ (פיקסלים) */
    public static final int DESKTOP_BREAKPOINT = 1200;
    // סיבה: breakpoint סטנדרטי לדסקטופ

    // =============== הגדרות נגישות ===============

    /** קונטראסט מינימלי לטקסט */
    public static final double MIN_TEXT_CONTRAST = 4.5;
    // סיבה: דרישת WCAG לנגישות

    /** קונטראסט מינימלי לטקסט גדול */
    public static final double MIN_LARGE_TEXT_CONTRAST = 3.0;
    // סיבה: דרישת WCAG לטקסט גדול

    /** זמן המתנה לhover (מילישניות) */
    public static final int HOVER_DELAY = 300;
    // סיבה: זמן המתנה לפני הצגת tooltip

    /** זמן המתנה לfocus (מילישניות) */
    public static final int FOCUS_DELAY = 100;
    // סיבה: זמן המתנה לפני הצגת אלמנט focus

    // =============== הגדרות התראות ואימותים ===============

    /** זמן הצגת התראה (מילישניות) */
    public static final int ALERT_DISPLAY_DURATION = 5000;
    // סיבה: 5 שניות מספיקות לקריאת התראה

    /** זמן הצגת התראת שגיאה (מילישניות) */
    public static final int ERROR_ALERT_DURATION = 8000;
    // סיבה: שגיאות צריכות זמן יותר לקריאה

    /** זמן הצגת התראת הצלחה (מילישניות) */
    public static final int SUCCESS_ALERT_DURATION = 3000;
    // סיבה: הצלחה צריכה זמן קצר יותר

    /** מספר מקסימלי של התראות בו-זמנית */
    public static final int MAX_CONCURRENT_ALERTS = 5;
    // סיבה: מספיק התראות מבלי להציף את המסך

    // =============== הגדרות חיפוש ואוטו-קומפליט ===============

    /** זמן debounce לחיפוש (מילישניות) */
    public static final int SEARCH_DEBOUNCE_DELAY = 300;
    // סיבה: זמן המתנה לפני ביצוע חיפוש

    /** מספר מקסימלי של הצעות באוטו-קומפליט */
    public static final int MAX_AUTOCOMPLETE_SUGGESTIONS = 10;
    // סיבה: מספיק הצעות מבלי להציף את המשתמש

    /** מספר מינימלי של תווים לאוטו-קומפליט */
    public static final int MIN_AUTOCOMPLETE_CHARS = 2;
    // סיבה: מספיק תווים לקבלת הצעות רלוונטיות
}