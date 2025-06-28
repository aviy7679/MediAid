package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Procedure;
import com.example.mediaid.dal.UMLS_terms.ProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ProcedureProcessor extends GenericUmlsProcessor<Procedure> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור הליכים רפואיים
    private static final Set<String> PROCEDURE_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T060", // Diagnostic Procedure
            "T061", // Therapeutic or Preventive Procedure
            "T059", // Laboratory Procedure
            "T058"  // Health Care Activity
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "SNOMEDCT_US", // SNOMED CT
            "CPT",        // Current Procedural Terminology
            "LNC",        // Logical Observation Identifiers Names and Codes
            "MSH",        // Medical Subject Headings
            "ICF",        // International Classification of Functioning, Disability and Health
            "ICD10PCS"    // ICD-10 Procedure Coding System
    );

    @Autowired
    public ProcedureProcessor(ProcedureRepository repository) {
        super(repository, PROCEDURE_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new Procedure(), "Medical procedures");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}