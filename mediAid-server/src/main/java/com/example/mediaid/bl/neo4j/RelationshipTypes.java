package com.example.mediaid.bl.neo4j;


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
            default -> 0.5;

            // התאמת משקל לפי סוג היחס
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
     * בדיקה אם הקשר מתאים לטיפוסי הצמתים
     */
    public static boolean isValidRelationshipForNodeTypes(
            Set<String> diseaseCuis, Set<String> medicationCuis, Set<String> symptomCuis,
            Set<String> riskFactorCuis, Set<String> procedureCuis, Set<String> anatomicalCuis,
            Set<String> labTestCuis, Set<String> biologicalFunctionCuis,
            String cui1, String cui2, String relType) {

        return switch (relType) {
            case INDICATES ->
                // סימפטום ← מחלה
                    symptomCuis.contains(cui1) && diseaseCuis.contains(cui2);
            case HAS_SYMPTOM ->
                // מחלה ← סימפטום
                    diseaseCuis.contains(cui1) && symptomCuis.contains(cui2);
            case TREATS ->
                // תרופה ← מחלה
                    medicationCuis.contains(cui1) && diseaseCuis.contains(cui2);
            case TREATED_BY ->
                // מחלה ← תרופה
                    diseaseCuis.contains(cui1) && medicationCuis.contains(cui2);
            case CONTRAINDICATED_FOR ->
                // תרופה ← מחלה/מצב
                    medicationCuis.contains(cui1) && (diseaseCuis.contains(cui2) || riskFactorCuis.contains(cui2));
            case INTERACTS_WITH ->
                // תרופה ← תרופה
                    medicationCuis.contains(cui1) && medicationCuis.contains(cui2);
            case SIDE_EFFECT_OF ->
                // סימפטום ← תרופה
                    symptomCuis.contains(cui1) && medicationCuis.contains(cui2);
            case CAUSES_SIDE_EFFECT ->
                // תרופה ← סימפטום
                    medicationCuis.contains(cui1) && symptomCuis.contains(cui2);
            case MAY_PREVENT ->
                // תרופה ← מחלה
                    medicationCuis.contains(cui1) && diseaseCuis.contains(cui2);
            case COMPLICATION_OF ->
                // מחלה ← מחלה
                    diseaseCuis.contains(cui1) && diseaseCuis.contains(cui2);
            case AGGRAVATES ->
                // מחלה/סימפטום ← מחלה
                    (diseaseCuis.contains(cui1) || symptomCuis.contains(cui1)) && diseaseCuis.contains(cui2);
            case RISK_FACTOR_FOR, INCREASES_RISK_OF ->
                // מצב/מחלה ← מחלה
                    (riskFactorCuis.contains(cui1) || diseaseCuis.contains(cui1)) && diseaseCuis.contains(cui2);
            case DIAGNOSED_BY ->
                // מחלה ← בדיקה/פרוצדורה
                    diseaseCuis.contains(cui1) && (labTestCuis.contains(cui2) || procedureCuis.contains(cui2));
            case DIAGNOSES ->
                // בדיקה/פרוצדורה ← מחלה
                    (labTestCuis.contains(cui1) || procedureCuis.contains(cui1)) && diseaseCuis.contains(cui2);
            case PRECEDES ->
                // מחלה/סימפטום ← מחלה/סימפטום
                    (diseaseCuis.contains(cui1) || symptomCuis.contains(cui1)) &&
                            (diseaseCuis.contains(cui2) || symptomCuis.contains(cui2));
            case LOCATED_IN ->
                // מחלה/סימפטום ← איבר
                    (diseaseCuis.contains(cui1) || symptomCuis.contains(cui1)) && anatomicalCuis.contains(cui2);
            case INHIBITS, STIMULATES ->
                // תרופה ← פונקציה ביולוגית
                    medicationCuis.contains(cui1) && biologicalFunctionCuis.contains(cui2);
            default -> false;
        };
    }
}

