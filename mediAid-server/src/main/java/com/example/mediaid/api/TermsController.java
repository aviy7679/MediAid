//package com.example.mediaid.api;
//
//import com.example.mediaid.dal.UMLS_terms.Disease;
//import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
//import com.example.mediaid.dal.UMLS_terms.Medication;
//import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/searchTerms")
//public class TermsController {
//
//    @Autowired
//    private MedicationRepository medicationRepository;
//    @Autowired
//    private DiseaseRepository diseaseRepository;
//
//    @GetMapping("/medication")
//    public List<Medication> searchMedications(
//            @RequestParam String query,
//            @RequestParam(defaultValue = "10") int limit) {
//        return medicationRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
//                 PageRequest.of(0, limit));
//    }
//
//    @GetMapping("/disease")
//    public List<Disease> searchDiseases(
//            @RequestParam String query,
//            @RequestParam(defaultValue = "10") int limit) {
//        return diseaseRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
//                 PageRequest.of(0, limit));
//    }
//
//}
// TermsController.java - גרסה מתוקנת
package com.example.mediaid.api;

import com.example.mediaid.dal.UMLS_terms.Disease;
import com.example.mediaid.dal.UMLS_terms.DiseaseRepository;
import com.example.mediaid.dal.UMLS_terms.Medication;
import com.example.mediaid.dal.UMLS_terms.MedicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // שונה מ /searchTerms ל /api
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class TermsController {

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    // GET /api/medications/search - מתאים לקריאה מ-MedicationSearch.jsx
    @GetMapping("/medications/search")
    public List<Medication> searchMedications(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return medicationRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
                PageRequest.of(0, limit));
    }

    // GET /api/diseases/search - מתאים לקריאה מ-UserDataWizard.jsx
    @GetMapping("/diseases/search")
    public List<Disease> searchDiseases(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return diseaseRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query,
                PageRequest.of(0, limit));
    }

    // שמור על ה-endpoints הישנים לתאימות לאחור
    @GetMapping("/searchTerms/medication")
    @Deprecated
    public List<Medication> searchMedicationsOld(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return searchMedications(query, limit);
    }

    @GetMapping("/searchTerms/disease")
    @Deprecated
    public List<Disease> searchDiseasesOld(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return searchDiseases(query, limit);
    }
}