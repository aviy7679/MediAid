package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.RiskFactor;
import com.example.mediaid.dal.UMLS_terms.RiskFactorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RiskFactorProcessor extends GenericUmlsProcessor<RiskFactor> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור גורמי סיכון
    private static final Set<String> RISK_FACTOR_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T080", // Qualitative Concept
            "T081", // Quantitative Concept
            "T169", // Functional Concept
            "T033", // Finding
            "T037", // Injury or Poisoning
            "T203"  // Drug Delivery Device
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "SNOMEDCT_US", // SNOMED CT
            "MSH",        // Medical Subject Headings
            "LNC",        // Logical Observation Identifiers Names and Codes
            "MEDDRA"      // Medical Dictionary for Regulatory Activities
    );

    @Autowired
    public RiskFactorProcessor(RiskFactorRepository repository) {
        super(repository, RISK_FACTOR_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new RiskFactor(), "Risk factors");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}