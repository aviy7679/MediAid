package com.example.mediaid.neo4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    // קשרים חדשים שהוספת
    public static final String CAUSED_BY = "CAUSED_BY";
    public static final String CAUSES = "CAUSES";
    public static final String HAS_ACTIVE_INGREDIENT = "HAS_ACTIVE_INGREDIENT";
    public static final String ACTIVE_INGREDIENT_OF = "ACTIVE_INGREDIENT_OF";

    // מיפוי מקשרי UMLS לקשרים שלנו - הגרסה המלאה והמתוקנת
    public static final Map<String, String> UMLS_TO_NEO4J_RELATIONSHIPS = new HashMap<>();
    static {
        // 1. טיפולים
        UMLS_TO_NEO4J_RELATIONSHIPS.put("treats", TREATS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_treat", TREATS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("therapeutic_class_of", TREATS);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_therapeutic_class", TREATS);

        // 2. סימפטומים ומחלות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_finding", HAS_SYMPTOM);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("finding_of", INDICATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_manifestation", HAS_SYMPTOM);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("manifestation_of", INDICATES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_associated_finding", HAS_SYMPTOM);

        // 3. התוויות נגד
        UMLS_TO_NEO4J_RELATIONSHIPS.put("contraindicated_with", CONTRAINDICATED_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("ci_with", CONTRAINDICATED_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_contraindication", CONTRAINDICATED_FOR);

        // 4. אינטראקציות תרופות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("interacts_with", INTERACTS_WITH);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("drug_interaction_of", INTERACTS_WITH);

        // 5. תופעות לוואי
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_adverse_effect", CAUSES_SIDE_EFFECT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("adverse_effect_of", SIDE_EFFECT_OF);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("side_effect_of", SIDE_EFFECT_OF);

        // 6. גורמי סיכון
        UMLS_TO_NEO4J_RELATIONSHIPS.put("risk_factor_for", RISK_FACTOR_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("predisposes", RISK_FACTOR_FOR);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("increases_risk_of", INCREASES_RISK_OF);

        // 7. מניעה
        UMLS_TO_NEO4J_RELATIONSHIPS.put("prevents", MAY_PREVENT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_prevent", MAY_PREVENT);

        // 8. מיקום אנטומי
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_finding_site", LOCATED_IN);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("finding_site_of", LOCATED_IN);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_location", LOCATED_IN);

        // 9. אבחון
        UMLS_TO_NEO4J_RELATIONSHIPS.put("diagnosed_by", DIAGNOSED_BY);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("diagnoses", DIAGNOSES);

        // 10. סיבתיות רפואית
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_causative_agent", CAUSED_BY);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("may_cause", CAUSES);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("complication_of", COMPLICATION_OF);

        // 11. רק המרכיבים הפעילים של תרופות
        UMLS_TO_NEO4J_RELATIONSHIPS.put("has_active_ingredient", HAS_ACTIVE_INGREDIENT);
        UMLS_TO_NEO4J_RELATIONSHIPS.put("active_ingredient_of", ACTIVE_INGREDIENT_OF);
    }

    /**
     * מחשב את משקל הקשר לפי סוג הקשר ומקור הנתונים
     */
    public static double calculateRelationshipWeight(String rela, String sab) {
        double baseWeight = switch (rela.toLowerCase()) {
            // טיפולים
            case "treats" -> 0.9;
            case "may_treat" -> 0.6;

            // התוויות נגד וסכנות
            case "contraindicated_with", "ci_with", "has_contraindication" -> 0.95;

            // אינטראקציות
            case "interacts_with", "drug_interaction_of" -> 0.85;

            // סימפטומים
            case "finding_of", "manifestation_of" -> 0.8;
            case "has_finding", "has_manifestation" -> 0.8;

            // תופעות לוואי
            case "adverse_effect_of", "has_adverse_effect", "side_effect_of", "has_side_effect" -> 0.75;

            // מניעה
            case "prevents", "may_prevent" -> 0.7;

            // סיבוכים
            case "complication_of", "has_complication" -> 0.8;

            // החמרה
            case "aggravates", "worsens" -> 0.75;

            // גורמי סיכון
            case "predisposes", "risk_factor_for", "increases_risk_of" -> 0.7;

            // אבחון
            case "diagnosed_by", "diagnoses" -> 0.85;

            // רצף התקדמות
            case "precedes", "follows" -> 0.6;

            // מיקום אנטומי
            case "location_of", "has_location" -> 0.9;

            // מנגנון פעולה ביולוגי
            case "inhibits", "stimulates" -> 0.75;

            // סיבתיות
            case "has_causative_agent", "may_cause" -> 0.8;

            // מרכיבים פעילים
            case "has_active_ingredient", "active_ingredient_of" -> 0.9;

            default -> 0.5;
        };

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

    /**
     * בדיקה אם הקשר מתאים לטיפוסי הצמתים - הגרסה המתוקנת
     */
    public static boolean isValidRelationshipForNodeTypes(
            Set<String> diseaseCuis, Set<String> medicationCuis, Set<String> symptomCuis,
            Set<String> riskFactorCuis, Set<String> procedureCuis, Set<String> anatomicalCuis,
            Set<String> labTestCuis, Set<String> biologicalFunctionCuis,
            String cui1, String cui2, String relType) {

        // בדיקת null safety
        if (relType == null || cui1 == null || cui2 == null ||
                diseaseCuis == null || medicationCuis == null || symptomCuis == null ||
                riskFactorCuis == null || procedureCuis == null || anatomicalCuis == null ||
                labTestCuis == null || biologicalFunctionCuis == null) {
            return false;
        }

        return switch (relType) {
            case INDICATES, HAS_SYMPTOM -> // סימפטום ↔ מחלה
                    (symptomCuis.contains(cui1) && diseaseCuis.contains(cui2)) ||
                            (diseaseCuis.contains(cui1) && symptomCuis.contains(cui2));

            case TREATS, TREATED_BY -> // תרופה ↔ מחלה
                    (medicationCuis.contains(cui1) && diseaseCuis.contains(cui2)) ||
                            (diseaseCuis.contains(cui1) && medicationCuis.contains(cui2));

            case CONTRAINDICATED_FOR -> // תרופה ← מחלה/מצב
                    medicationCuis.contains(cui1) &&
                            (diseaseCuis.contains(cui2) || riskFactorCuis.contains(cui2));

            case INTERACTS_WITH -> // תרופה ↔ תרופה
                    medicationCuis.contains(cui1) && medicationCuis.contains(cui2);

            case SIDE_EFFECT_OF, CAUSES_SIDE_EFFECT -> // סימפטום ↔ תרופה
                    (symptomCuis.contains(cui1) && medicationCuis.contains(cui2)) ||
                            (medicationCuis.contains(cui1) && symptomCuis.contains(cui2));

            case MAY_PREVENT -> // תרופה → מחלה
                    medicationCuis.contains(cui1) && diseaseCuis.contains(cui2);

            case RISK_FACTOR_FOR, INCREASES_RISK_OF -> // מצב → מחלה
                    (riskFactorCuis.contains(cui1) || diseaseCuis.contains(cui1)) &&
                            diseaseCuis.contains(cui2);

            case LOCATED_IN -> // מחלה/סימפטום → איבר
                    (diseaseCuis.contains(cui1) || symptomCuis.contains(cui1)) &&
                            anatomicalCuis.contains(cui2);

            case DIAGNOSED_BY, DIAGNOSES -> // מחלה ↔ בדיקה
                    (diseaseCuis.contains(cui1) && labTestCuis.contains(cui2)) ||
                            (labTestCuis.contains(cui1) && diseaseCuis.contains(cui2));

            case CAUSED_BY, CAUSES -> // סיבתיות רפואית
                    (diseaseCuis.contains(cui1) && diseaseCuis.contains(cui2)) ||
                            (symptomCuis.contains(cui1) && diseaseCuis.contains(cui2)) ||
                            (diseaseCuis.contains(cui1) && symptomCuis.contains(cui2));

            case HAS_ACTIVE_INGREDIENT, ACTIVE_INGREDIENT_OF -> // תרופה ↔ חומר פעיל
                    medicationCuis.contains(cui1) && medicationCuis.contains(cui2);

            default -> false;
        };
    }
}