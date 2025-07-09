package com.example.mediaid.bl.build_UMLS_terms;

import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import com.example.mediaid.constants.DatabaseConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class MedicationProcessor extends GenericUmlsProcessor<Medication> implements CommandLineRunner {

    @Autowired
    public MedicationProcessor(MedicationRepository repository) {
        super(repository,
                new HashSet<>(Arrays.asList(DatabaseConstants.MEDICATION_SEMANTIC_TYPES)),
                Arrays.asList(DatabaseConstants.MEDICATION_PREFERRED_SOURCES),
                cui -> new Medication(),
                "Medications");
    }

    @Override
    public void run(String... args) {
        processAndSave();
    }
}