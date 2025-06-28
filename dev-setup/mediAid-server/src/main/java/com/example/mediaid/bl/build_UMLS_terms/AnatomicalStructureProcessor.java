package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.AnatomicalStructure;
import com.example.mediaid.dal.UMLS_terms.AnatomicalStructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AnatomicalStructureProcessor extends GenericUmlsProcessor<AnatomicalStructure> implements CommandLineRunner {
    // קטגוריות סמנטיות לאיתור מבנים אנטומיים
    private static final Set<String> ANATOMICAL_SEMANTIC_TYPES = new HashSet<>(Arrays.asList(
            "T017", // Anatomical Structure
            "T029", // Body Location or Region
            "T023", // Body Part, Organ, or Organ Component
            "T030", // Body Space or Junction
            "T021"  // Fully Formed Anatomical Structure
    ));

    // מקורות מידע מועדפים לבחירת מונחים
    private static final List<String> PREFERRED_SOURCES = Arrays.asList(
            "SNOMEDCT_US", // SNOMED CT
            "FMA",        // Foundational Model of Anatomy
            "NCBI",       // National Center for Biotechnology Information
            "MSH",        // Medical Subject Headings
            "TA"          // Terminologia Anatomica
    );

    @Autowired
    public AnatomicalStructureProcessor(AnatomicalStructureRepository repository) {
        super(repository, ANATOMICAL_SEMANTIC_TYPES, PREFERRED_SOURCES,
                cui -> new AnatomicalStructure(), "Anatomical structures");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}