package com.example.mediaid.bl.neo4j;


import java.util.HashMap;
import java.util.Map;

/**
 * סוגי הקשרים במערכת והמיפויים שלהם מ-UMLS
 */
public class RelationshipTypes {

    // קשרים בסיסיים
    public static final String INDICATES = "INDICATES";               // סימפטום → מחלה
    public static final String HAS_SYMPTOM = "HAS_SYMPTOM";           // מחלה → סימפטום
    public static final String TREATS = "TREATS";                     // תרופה → מחלה
    public static final String TREATED_BY = "TREATED_BY";             // מחלה → תרופה
    public static final String CONTRAINDICATED_FOR = "CONTRAINDICATED_FOR"; // תרופה → מחלה
    public static final String INTERACTS_WITH = "INTERACTS_WITH";     // תרופה → תרופה

    // קשרים מורחבים
    public static final String SIDE_EFFECT_OF = "SIDE_EFFECT_OF";     // סימפטום → תרופה
    public static final String CAUSES_SIDE_EFFECT = "CAUSES_SIDE_EFFECT"; // תרופה → סימפטום
    public static final String MAY_PREVENT = "MAY_PREVENT";           // תרופה → מחלה
    public static final String COMPLICATION_OF = "COMPLICATION_OF";   // מחלה → מחלה
    public static final String AGGRAVATES = "AGGRAVATES";             // מחלה/סימפטום → מחלה
    public static final String RISK_FACTOR_FOR = "RISK_FACTOR_FOR";   // מצב → מחלה
    public static final String INCREASES_RISK_OF = "INCREASES_RISK_OF"; // מצב → מחלה
    public static final String DIAGNOSED_BY = "DIAGNOSED_BY";         // מחלה → בדיקה
    public static final String DIAGNOSES = "DIAGNOSES";               // בדיקה → מחלה
    public static final String PRECEDES = "PRECEDES";                 // מחלה/סימפטום → מחלה/סימפטום
    public static final String LOCATED_IN = "LOCATED_IN";             // מחלה/סימפטום → איבר
    public static final String INHIBITS = "INHIBITS";                 // תרופה → פונקציה ביולוגית
    public static final String STIMULATES = "STIMULATES";             // תרופה → פונקציה ביולוגית

