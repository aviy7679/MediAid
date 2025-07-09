package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import com.example.mediaid.constants.DatabaseConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DiseaseProcessor extends GenericUmlsProcessor<Disease> implements CommandLineRunner {

    @Autowired
    public DiseaseProcessor(DiseaseRepository repository) {
        super(repository,
                new HashSet<>(Arrays.asList(DatabaseConstants.DISEASE_SEMANTIC_TYPES)),
                Arrays.asList(DatabaseConstants.DISEASE_PREFERRED_SOURCES),
                cui -> new Disease(),
                "Diseases");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}