package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Symptom;
import com.example.mediaid.dal.UMLS_terms.SymptomRepository;
import com.example.mediaid.constants.DatabaseConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SymptomProcessor extends GenericUmlsProcessor<Symptom> implements CommandLineRunner {

    @Autowired
    public SymptomProcessor(SymptomRepository repository) {
        super(repository,
                new HashSet<>(Arrays.asList(DatabaseConstants.SYMPTOM_SEMANTIC_TYPES)),
                Arrays.asList(DatabaseConstants.SYMPTOM_PREFERRED_SOURCES),
                cui -> new Symptom(),
                "Symptoms");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}