    // מיפוי מקשרי UMLS לקשרים שלנו
    public static final Map<String, String> UMLS_TO_NEO4J_RELATIONSHIPS = new HashMap<>();
    static {
        // סימפטומים ומחלות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_finding", HAS_SYMPTOM);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("finding_of", INDICATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("manifestation_of", INDICATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_manifestation", HAS_SYMPTOM);

        // טיפולים
        UMLS_TO_NEO4J_RELATIONSHIPS.put("treats", TREATS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_treat", TREATS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_be_treated_by", TREATED_BY);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_therapeutic_class", TREATS);

        // התוויות נגד
        UMLS_TO_NEO4J_RELATIONSHIPS.put("contraindicated_with", CONTRAINDICATED_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("ci_with", CONTRAINDICATED_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_contraindication", CONTRAINDICATED_FOR);

        // אינטראקציות תרופות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("interacts_with", INTERACTS_WITH);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("drug_interaction_of", INTERACTS_WITH);

        // תופעות לוואי
        UMLS_TO_NEO4J_RELATIONSHIPS.put("adverse_effect_of", SIDE_EFFECT_OF);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_adverse_effect", CAUSES_SIDE_EFFECT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("side_effect_of", SIDE_EFFECT_OF);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_side_effect", CAUSES_SIDE_EFFECT);

        // מניעה
        UMLS_TO_NEO4J_RELATIONSHIPS.put("prevents", MAY_PREVENT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_prevent", MAY_PREVENT);

        // סיבוכים
        UMLS_TO_NEO4J_RELATIONSHIPS.put("complication_of", COMPLICATION_OF);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_complication", COMPLICATION_OF);

        // החמרה
        UMLS_TO_NEO4J_RELATIONSHIPS.put("aggravates", AGGRAVATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("worsens", AGGRAVATES);

        // גורמי סיכון
        UMLS_TO_NEO4J_RELATIONSHIPS.put("predisposes", RISK_FACTOR_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("risk_factor_for", RISK_FACTOR_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("risk_of", INCREASES_RISK_OF);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("increases_risk_of", INCREASES_RISK_OF);

        // אבחון
        UMLS_TO_NEO4J_RELATIONSHIPS.put("diagnosed_by", DIAGNOSED_BY);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("diagnoses", DIAGNOSES);

        // רצף התקדמות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("precedes", PRECEDES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("follows", PRECEDES);

        // מיקום אנטומי
        UMLS_TO_NEO4J_RELATIONSHIPS.put("location_of", LOCATED_IN);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_location", LOCATED_IN);

        // מנגנון פעולה ביולוגי
        UMLS_TO_NEO4J_RELATIONSHIPS.put("inhibits", INHIBITS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("stimulates", STIMULATES);
    }

    /**
     * מחשב את משקל הקשר לפי סוג הקשר ומקור הנתונים
     */
    public static double calculateRelationshipWeight(String rela, String sab) {
        double baseWeight = 0.5;

        // התאמת משקל לפי סוג היחס
        switch (rela.toLowerCase()) {
            // טיפולים
            case "treats":
                baseWeight = 0.9;
                break;
            case "may_treat":
                baseWeight = 0.6;
                break;

            // התוויות נגד וסכנות
            case "contraindicated_with":
            case "ci_with":
            case "has_contraindication":
                baseWeight = 0.95;
                break;

            // אינטראקציות
            case "interacts_with":
            case "drug_interaction_of":
                baseWeight = 0.85;
                break;

            // סימפטומים
            case "finding_of":
            case "manifestation_of":
                baseWeight = 0.8;
                break;
            case "has_finding":
            case "has_manifestation":
                baseWeight = 0.8;
                break;

            // תופעות לוואי
            case "adverse_effect_of":
            case "has_adverse_effect":
            case "side_effect_of":
            case "has_side_effect":
                baseWeight = 0.75;
                break;

            // מניעה
            case "prevents":
            case "may_prevent":
                baseWeight = 0.7;
                break;

            // סיבוכים
            case "complication_of":
            case "has_complication":
                baseWeight = 0.8;
                break;

            // החמרה
            case "aggravates":
            case "worsens":
                baseWeight = 0.75;
                break;

            // גורמי סיכון
            case "predisposes":
            case "risk_factor_for":
            case "increases_risk_of":
                baseWeight = 0.7;
                break;

            // אבחון
            case "diagnosed_by":
            case "diagnoses":
                baseWeight = 0.85;
                break;

            // רצף התקדמות
            case "precedes":
            case "follows":
                baseWeight = 0.6;
                break;

            // מיקום אנטומי
            case "location_of":
            case "has_location":
                baseWeight = 0.9;
                break;

            // מנגנון פעולה ביולוגי
            case "inhibits":
            case "stimulates":
                baseWeight = 0.75;
                break;
        }

        // התאמת משקל לפי מקור הנתונים
        switch (sab) {
            case "SNOMEDCT_US":
                baseWeight *= 1.2;  // המקור האמין ביותר
                break;
            case "RXNORM":
                baseWeight *= 1.1;  // מקור מצוין לתרופות
                break;
            case "NDF-RT":
                baseWeight *= 1.05; // מקור טוב להתוויות נגד
                break;
            case "ICD10":
                baseWeight *= 1.1;  // מקור טוב למחלות
                break;
            case "MSH":
                baseWeight *= 1.05; // מקור טוב למונחים רפואיים
                break;
        }

        // נרמול המשקל ל-0-1
        return Math.min(0.99, baseWeight);
    }
}
