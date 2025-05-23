package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.BiologicalFunction;
import com.example.mediaid.dal.UMLS_terms.BiologicalFunctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BiologicalFunctionProcessor extends GenericUmlsProcessor<BiologicalFunction> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור פונקציות ביולוגיות
    private static final Set<String> BIOLOGICAL_FUNCTION_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T038", // Biologic Function
            "T039", // Physiologic Function
            "T040", // Organism Function
            "T042", // Organ or Tissue Function
            "T043", // Cell Function
            "T044", // Molecular Function
            "T045"  // Genetic Function
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "SNOMEDCT_US", // SNOMED CT
            "GO",         // Gene Ontology
            "MSH",        // Medical Subject Headings
            "NCI",        // National Cancer Institute Thesaurus
            "MEDLINEPLUS" // MedlinePlus Health Information
    );

    @Autowired
    public BiologicalFunctionProcessor(BiologicalFunctionRepository repository) {
        super(repository, BIOLOGICAL_FUNCTION_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new BiologicalFunction(), "Biological functions");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}