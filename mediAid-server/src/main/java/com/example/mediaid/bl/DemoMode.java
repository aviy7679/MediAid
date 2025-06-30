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
            "C4014808", // Hypertensive disease (יתר לחץ דם)
            "C4013416", // Diabetes mellitus (סוכרת)
            "C4227568", // Heart failure (אי ספיקת לב)
            "C0004096", // Asthma (אסתמה)
            "C0002962", // Angina pectoris (תעוקת חזה)
            "C3280935", // Myocardial infarction (התקף לב)
            "C0854103", // Heart disease (מחלת לב)
//            "C3277399", // Depression (דיכאון)
//            "C4228281", // Anxiety (חרדה)
            "C4229468", // Anemia (אנמיה)
            "C0002395", // Alzheimer's disease (מחלת אלצהיימר)
            "C3553282", // Irritable bowel syndrome (תסמונת המעי הרגיז)
            "C4229251", // Gastroesophageal reflux disease (ריפלוקס / GERD)
            "C0020473", // Hyperlipidemia (שומנים גבוהים בדם)
            "C3551790", // Migraine disorders (מיגרנה)
            "C0021400", // Influenza (שפעת)
            "C0024117", // Chronic obstructive pulmonary disease (COPD - מחלת ריאות חסימתית כרונית)
            "C0026769", // Multiple sclerosis (טרשת נפוצה)
            "C0003873", // Rheumatoid arthritis (דלקת מפרקים שגרונית)
            "C4229471", // Hypothyroidism (תת־פעילות של בלוטת התריס)
            "C1561643", // Chronic kidney disease (מחלת כליות כרונית)
            "C0154722", // Epilepsy (אפילפסיה)
            "C3714514", // Infection (זיהום)
            "C4230171", // Attention deficit disorder with hyperactivity (הפרעת קשב עם היפראקטיביות - ADHD)
            "C3554473", // Anorexia (איבוד תיאבון / אנורקסיה)
            "C3553854", // Blindness (עיוורון)
            "C3554760", // Stroke (שבץ מוחי)
            "C0030567", // Parkinson disease (פרקינסון)
            "C0016053", // Fibromyalgia (פיברומיאלגיה)
            "C0006277", // Bronchitis (ברונכיטיס)

            // סימפטומים נפוצים
            "C3807341", // Chest pain (כאב חזה)
            "C4230442", // Dyspnea (קוצר נשימה)
            "C0004093", // Asthenia (חולשה)
            "C0015967", // Fever (חום)
            "C0018681", // Headache (כאב ראש)
            "C3554470", // Nausea (בחילה)
            "C4230730", // Vomiting (הקאה)
            "C0012833", // Dizziness (סחרחורת)
            "C0030193", // Pain (כאב)
            "C0015672", // Fatigue (עייפות)
            "C3554472", // Diarrhea (שלשול)
            "C1963177", // Muscle pain (כאבי שרירים)
            "C1969971", // Insomnia (נדודי שינה)
            "C4227880", // Rash (פריחה)
            "C0016382", // Flushing (אודם בפנים)
            "C0231528", // Myalgia (כאבי שרירים - מונח רפואי)
            "C3805748", // Tremor (רעד)
            "C0232462", // Decrease in appetite (איבוד תיאבון)
            "C4228281", // Anxiety (חרדה)
            "C3160712", // Palpitations (דפיקות לב)
            "C3150182", // Irritability (עצבנות)
            "C0009676", // Confusion (בלבול)
            "C0344232", // Blurred vision (טשטוש ראייה)
            "C3550344", // Frequent urination (שתן תכוף)
            "C3887873", // Hearing loss (אובדן שמיעה)
            "C0700590", // Sweating increased (הזעה מוגברת)
            "C0033774", // Itching (גרד)
            "C0687681", // Feverishness (תחושת חום)
            "C4227790", // Hyperactivity (היפראקטיביות)
            "C0424000", // Suicidal ideation (מחשבות אובדניות)
            "C0043352", // Xerostomia (יובש בפה)
            "C3887611", // Restlessness (אי שקט)
            "C3277399", // Depression (דיכאון)
            "C1262477", // Weight loss (ירידה במשקל)
            "C4227865", // Weight gain (עלייה במשקל)
            "C0220870", // Lightheadedness (תחושת עילפון)
            "C0235004", // Head pressure (לחץ בראש)


            // תרופות נפוצות
            "C0004057", // Aspirin (אספירין)
            "C0025598", // Metformin (מטפורמין)
            "C0065374", // Lisinopril (ליזינופריל)
            "C0286651", // Atorvastatin (אטורווסטטין)
            "C0051696", // Amlodipine (אמלודיפין)
            "C0700583", // Metoprolol (מטופרולול)
            "C0001927", // Albuterol (אלבוטרול)
            "C0017725", // Glucose (גלוקוז)
            "C0021655", // Insulin (אינסולין)
            "C0000970",  // Acetaminophen (אקטמינופן)
            "C0025810", // Methylphenidate (ריטלין)
            "C0016365", // Fluoxetine (פרוזאק)
            "C0074393", // Sertraline (סרטרלין)
            "C0593507", // Ibuprofen (איבופרופן)
            "C0028978", // Omeprazole (אומפרזול)
            "C0065180", // Loratadine (לוראטדין)
            "C3241607", // Ventolin (ונטולין)
            "C0012010", // Diazepam (וליום)
            "C0016860", // Furosemide (פוסיד – משתן)
            "C1881373", // Levothyroxine (אלטרוקסין – להיפותירואידיזם)
            "C0002645", // Amoxicillin (אמוקסיצילין – אנטיביוטיקה נפוצה)
            "C0030842",  //Penicillin Antibiotic (פניצילין - אנטיביוטיקה)

            //בדיקות נפוצות
            "C0013798", //EKG/ECG - בדיקת לב
            "C0009555", //ספירת דם מלאה - CBC
            "C0042037", //בדיקת שתן כללית
            "C0039985", //צילום חזה
            "C0005824", //מדידת לחץ דם
            "C0013516", //אקו לב - אולטרה-סאונד לב
            "C0392201", //בדיקת סוכר בדם
            "C0024485", //MRI
            "C0040405", //CT
            "C0041618", //אולטרה-סאונד כללי
            "C0023901", //בדיקות תפקודי כבד
            "C0022662" //בדיקות תפקודי כליות
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
