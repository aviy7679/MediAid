package com.example.mediaid.bl.neo4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * סוגי היישויות במערכת
 */
public class EntityTypes {

    // סוגי יישויות מרכזיים
    public static final String DISEASE = "Disease";
    public static final String MEDICATION = "Medication";
    public static final String SYMPTOM = "Symptom";
    public static final String RISK_FACTOR = "RiskFactor";
    public static final String PROCEDURE = "Procedure";
    public static final String BIOLOGICAL_FUNCTION = "BiologicalFunction";
    public static final String ANATOMICAL_STRUCTURE = "AnatomicalStructure";

    // יישויות נוספות
    public static final String LABORATORY_TEST = "LaboratoryTest";
    public static final String DIAGNOSTIC_PROCEDURE = "DiagnosticProcedure";

    // מקורות נתונים מועדפים - לסינון
    public static final Set<String> PREFERRED_SOURCES = new HashSet<>(Arrays.asList(
            "SNOMEDCT_US",  // המקור האמין ביותר לקשרים קליניים
            "RXNORM",       // מקור מצוין לתרופות ויחסי תרופות
            "NDF-RT",       // קשרי תרופות והתוויות נגד
            "MSH",          // מונחי MeSH
            "ICD10",        // קידוד מחלות
            "MTHSPL",       // מידע אודות תרופות ותווית
            "AOD",          // מידע על סמים ואלכוהול
            "ATC",          // מערכת סיווג רפואית
            "CSP",          // פתולוגיה
            "MEDLINEPLUS",  // מידע על בריאות למטופלים
            "MTH",           // מטא-תזאורוס
            "SNOMEDCT_US",  // SNOMED CT US Edition
            "RXNORM",       // RxNorm
            "MTHSPL",       // Metathesaurus Structured Product Labels
            "NCI",          // National Cancer Institute
            "LNC",          // LOINC
            "NCBI",         // National Center for Biotechnology Information
            "OMIM",         // Online Mendelian Inheritance in Man
            "HPO",          // Human Phenotype Ontology
            "MEDCIN",       // MEDCIN
            "ICD10PCS",     // ICD-10 Procedure Coding System
            "UWDA",         // University of Washington Digital Anatomist
            "SNOMEDCT_VET" // SNOMED CT Veterinary Extension
    ));

    // טבלת מיפוי בין מקורות UMLS לסוגי יישויות
    public static final Map<String, String> SEMANTIC_TYPE_TO_ENTITY_MAPPING = Map.ofEntries(
            Map.entry("T047", DISEASE),           // Disease or Syndrome
            Map.entry("T048", DISEASE),           // Mental or Behavioral Dysfunction
            Map.entry("T019", DISEASE),           // Congenital Abnormality
            Map.entry("T046", DISEASE),           // Pathologic Function
            Map.entry("T184", SYMPTOM),           // Sign or Symptom
            Map.entry("T121", BIOLOGICAL_FUNCTION), // Pharmacologic Substance
            Map.entry("T116", ANATOMICAL_STRUCTURE), // Amino Acid, Peptide, or Protein
            Map.entry("T195", ANATOMICAL_STRUCTURE), // Anatomical Structure
            Map.entry("T123", BIOLOGICAL_FUNCTION), // Biologically Active Substance
            Map.entry("T061", PROCEDURE),         // Therapeutic or Preventive Procedure
            Map.entry("T060", DIAGNOSTIC_PROCEDURE), // Diagnostic Procedure
            Map.entry("T034", LABORATORY_TEST),   // Laboratory or Test Result
            Map.entry("T109", MEDICATION),        // Organic Chemical
            Map.entry("T197", MEDICATION),        // Inorganic Chemical
            Map.entry("T200", MEDICATION),        // Clinical Drug
            Map.entry("T114", RISK_FACTOR)        // Quantitative Concept (for BMI, age, etc.)
    );
}