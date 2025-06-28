package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.LabTest;
import com.example.mediaid.dal.UMLS_terms.LabTestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LabTestProcessor extends GenericUmlsProcessor<LabTest> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור בדיקות מעבדה
    private static final Set<String> LAB_TEST_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T059", // Laboratory Procedure
            "T034"  // Laboratory or Test Result
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "LNC",        // Logical Observation Identifiers Names and Codes (LOINC)
            "SNOMEDCT_US", // SNOMED CT
            "CPT",        // Current Procedural Terminology
            "MSH",        // Medical Subject Headings
            "ICD10PCS"    // ICD-10 Procedure Coding System
    );

    @Autowired
    public LabTestProcessor(LabTestRepository repository) {
        super(repository, LAB_TEST_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new LabTest(), "Laboratory tests");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}