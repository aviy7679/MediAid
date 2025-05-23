package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DiseaseProcessor extends GenericUmlsProcessor<Disease> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור מחלות
    private static final Set<String> DISEASE_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T046", // Disease or Syndrome
            "T047", // Disease or Finding
            "T191"  // Neoplastic Process
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "SNOMEDCT_US", // SNOMED CT
            "MSH",        // Medical Subject Headings
            "ICD10CM",    // ICD-10 Clinical Modification
            "LNC",        // Logical Observation Identifiers Names and Codes
            "MEDDRA"      // Medical Dictionary for Regulatory Activities
    );

    @Autowired
    public DiseaseProcessor(DiseaseRepository repository) {
        super(repository, DISEASE_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new Disease(), "Diseases");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}