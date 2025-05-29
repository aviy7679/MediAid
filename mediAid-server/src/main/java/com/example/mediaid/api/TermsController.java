package com.example.mediaid.api;

import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/searchTerms")
public class TermsController {

    @Autowired
    private MedicationRepository medicationRepository;
    @Autowired
    private DiseaseRepository diseaseRepository;

    @GetMapping("/medication")
    public List<Medication> searchMedications(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return medicationRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
                 PageRequest.of(0, limit));
    }

    @GetMapping("/disease")
    public List<Disease> searchDiseases(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return diseaseRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
                 PageRequest.of(0, limit));
    }

}
