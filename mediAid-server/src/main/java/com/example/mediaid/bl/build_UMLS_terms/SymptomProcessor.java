package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Symptom;
import com.example.mediaid.dal.UMLS_terms.SymptomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SymptomProcessor extends GenericUmlsProcessor<Symptom> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור סימפטומים
    private static final Set<String> SYMPTOM_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T184"  // Sign or Symptom
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "SNOMEDCT_US", // SNOMED CT
            "MSH",        // Medical Subject Headings
            "MEDDRA",     // Medical Dictionary for Regulatory Activities
            "ICD10CM"     // ICD-10 Clinical Modification
    );

    @Autowired
    public SymptomProcessor(SymptomRepository repository) {
        super(repository, SYMPTOM_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new Symptom(), "Symptoms");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}