package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class MedicationProcessor extends GenericUmlsProcessor<Medication> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור תרופות
    private static final Set<String> MEDICATION_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T121", // Pharmacologic Substance
            "T200", // Clinical Drug
            "T195", // Antibiotic
            "T125"  // Hormone
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "RXNORM",     // RxNorm
            "SNOMEDCT_US", // SNOMED CT
            "MSH",        // Medical Subject Headings
            "ATC",        // Anatomical Therapeutic Chemical
            "MEDDRA",     // Medical Dictionary for Regulatory Activities
            "NDFRT"       // National Drug File - Reference Terminology
    );

    @Autowired
    public MedicationProcessor(MedicationRepository repository) {
        super(repository, MEDICATION_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new Medication(), "Medications");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}