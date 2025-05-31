package com.example.mediaid.bl;

import lombok.Data;

import java.util.Set;
@Data
public class DemoMode {

    // הפעלה/כיבוי של מצב דמו
    public static final boolean MODE = true;

    // CUIs למצב דמו - מתוקן עם CUIs שקיימים בפועל
    public static final Set<String> DEMO_CUIS = Set.of(
            // מחלות נפוצות
            "C0020538", // Hypertensive disease (יתר לחץ דם)
            "C0011849", // Diabetes mellitus (סוכרת)
            "C0018802", // Heart failure (אי ספיקת לב)
            "C0004096", // Asthma (אסתמה)
            "C0002962", // Angina pectoris (תעוקת חזה)
            "C0027051", // Myocardial infarction (התקף לב)
            "C0018799", // Heart disease (מחלת לב)
            "C0011570", // Depression (דיכאון)
            "C0003467", // Anxiety (חרדה)
            "C0002871", // Anemia (אנמיה)

            // סימפטומים נפוצים
            "C0008031", // Chest pain (כאב חזה)
            "C0013404", // Dyspnea (קוצר נשימה)
            "C0004093", // Asthenia (חולשה)
            "C0015967", // Fever (חום)
            "C0018681", // Headache (כאב ראש)
            "C0027497", // Nausea (בחילה)
            "C0042963", // Vomiting (הקאה)
            "C0012833", // Dizziness (סחרחורת)
            "C0030193", // Pain (כאב)
            "C0015672", // Fatigue (עייפות)

            // תרופות נפוצות
            "C0004147", // Aspirin (אספירין)
            "C0025598", // Metformin (מטפורמין)
            "C0699142", // Lisinopril (ליזינופריל)
            "C0065374", // Atorvastatin (אטורווסטטין)
            "C0699129", // Amlodipine (אמלודיפין)
            "C0700583", // Metoprolol (מטופרולול)
            "C0001927", // Albuterol (אלבוטרול)
            "C0017725", // Glucose (גלוקוז)
            "C0020740", // Insulin (אינסולין)
            "C0000970"  // Acetaminophen (אקטמינופן)
    );

    /**
     * בדיקה אם CUI כלול במצב דמו
     */
    public static boolean isRelevantForDemo(String cui) {
        if (!MODE) {
            return true; // אם מצב דמו כבוי, הכל רלוונטי
        }
        return DEMO_CUIS.contains(cui);
    }

    /**
     * בדיקה אם קשר רלוונטי למצב דמו
     */
    public static boolean isRelationshipRelevantForDemo(String cui1, String cui2) {
        if (!MODE) {
            return true;
        }
        return isRelevantForDemo(cui1) && isRelevantForDemo(cui2);
    }

    /**
     * קבלת סטטיסטיקות על מצב דמו
     */
    public static String getDemoStats() {
        if (!MODE) {
            return "Demo mode is OFF - all data will be processed";
        }
        return String.format("Demo mode is ON - filtering to %d CUIs", DEMO_CUIS.size());
    }
}
