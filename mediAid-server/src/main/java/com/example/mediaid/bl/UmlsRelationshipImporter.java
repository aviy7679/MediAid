//package com.example.mediaid.bl;
//
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.Session;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.*;
//
//@Service
//public class UmlsRelationshipImporter {
//
//    private final Driver driver;
//
//    @Autowired
//    public UmlsRelationshipImporter(Driver driver) {
//        this.driver = driver;
//    }
//
//    private static final Set<String> PREFERRED_SOURCES = new HashSet<>(Arrays.asList(
//            "SNOMEDCT_US",  // המקור האמין ביותר לקשרים קליניים
//            "RXNORM",       // מקור מצוין לתרופות ויחסי תרופות
//            "NDF-RT",       // קשרי תרופות והתוויות נגד
//            "MSH",          // מונחי MeSH
//            "ICD10",        // קידוד מחלות
//            "MTHSPL"        // מידע אודות תרופות ותווית
//    ));
//
//    private static final Map<String, String> TARGETED_RELATIONSHIPS = new HashMap<>();
//    static {
//        // יחסי סימפטומים ומחלות
//        TARGETED_RELATIONSHIPS.put("has_finding", "HAS_SYMPTOM");        // מחלה → סימפטום
//        TARGETED_RELATIONSHIPS.put("finding_of", "INDICATES");           // סימפטום → מחלה
//        TARGETED_RELATIONSHIPS.put("manifestation_of", "INDICATES");     // סימפטום → מחלה
//        TARGETED_RELATIONSHIPS.put("has_manifestation", "HAS_SYMPTOM");  // מחלה → סימפטום
//
//        // יחסי טיפול
//        TARGETED_RELATIONSHIPS.put("treats", "TREATS");                  // תרופה → מחלה
//        TARGETED_RELATIONSHIPS.put("may_treat", "TREATS");               // תרופה → מחלה
//        TARGETED_RELATIONSHIPS.put("may_be_treated_by", "TREATED_BY");   // מחלה → תרופה
//
//        // התוויות נגד
//        TARGETED_RELATIONSHIPS.put("contraindicated_with", "CONTRAINDICATED_FOR");
//        TARGETED_RELATIONSHIPS.put("CI_with", "CONTRAINDICATED_FOR");
//
//        // אינטראקציות תרופות
//        TARGETED_RELATIONSHIPS.put("interacts_with", "INTERACTS_WITH");
//        TARGETED_RELATIONSHIPS.put("has_contraindication", "CONTRAINDICATED_FOR");
//    }
//    private Set<String> diseaseCuis = new HashSet<>();
//    private Set<String> medicationCuis = new HashSet<>();
//    private Set<String> symptomCuis = new HashSet<>();
//
//    //ייבוא הקשרים
//    public void importRelationships(String mrrelPath) {
//        try{
//            loadAllCuisFromGraph();
//        }catch (Exception e){
//            System.err.println("Error importing relationship: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private Set<String> loadCuisForEntityType(Session session, String entityType) {
//        return new HashSet<>(
//                session.readTransaction(tx -> {
//                    var result = tx.run("MATCH (e:" + entityType + ") RETURN e.cui AS cui");
//                    List<String> cuis = new ArrayList<>();
//                    result.forEachRemaining(record -> cuis.add(record.get("cui").asString()));
//                    return cuis;
//                })
//        );
//    }
//
//    //טוען את הצמתים שכבר קיימים
//    private void loadAllCuisFromGraph() {
//        try(Session session = driver.session()) {
//            // טען CUIs של מחלות
//            Set<String> diseaseCuis = loadCuisForEntityType(session, "Disease");
//            System.out.println(" מזהי CUI של מחלות." + diseaseCuis.size() + "נטענו ");
//
//            // טען CUIs של תרופות
//            Set<String> medicationCuis = loadCuisForEntityType(session, "Medication");
//            System.out.println(" מזהי CUI של תרופות." + medicationCuis.size() + "נטענו ");
//
//            // טען CUIs של סימפטומים
//            Set<String> symptomCuis = loadCuisForEntityType(session, "Symptom");
//            System.out.println(" מזהי CUI של סימפטומים." + symptomCuis.size() + "נטענו ");
//        }
//    }
//
//    //בדיקה אם CUI קיים
//    private boolean nodeCuiExists(String cui) {
//        return diseaseCuis.contains(cui) || medicationCuis.contains(cui) || symptomCuis.contains(cui);
//    }
//
//    //ייבוא מקובץ MRREL.RFF עם סינון קשרים רלוונטיים
//    private void importRelationshipsFromMrrel(String mrrelPath) throws IOException {
//        System.out.println("Importing relationships from " + mrrelPath);
//        List<Map<String, Object>> validRelationships = new ArrayList<>();
//        // מונים לסטטיסטיקה
//        int totalLines = 0;
//        int skippedSourceNotPreferred = 0;
//        int skippedRelationshipNotTargeted = 0;
//        int skippedMissingNodes = 0;
//        int skippedInvalidNodeTypes = 0;
//        int acceptedRelationships = 0;
//
//        // גודל אצווה
//        final int BATCH_SIZE = 10000;
//        try (BufferedReader reader = new BufferedReader(new FileReader(mrrelPath))){
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//                totalLines++;
//                if(totalLines % 1000000 == 0) {
//                    System.out.println(totalLines+" lines read.");
//
//                    String[] fields = line.split("\\|");
//
//                    if(fields.length <16) continue;
//
//                    String cui1 = fields[0];  // CUI ראשון
//                    String cui2 = fields[4];  // CUI שני
//                    String rel = fields[3];   // סוג יחס כללי
//                    String rela = fields[7];  // סוג יחס ספציפי
//                    String sab = fields[10];  // מקור הנתונים
//
//                    //סינון לפי המקורות המועדפים
//                    if(!PREFERRED_SOURCES.contains(sab)){
//                        skippedSourceNotPreferred++;
//                        continue;
//                    }
//                    //סינון לפי סוגי יחס
//                    if (rela==null ||rela.isEmpty() ||!TARGETED_RELATIONSHIPS.containsKey(rela.toLowerCase())){
//                        skippedRelationshipNotTargeted++;
//                        continue;
//                    }
//                    //סינון ע"פ CUI שקיימים במאגר
//                    if(!nodeCuiExists(cui1)||!nodeCuiExists(cui2)){
//                        skippedMissingNodes++;
//                        continue;
//                    }
//                    //
//
//                }
//            }
//        }
//    }
//
//}
